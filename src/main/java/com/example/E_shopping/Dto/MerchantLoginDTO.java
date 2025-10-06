package com.example.E_shopping.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MerchantLoginDTO {

    @NotBlank(message = "Email or Mobile cannot be empty")
    private String emailOrMobile;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
