package com.example.E_shopping.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private String orderId;
    private List<OrderItemDTO> items;
    private Double totalAmount;
    private String status;
    private String paymentId;
    // timestamps for client display
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime paidAt;
    private LocalDateTime preparingAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;

    // IMPORTANT: real field for order date
    private LocalDateTime orderDate;
}
