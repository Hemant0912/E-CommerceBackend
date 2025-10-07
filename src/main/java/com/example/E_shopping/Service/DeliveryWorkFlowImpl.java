package com.example.E_shopping.Service;

import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Repository.OrderRepository;
import io.temporal.workflow.Workflow;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

public class DeliveryWorkFlowImpl implements DeliveryWorkFlow {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void deliverOrder(Long orderId) {
        try {
            // Simulate delivery delay
            Workflow.sleep(Duration.ofMinutes(10));

            // Update order status
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setStatus("DELIVERED");
            orderRepository.save(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
