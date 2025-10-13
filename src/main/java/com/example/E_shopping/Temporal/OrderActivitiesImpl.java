package com.example.E_shopping.Temporal;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;


@Component
public class OrderActivitiesImpl implements OrderActivities {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public boolean processPayment(String orderId, Double amount, String userId) {
        System.out.println("Processing payment of " + amount + " for order " + orderId);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId("PAY-" + System.currentTimeMillis());

        for (CartItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("not enough stock for product: " + product.getName());
            }
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        orderRepository.save(order);
        return true;
    }

    @Override
    public void scheduleDelivery(String orderId, String userId) {
        System.out.println("Scheduling delivery for ordre " + orderId + " for user " + userId);
    }
    @Override
    public void updateOrderStatusWithTimestamp(String orderId, String status, LocalDateTime timestamp) {
        Order order = orderRepository.findByOrderId(orderId)
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
    public void setEstimatedDeliveryDate(String orderId, LocalDateTime estimatedDate) {
        Order order = orderRepository.findByOrderId(orderId) // use findByOrderId instead of findById
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setEstimatedDeliveryDate(estimatedDate);
        orderRepository.save(order);
    }


    @Override
    public void processRefund(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setRefundStatus("COMPLETED");
        order.setRefundAt(LocalDateTime.now());
        orderRepository.save(order);

        System.out.println("refund done for: " + order.getOrderId());
    }
}
