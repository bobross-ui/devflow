package com.example.devflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    @NotBlank(message = "description must not be blank")
    private String description;

}
