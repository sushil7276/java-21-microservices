package com.ecommerce.favourite.logging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Makes requests easy to follow. For every request that reaches a service it writes two log lines:
 *      [order-service] <=== POST /api/orders/checkout (user 5)
 *      [order-service] ---> 201 POST /api/orders/checkout (84 ms)
 * and it adds the response header  X-Trace-Id  - the id that is the same in ALL services for one request.
 * Use it with  ./scripts/trace.sh <traceId>  to see the whole journey.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)      // after Spring's tracing filter, so the traceId already exists
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    private final String me;

    public RequestLogFilter(@Value("${spring.application.name}") String me) {
        this.me = me;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");          // do not flood the log with health checks
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            res.setHeader("X-Trace-Id", traceId);
        }
        String user = req.getHeader("X-User-Id");
        String what = req.getMethod() + " " + req.getRequestURI();
        log.info("[{}] <=== {}{}", me, what, user == null ? "" : " (user " + user + ")");
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            log.info("[{}] ---> {} {} ({} ms)", me, res.getStatus(), what, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
