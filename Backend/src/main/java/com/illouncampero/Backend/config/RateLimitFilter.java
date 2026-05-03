package com.illouncampero.Backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket(int tokens, Duration duration) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(tokens, Refill.intervally(tokens, duration)))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        // Límites más estrictos en endpoints sensibles
        int maxRequests;
        if (path.contains("/api/pedidos/realizar-pedido") || path.contains("/api/pagos")) {
            maxRequests = 10;
        } else if (path.contains("/api/cupones/validar")) {
            maxRequests = 20;
        } else {
            maxRequests = 60;
        }

        Bucket bucket = buckets.computeIfAbsent(
                ip + "|" + path,
                k -> createBucket(maxRequests, Duration.ofMinutes(1))
        );

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demasiadas peticiones. Espera un momento.\"}");
        }
    }
}
