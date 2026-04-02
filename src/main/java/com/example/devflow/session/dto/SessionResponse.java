package com.example.devflow.session.dto;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private String title;
    private String notes;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> tags;
}