package com.ecommerce.product.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.ecommerce.product.dto.ProductResponse;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * REDIS PRODUCT CACHE
 * -------------------
 * Reading the catalogue is by far the most common request of a shop, and the answer rarely changes.
 * So the answers are kept in Redis (memory) for a few minutes and MySQL is only asked when Redis does not have them.
 *
 * How it works (Spring Cache abstraction):
 *   - ProductService methods marked @Cacheable  : look in Redis first; on a miss run the method and store the result.
 *   - ProductService methods marked @CacheEvict : delete the cached answers when the data changes.
 *
 * See what is stored:   docker exec ecommerce-redis redis-cli --scan --pattern 'ecommerce:*'
 *                       docker exec ecommerce-redis redis-cli get 'ecommerce:product::1'
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** One entry per product, key = product id. */
    public static final String PRODUCT = "product";
    /** One entry per distinct search (category + name), the value is a list of products. */
    public static final String PRODUCT_LIST = "products";

    /** An answer is thrown away after 10 minutes at the latest, even if nobody changed anything (safety net). */
    private static final Duration TTL = Duration.ofMinutes(10);

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // The values are stored as readable JSON (not Java binary), so you can look at them with redis-cli.
        JsonMapper json = JsonMapper.builder().build();
        JavaType listOfProducts = json.getTypeFactory().constructCollectionType(List.class, ProductResponse.class);

        RedisCacheConfiguration common = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .prefixCacheNameWith("ecommerce:")        // keys look like  ecommerce:product::1
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(common)
                .withCacheConfiguration(PRODUCT, common.serializeValuesWith(
                        SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(json, ProductResponse.class))))
                .withCacheConfiguration(PRODUCT_LIST, common.serializeValuesWith(
                        SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(json, listOfProducts))))
                // Cache changes are applied only AFTER the database transaction has committed,
                // so a reader can never put an old copy into Redis while the new one is being saved.
                .transactionAware()
                .build();
    }

    /**
     * If Redis is down the shop must still work: just log a warning and read from MySQL (slower, but correct)
     * instead of failing the request.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
