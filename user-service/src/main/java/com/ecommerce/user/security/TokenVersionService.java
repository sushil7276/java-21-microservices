package com.ecommerce.user.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.common.security.TokenVersionKeys;

/**
 * REDIS TOKEN REVOCATION
 * ----------------------
 * A JWT is valid until it expires (1 hour), and nobody can "take it back" because the server keeps no list of tokens.
 * The trick used here: every user has a number in Redis (the token version).
 *
 *   login            -> the token gets the user's current number inside it   (claim "ver")
 *   password changed -> the number goes up by 1                              (revokeAll)
 *   every request    -> the gateway compares: number in token  <  number in Redis ?  then the token is refused (401)
 *
 * So all old tokens of that user stop working at once, while new logins (which carry the new number) work.
 *
 *   docker exec ecommerce-redis redis-cli get ecommerce:token-version:14
 */
@Service
public class TokenVersionService {

    private static final Logger log = LoggerFactory.getLogger(TokenVersionService.class);

    private final StringRedisTemplate redis;

    public TokenVersionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** The number to put into a new token (0 when the user never had a revocation). */
    public long current(long userId) {
        try {
            String value = redis.opsForValue().get(TokenVersionKeys.of(userId));
            return value == null ? 0 : Long.parseLong(value);
        } catch (RuntimeException e) {
            log.warn("Redis not reachable, issuing the token with version 0: {}", e.toString());
            return 0;
        }
    }

    /** Makes every token issued so far for this user invalid. */
    public void revokeAll(long userId) {
        try {
            String key = TokenVersionKeys.of(userId);
            redis.opsForValue().increment(key);
            redis.expire(key, Duration.ofDays(TokenVersionKeys.TTL_DAYS));
        } catch (RuntimeException e) {
            // Do not block the password change just because Redis is down, but make the problem visible.
            log.error("Could NOT revoke the old tokens of user {} (Redis not reachable): {}", userId, e.toString());
        }
    }
}
