package com.ecommerce.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import reactor.core.publisher.Mono;

/**
 * REDIS TOKEN REVOCATION: the check
 * ---------------------------------
 * Wraps the normal JWT decoder. After the signature and the expiry date were verified, it asks Redis for the user's
 * current token version and refuses the token when the version inside the token ("ver") is lower.
 * (The numbers are written by user-service, see TokenVersionService there.)
 *
 * A refused token ends as HTTP 401, exactly like an expired one, and the website signs the user out.
 */
public class RevocationCheckingJwtDecoder implements ReactiveJwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(RevocationCheckingJwtDecoder.class);

    /**
     * Must be the same key that user-service writes: com.ecommerce.common.security.TokenVersionKeys.of(userId).
     * (The gateway does not depend on the shared "common" library, which is built for the MVC services, so the
     * one line is repeated here.)
     */
    private static final String KEY_PREFIX = "ecommerce:token-version:";

    private final ReactiveJwtDecoder delegate;
    private final ReactiveStringRedisTemplate redis;

    public RevocationCheckingJwtDecoder(ReactiveJwtDecoder delegate, ReactiveStringRedisTemplate redis) {
        this.delegate = delegate;
        this.redis = redis;
    }

    @Override
    public Mono<Jwt> decode(String token) throws org.springframework.security.oauth2.jwt.JwtException {
        return delegate.decode(token).flatMap(jwt -> {
            Number userId = jwt.getClaim("uid");
            if (userId == null) {
                return Mono.just(jwt);
            }
            long versionInToken = jwt.getClaim("ver") instanceof Number n ? n.longValue() : 0;   // old tokens have no "ver"

            return redis.opsForValue().get(KEY_PREFIX + userId.longValue())
                    .map(Long::parseLong)
                    .defaultIfEmpty(0L)                       // no number in Redis = nothing was ever revoked
                    .onErrorResume(e -> {                     // Redis down: keep the shop working, but say so
                        log.warn("Redis not reachable, token revocation is NOT checked: {}", e.toString());
                        return Mono.just(0L);
                    })
                    .flatMap(currentVersion -> versionInToken < currentVersion
                            ? Mono.<Jwt>error(new BadJwtException("Token has been revoked"))
                            : Mono.just(jwt));
        });
    }
}
