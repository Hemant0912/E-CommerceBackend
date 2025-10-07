package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private String userId;
    private Double totalPrice;
}
