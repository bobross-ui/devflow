package com.example.devflow.ratelimit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.devflow.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    // Separate maps for auth (per IP) and API (per user) buckets
    private final Map<String, TokenBucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        TokenBucket bucket;

        if (isAuthEndpoint(path)) {
            // Per IP - 10 request/minute
            String ip = getClientIp(request);
            bucket = authBuckets.computeIfAbsent(ip, k -> new TokenBucket(10, 10.0 / 60.0));
        } else {
            // Per user - 60 requests/minute
            // Use IP as fallback for unauthenticated requests
            String userId = getUserIdentifier(request);
            bucket = apiBuckets.computeIfAbsent((userId), k -> new TokenBucket(60, 60.0 / 60.0));
        }

        if (!bucket.tryConsume()) {
            writeTooManyRequests(response, request, bucket.getSecondsUntilRefill());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String path) {
        return path.contains("/auth/login") || path.contains("/auth/register");
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String getUserIdentifier(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "user: " + auth.substring(7, Math.min(auth.length(), 20));
        }
        return "ip:" + getClientIp(request);
    }

    private void writeTooManyRequests(HttpServletResponse response,
            HttpServletRequest request, long retryAfter) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(429)
                .error("TOO_MANY_REQUESTS")
                .message("Rate limit exceeded. Try again in " + retryAfter + " seconds")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
