package com.examprep.web.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "Please enter your full name")
    @Size(min = 2, max = 120, message = "Your name must be between 2 and 120 characters")
    private String fullName;

    @NotBlank(message = "Please enter your email address")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "That email address is too long")
    private String email;

    @NotBlank(message = "Please choose a password")
    @Size(min = 8, max = 72, message = "Your password must be between 8 and 72 characters")
    private String password;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
