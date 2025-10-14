package com.example.E_shopping.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int LIMIT = 10; // exactly 10 requests per minute

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String endpoint = request.getRequestURI();
        String userKey = request.getRemoteAddr(); // default for everyone

        // Use email as key for authenticated users
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth.isAuthenticated() && auth.getPrincipal() instanceof String email) {
                userKey = email;
            }
        }

        // Bucket key = user/IP + endpoint
        String bucketKey = userKey + ":" + endpoint;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> {
            // Refill only **once per minute**, no extra burst
            Refill refill = Refill.intervally(LIMIT, Duration.ofMinutes(1));
            Bandwidth limit = Bandwidth.classic(LIMIT, refill);
            return Bucket.builder().addLimit(limit).build();
        });

        if (bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"error\": \"Too many requests - limit is 10 per minute\"}");
        } catch (Exception e) {}

        return false;
    }
}
