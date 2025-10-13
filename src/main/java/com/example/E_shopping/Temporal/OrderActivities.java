package com.example.E_shopping.Temporal;

import io.temporal.activity.ActivityInterface;

import java.time.LocalDateTime;

@ActivityInterface
public interface OrderActivities {
    boolean processPayment(String orderId, Double amount, String userId);
    void scheduleDelivery(String orderId, String userId);
    void updateOrderStatusWithTimestamp(String orderId, String status, LocalDateTime timestamp);

    void setEstimatedDeliveryDate(String orderId, LocalDateTime estimatedDate);

    void processRefund(String orderId);
}

