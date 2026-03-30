package com.example.devflow.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {
    private String description;
    private Boolean completed;
}