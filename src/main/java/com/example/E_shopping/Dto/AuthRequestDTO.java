package com.example.E_shopping.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequestDTO {

    @NotBlank(message = "Email or Mobile cannot be empty")
    private String emailOrMobile;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
