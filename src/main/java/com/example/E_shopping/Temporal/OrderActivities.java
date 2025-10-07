package com.example.E_shopping.Temporal;

public interface OrderActivities {
    boolean processPayment(String orderId, Double amount, String userId);
    void scheduleDelivery(String orderId, String userId);
}

