package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String type;
    private String category;
    private String color;
    private double price;
    private int quantity;
    private String description;
    private Long merchantId;
    private String merchantName;
}
