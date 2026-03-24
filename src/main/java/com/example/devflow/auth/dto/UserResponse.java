package com.example.devflow.auth.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private Integer weeklyGoalHours;
    private String preferredReportDay;
    private LocalDateTime createdAt;
}
