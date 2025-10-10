package com.example.E_shopping.Temporal;

import io.temporal.workflow.Workflow;
import io.temporal.activity.ActivityOptions;
import java.time.Duration;
import java.time.LocalDateTime;

public class OrderWorkflowImpl implements OrderWorkflow {

    @Override
    public void processOrder(String orderId, Double totalPrice, String userId) {
        OrderActivities activities = Workflow.newActivityStub(
                OrderActivities.class,
                ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build()
        );

        Long id = Long.parseLong(orderId);

        Workflow.sleep(Duration.ofDays(1));
        activities.updateOrderStatusWithTimestamp(id, "PREPARING", LocalDateTime.now());

        Workflow.sleep(Duration.ofDays(3));
        activities.updateOrderStatusWithTimestamp(id, "OUT_FOR_DELIVERY", LocalDateTime.now());

        Workflow.sleep(Duration.ofHours(4));
        activities.updateOrderStatusWithTimestamp(id, "DELIVERED", LocalDateTime.now());
    }

    @Override
    public void scheduleRefund(String orderId, String userId, int daysDelay) {
        OrderActivities activities = Workflow.newActivityStub(
                OrderActivities.class,
                ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build()
        );

        Long id = Long.parseLong(orderId);
        Workflow.sleep(Duration.ofDays(daysDelay)); // Wait 1 day before refund
        activities.processRefund(id);
    }
}
