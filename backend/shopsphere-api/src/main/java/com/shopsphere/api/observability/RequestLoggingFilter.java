package com.shopsphere.api.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id (honouring a sane incoming {@code X-Request-Id}), puts it in the
 * logging MDC and the response header, and writes one access-log line per request:
 * method, path, status, duration. Query strings, headers, bodies and tokens are never logged.
 * Runs before Spring Security so 401/403 responses are logged and carry the id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();
            String path = request.getRequestURI();
            if (path.startsWith("/actuator/")) {
                // Prometheus scrapes constantly; keep them out of INFO.
                log.debug("{} {} -> {} ({} ms)", request.getMethod(), path, status, ms);
            } else if (status >= 500) {
                log.error("{} {} -> {} ({} ms)", request.getMethod(), path, status, ms);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms)", request.getMethod(), path, status, ms);
            } else {
                log.info("{} {} -> {} ({} ms)", request.getMethod(), path, status, ms);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
