package com.example.devflow.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTagRequest {

    @NotBlank(message = "tag name must not be blank")
    private String name;
}
