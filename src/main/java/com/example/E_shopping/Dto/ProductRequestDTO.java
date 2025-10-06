package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class ProductRequestDTO {
    private String name;
    private String type;        // e.g., phone, shirt
    private String category;    // ✅ e.g., electronics, clothing
    private String description;
    private String color;
    private double price;
    private int quantity;
    private Long merchantId;
}
