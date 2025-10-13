package com.example.E_shopping.Dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile must start with 6-9 and be 10 digits")
    @NotBlank(message = "Mobile cannot be empty")
    private String mobile;

    @Size(min = 5, message = "Password must be at least 5 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[@#$%^&+=]).*$", message = "Password must contain number and special character")
    @NotBlank(message = "Password cannot be empty")
    private String password;

    @NotBlank(message = "Address cannot be empty")
    @Size(min = 10, message = "Address should be at least 10 characters long")
    @Pattern(regexp = "^[a-zA-Z0-9\\s,.-]+$", message = "Address contains invalid characters")
    private String address;
}
