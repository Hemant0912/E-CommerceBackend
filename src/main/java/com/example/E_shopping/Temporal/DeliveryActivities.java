package com.example.E_shopping.Temporal;

import io.temporal.activity.ActivityInterface;
import java.time.LocalDateTime;

@ActivityInterface
public interface DeliveryActivities {
    void updateOrderStatusWithTimestamp(Long orderId, String status, LocalDateTime timestamp);
    void setEstimatedDeliveryDate(Long orderId, LocalDateTime estimatedDate);
}
