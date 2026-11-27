package com.example.sampleapi.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotNull(message = "age is required")
        @Min(value = 1, message = "age must be at least 1")
        @Max(value = 120, message = "age must be at most 120")
        Integer age
) {
}
