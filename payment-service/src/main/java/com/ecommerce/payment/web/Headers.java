package com.ecommerce.payment.web;

/** Identity headers injected by the API gateway after the JWT has been validated. */
public final class Headers {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_NAME = "X-User-Name";
    public static final String USER_ROLE = "X-User-Role";

    private Headers() {
    }
}
