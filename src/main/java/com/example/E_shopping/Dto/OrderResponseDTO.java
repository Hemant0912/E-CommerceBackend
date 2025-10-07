package com.example.E_shopping.Dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private List<OrderItemDTO> items;
    private Double totalAmount;
    private String status;  // this will show status like pending paid etc
    private String paymentId;
}
