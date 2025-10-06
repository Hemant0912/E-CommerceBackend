package com.example.E_shopping.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;  // changed to String
    private String name;
    private String email;
    private String mobile;
    private String role;
    private String address;
}
