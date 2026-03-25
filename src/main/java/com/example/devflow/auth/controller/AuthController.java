package com.example.devflow.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.devflow.auth.dto.LoginRequest;
import com.example.devflow.auth.dto.RegisterRequest;
import com.example.devflow.auth.dto.TokenResponse;
import com.example.devflow.auth.dto.UserResponse;
import com.example.devflow.auth.service.AuthService;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(),request.getPassword(),request.getDisplayName());
        
        UserResponse response = UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .displayName(user.getDisplayName())
                                .weeklyGoalHours(user.getWeeklyGoalHours())
                                .preferredReportDay(user.getPreferredReportDay())
                                .createdAt(user.getCreatedAt())
                                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
    

}
