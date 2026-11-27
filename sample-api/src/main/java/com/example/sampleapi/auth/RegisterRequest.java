package com.example.sampleapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password
) {
}
