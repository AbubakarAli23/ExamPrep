package com.examprep.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration input. The password upper bound matches BCrypt's 72-byte input limit.
 */
public record RegisterUserRequest(
        @NotBlank(message = "Please enter your full name")
        @Size(min = 2, max = 120, message = "Your name must be between 2 and 120 characters")
        String fullName,

        @NotBlank(message = "Please enter your email address")
        @Email(message = "Please enter a valid email address")
        @Size(max = 255, message = "That email address is too long")
        String email,

        @NotBlank(message = "Please choose a password")
        @Size(min = 8, max = 72, message = "Your password must be between 8 and 72 characters")
        String password) {
}
