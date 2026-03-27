package com.example.devflow.session.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.devflow.auth.filter.CustomUserDetails;
import com.example.devflow.exception.EntityNotFoundException;
import com.example.devflow.session.dto.CreateSessionRequest;
import com.example.devflow.session.dto.SessionResponse;
import com.example.devflow.session.dto.UpdateSessionRequest;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.service.SessionService;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;
    private final UserService userService;

    private User getCurrentUser(CustomUserDetails userDetails) {
        return userService.findById(userDetails.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private SessionResponse toResponse(CodingSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .notes(session.getNotes())
                .status(session.getStatus().toString())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateSessionRequest request) {

        User user = getCurrentUser(userDetails);
        CodingSession session = sessionService.createSession(user, request.getTitle(), request.getNotes());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        
        CodingSession session = sessionService.getSessionByIdAndUser(id, userDetails.getUserId());
        return ResponseEntity.ok(toResponse(session));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionRequest request) {
        
        CodingSession session = sessionService.updateSession(id, userDetails.getUserId(), request.getTitle(), request.getNotes());
        return ResponseEntity.ok(toResponse(session));
    }

     @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        
        sessionService.deleteSession(id, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
