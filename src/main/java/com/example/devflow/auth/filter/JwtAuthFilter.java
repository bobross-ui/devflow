package com.example.devflow.auth.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.devflow.auth.service.JwtService;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // No token provided — continue (SecurityConfig will reject if endpoint requires auth)
                filterChain.doFilter(request, response);
                return;
            }
            
            // Remove "Bearer " prefix to get the actual token
            String token = authHeader.substring(7);
            
            // Validate token
            if (!jwtService.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"status\": 401, \"error\": \"UNAUTHORIZED\", \"message\": \"Invalid or expired token\"}");
                response.setContentType("application/json");
                return;
            }
            
            // Extract user info from token
            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            
            // Load user from database to get password (for UserDetails)
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Create UserDetails and set in SecurityContext
            CustomUserDetails userDetails = new CustomUserDetails(userId, email, user.getPassword());
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\": 401, \"error\": \"UNAUTHORIZED\", \"message\": \"Authentication failed\"}");
            response.setContentType("application/json");
        }
    }
}