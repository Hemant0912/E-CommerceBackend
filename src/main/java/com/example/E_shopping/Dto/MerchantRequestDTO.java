package com.example.E_shopping.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MerchantRequestDTO {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Mobile cannot be empty")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number should be valid")
    private String mobile;

    @NotBlank(message = "Password cannot be empty")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$",
            message = "Password must contain uppercase, lowercase, digit, special character, and no spaces"
    )
    private String password;
}
