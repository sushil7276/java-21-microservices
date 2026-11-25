package com.ecommerce.cart.store;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * REDIS CART STORAGE
 * ------------------
 * Shopping carts live in Redis instead of MySQL: they are small, change all the time, and a lost cart is not a disaster.
 *
 * Every user has one Redis HASH (think: a small map):
 *
 *     key    ecommerce:cart:14                      (14 = user id)
 *     fields 5            -> {"productName":"Dell XPS 13","unitPrice":114990,"quantity":2}
 *            7            -> {"productName":"JBL Flip 4","unitPrice":7999,"quantity":1}
 *            _updatedAt   -> 1795556000000          (when the cart was last changed, in milliseconds)
 *
 * The field name is the product id, the value is the line as JSON.
 *
 * Look at it yourself:  docker exec ecommerce-redis redis-cli hgetall ecommerce:cart:14
 */
@Component
public class CartStore {

    private static final String KEY_PREFIX = "ecommerce:cart:";
    private static final String UPDATED_FIELD = "_updatedAt";

    /**
     * Safety net: Redis deletes a cart by itself after 90 days without changes.
     * (The clean-up batch job removes abandoned carts much earlier, after 14 days by default.)
     */
    private static final Duration MAX_IDLE = Duration.ofDays(90);

    /** What is stored in the JSON value of a field (the product id is already the field name). */
    private record Stored(String productName, BigDecimal unitPrice, Integer quantity) {
    }

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public CartStore(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    private static String key(long userId) {
        return KEY_PREFIX + userId;
    }

    /** All lines of the cart, ordered by product id. Empty list when the user has no cart. */
    public List<CartLine> items(long userId) {
        Map<Object, Object> fields = redis.opsForHash().entries(key(userId));
        List<CartLine> lines = new ArrayList<>();
        fields.forEach((field, value) -> {
            if (!UPDATED_FIELD.equals(field)) {                                   // skip the bookkeeping field
                Stored s = json.readValue((String) value, Stored.class);
                lines.add(new CartLine(Long.valueOf((String) field), s.productName(), s.unitPrice(), s.quantity()));
            }
        });
        lines.sort(Comparator.comparing(CartLine::productId));
        return lines;
    }

    public Optional<CartLine> item(long userId, long productId) {
        Object value = redis.opsForHash().get(key(userId), String.valueOf(productId));
        if (value == null) {
            return Optional.empty();
        }
        Stored s = json.readValue((String) value, Stored.class);
        return Optional.of(new CartLine(productId, s.productName(), s.unitPrice(), s.quantity()));
    }

    /** Add the line, or replace it when the product is already in the cart. */
    public void put(long userId, CartLine line) {
        String key = key(userId);
        String value = json.writeValueAsString(new Stored(line.productName(), line.unitPrice(), line.quantity()));
        redis.opsForHash().put(key, String.valueOf(line.productId()), value);
        touch(key);
    }

    /** Remove one product. Returns false when it was not in the cart. */
    public boolean remove(long userId, long productId) {
        String key = key(userId);
        Long removed = redis.opsForHash().delete(key, String.valueOf(productId));
        if (removed == null || removed == 0) {
            return false;
        }
        if (redis.opsForHash().size(key) <= 1) {                                  // only _updatedAt is left: the cart is empty
            redis.delete(key);
        } else {
            touch(key);
        }
        return true;
    }

    public boolean exists(long userId) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId)));
    }

    public void clear(long userId) {
        redis.delete(key(userId));
    }

    /**
     * Deletes every cart that was not changed since {@code cutoff}. Called by the clean-up batch job.
     * SCAN walks over the keys in small steps (it never blocks Redis the way KEYS would).
     *
     * @return how many carts were deleted
     */
    public int deleteIdleSince(Instant cutoff) {
        List<String> idle = new ArrayList<>();
        try (Cursor<String> keys = redis.scan(ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(200).build())) {
            while (keys.hasNext()) {
                String key = keys.next();
                Object updated = redis.opsForHash().get(key, UPDATED_FIELD);
                if (updated == null || Instant.ofEpochMilli(Long.parseLong((String) updated)).isBefore(cutoff)) {
                    idle.add(key);
                }
            }
        }
        idle.forEach(redis::delete);
        return idle.size();
    }

    /** Remember when the cart changed and restart the 90-day expiry. */
    private void touch(String key) {
        redis.opsForHash().put(key, UPDATED_FIELD, String.valueOf(Instant.now().toEpochMilli()));
        redis.expire(key, MAX_IDLE);
    }
}
