package dev.jacid.hrApplication.infrastructure.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Request correlation id. Reuses the caller's {@code X-Request-Id} (e.g. from a gateway or the frontend) when it is
 * a short token, otherwise generates a UUID; returns it in the response header and puts it in the logging MDC as
 * {@code requestId}, so every log line of the request (JSON logs included) can be correlated.
 * Runs before Spring Security, so 401/403 responses carry the id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /** Accepted incoming ids: no spaces or control characters (log injection) and a bounded length. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolve(request.getHeader(HEADER));
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static String resolve(String incoming) {
        return incoming != null && VALID_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
