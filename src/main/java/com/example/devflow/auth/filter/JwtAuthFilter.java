package com.example.devflow.auth.filter;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.devflow.auth.service.JwtService;
import com.example.devflow.exception.ErrorResponse;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                writeUnauthorized(response, request, "Invalid or expired token");
                return;
            }

            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);

            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            CustomUserDetails userDetails = new CustomUserDetails(userId, email, user.getPassword());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            writeUnauthorized(response, request, "Authentication failed");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request,
            String message) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(401)
                .error("UNAUTHORIZED")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}