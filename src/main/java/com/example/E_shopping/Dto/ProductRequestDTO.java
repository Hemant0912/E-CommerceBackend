package com.example.E_shopping.Dto;

import lombok.Data;

@Data
public class ProductRequestDTO {
    private String name;
    private String type;
    private String category;
    private String description;
    private String color;
    private Double price;
    private Integer quantity; // legacy field
    private Integer stock;    // new field for updates
    private Long merchantId;
}


