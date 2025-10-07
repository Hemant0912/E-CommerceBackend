package com.example.E_shopping.Temporal;

import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Repository.OrderRepository;
import io.temporal.workflow.Workflow;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {

    private final OrderRepository orderRepository;

    public OrderWorkflowImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void processOrder(String orderId, Double totalPrice, String userId) {
        try {
            Long id = Long.parseLong(orderId); // convert String to Long

            // temporal paid
            updateOrderStatus(id, "PAID");

            // temporal
            Workflow.sleep(Duration.ofMinutes(5));
            updateOrderStatus(id, "DELIVERING");

            //temporal delivered
            Workflow.sleep(Duration.ofDays(3));
            updateOrderStatus(id, "DELIVERED");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }
}