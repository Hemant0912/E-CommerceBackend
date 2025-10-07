package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private Double price;
    private int quantity;
    private Double totalPrice;
}