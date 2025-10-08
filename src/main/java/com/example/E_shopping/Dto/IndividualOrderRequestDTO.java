package com.example.E_shopping.Dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndividualOrderRequestDTO {
    private Long productId;
    private int quantity;
}
