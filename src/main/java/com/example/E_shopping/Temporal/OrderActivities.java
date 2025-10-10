package com.example.E_shopping.Temporal;

import io.temporal.activity.ActivityInterface;

import java.time.LocalDateTime;

@ActivityInterface
public interface OrderActivities {
    boolean processPayment(String orderId, Double amount, String userId);
    void scheduleDelivery(String orderId, String userId);
    void updateOrderStatusWithTimestamp(Long orderId, String status, LocalDateTime timestamp);

    void setEstimatedDeliveryDate(Long orderId, LocalDateTime estimatedDate);

    void processRefund(Long orderId);
}

