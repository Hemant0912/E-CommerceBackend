package com.example.E_shopping.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminViewMerchantDTO {
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String role;
}
