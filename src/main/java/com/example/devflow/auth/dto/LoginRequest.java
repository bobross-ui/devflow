package com.example.devflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password must not be blank")
    private String password;
}
