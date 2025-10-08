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
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofMinutes(5))
                        .build()
        );

        Long id = Long.parseLong(orderId);

        // Pending → Paid after 1 min
        Workflow.sleep(Duration.ofMinutes(1));
        activities.updateOrderStatusWithTimestamp(id, "PAID", LocalDateTime.now());

        // Paid → Preparing after 1 day
        Workflow.sleep(Duration.ofDays(1));
        activities.updateOrderStatusWithTimestamp(id, "PREPARING", LocalDateTime.now());

        // Preparing → Out for delivery after 3 days
        Workflow.sleep(Duration.ofDays(3));
        activities.updateOrderStatusWithTimestamp(id, "OUT_FOR_DELIVERY", LocalDateTime.now());

        // Out for delivery → Delivered after 4 hours
        Workflow.sleep(Duration.ofHours(4));
        activities.updateOrderStatusWithTimestamp(id, "DELIVERED", LocalDateTime.now());

        }
        }


