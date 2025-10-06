package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private String address;
    private int cartItemCount;
    private String token;
}
