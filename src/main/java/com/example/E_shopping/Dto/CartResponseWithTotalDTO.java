package com.example.E_shopping.Dto;

import lombok.Data;

import java.util.List;

@Data
public class CartResponseWithTotalDTO {
    private List<CartItemResponseDTO> items;
    private double totalAmount;
}
