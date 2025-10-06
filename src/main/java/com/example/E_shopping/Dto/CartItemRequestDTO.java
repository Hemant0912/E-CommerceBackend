package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class CartItemRequestDTO {
    private Long productId;
    private Integer quantity; // quantity to add or update
}

