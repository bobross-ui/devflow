package com.example.devflow.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.devflow.auth.dto.TokenResponse;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public User register(String email, String password, String displayName) {
        if (userService.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .displayName(displayName)
                .build();

        return userService.save(user);
    }
    
    public TokenResponse login(String email, String password) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900000L)
                .build();
    }
}