package com.ecommerce.common.security;

/**
 * The Redis key that holds a user's "token version" (a number, 0 if the key does not exist).
 *
 * Used by TWO services that must agree on the name:
 *   - user-service writes it (adds 1 when a password is changed or an account is deleted) and puts the current number
 *     into every new login token as the claim "ver";
 *   - api-gateway reads it on every request and rejects a token whose "ver" is lower than the number in Redis.
 */
public final class TokenVersionKeys {

    /** Redis forgets the number after 30 days: far longer than the 1 hour a login token is valid. */
    public static final long TTL_DAYS = 30;

    private TokenVersionKeys() {
    }

    public static String of(long userId) {
        return "ecommerce:token-version:" + userId;
    }
}
