package com.example.devflow.task.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String description;
    private Boolean completed;
    private LocalDateTime createdAt;
}
