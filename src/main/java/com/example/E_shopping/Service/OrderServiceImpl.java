package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.OrderItemDTO;
import com.example.E_shopping.Dto.OrderResponseDTO;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.config.TemporalService;
import com.example.E_shopping.util.JwtUtil;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TemporalService temporalService; // Add your Temporal service bean

   // creation order
    @Override
    public OrderResponseDTO createOrder(String token) {
        User user = getUserFromToken(token);

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double total = cartItems.stream()
                .mapToDouble(ci -> ci.getQuantity() * ci.getProduct().getPrice())
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setItems(cartItems);
        order.setTotalPrice(total);
        order.setStatus("PENDING");

        Order savedOrder = orderRepository.save(order);

        // Optionally clear the cart after placing order
        cartItemRepository.deleteAll(cartItems);

        return mapToDTO(savedOrder);
    }

    // payment of order
    @Override
    public OrderResponseDTO payOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().equals(user)) {
            throw new RuntimeException("Not your order");
        }

        order.setStatus("PAID");
        order.setPaymentId("PAY-" + System.currentTimeMillis());
        orderRepository.save(order);

// temporal for delivery status
        WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("DeliveryTaskQueue")
                .build();

        DeliveryWorkFlow workflow = client.newWorkflowStub(DeliveryWorkFlow.class, options);

        WorkflowClient.start(() -> workflow.deliverOrder(order.getId()));

        return mapToDTO(order);

    }

    // cancel order
    @Override
    public void cancelOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().equals(user)) {
            throw new RuntimeException("Not your order");
        }
        if (order.getStatus().equals("DELIVERED")) {
            throw new RuntimeException("Cannot cancel delivered order");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    //return order
    @Override
    public void returnOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().equals(user)) {
            throw new RuntimeException("Not your order");
        }
        if (!order.getStatus().equals("DELIVERED")) {
            throw new RuntimeException("Only delivered orders can be returned");
        }

        order.setStatus("RETURNED");
        orderRepository.save(order);
    }

    //  all order of a user
    @Override
    public List<OrderResponseDTO> getUserOrders(String token) {
        User user = getUserFromToken(token);

        return orderRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setPaymentId(order.getPaymentId());
        dto.setTotalAmount(order.getTotalPrice());

        List<OrderItemDTO> items = order.getItems().stream().map(ci -> {
            OrderItemDTO item = new OrderItemDTO();
            item.setProductId(ci.getProduct().getId());
            item.setProductName(ci.getProduct().getName());
            item.setPrice(ci.getProduct().getPrice());
            item.setQuantity(ci.getQuantity());
            item.setTotalPrice(ci.getQuantity() * ci.getProduct().getPrice());
            return item;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    // see user if exists
    private User getUserFromToken(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
