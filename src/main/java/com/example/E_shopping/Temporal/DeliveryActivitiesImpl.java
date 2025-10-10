package com.example.E_shopping.Temporal;
import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class DeliveryActivitiesImpl implements DeliveryActivities {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void updateOrderStatusWithTimestamp(Long orderId, String status, LocalDateTime timestamp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        switch (status) {
            case "PAID": order.setPaidAt(timestamp); break;
            case "PREPARING": order.setPreparingAt(timestamp); break;
            case "OUT_FOR_DELIVERY": order.setOutForDeliveryAt(timestamp); break;
            case "DELIVERED": order.setDeliveredAt(timestamp); break;
        }

        orderRepository.save(order);
    }

    @Override
    public void setEstimatedDeliveryDate(Long orderId, LocalDateTime estimatedDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setEstimatedDeliveryDate(estimatedDate);
        orderRepository.save(order);
    }
}
