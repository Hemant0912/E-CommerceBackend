package com.example.E_shopping.Temporal;

import org.springframework.stereotype.Component;

@Component
public class OrderActivitiesImpl implements OrderActivities {

    @Override
    public boolean processPayment(String orderId, Double amount, String userId) {
        System.out.println("Processing payment of " + amount + " for order " + orderId);
        return true; // mock payment
    }

    @Override
    public void scheduleDelivery(String orderId, String userId) {
        System.out.println("Scheduling delivery for order " + orderId + " for user " + userId);
    }
}
