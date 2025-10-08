package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.IndividualOrderRequestDTO;
import com.example.E_shopping.Dto.OrderItemDTO;
import com.example.E_shopping.Dto.OrderResponseDTO;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.config.TemporalService;
import com.example.E_shopping.Temporal.OrderWorkflow;
import com.example.E_shopping.util.JwtUtil;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TemporalService temporalService;

    // 1️⃣ CREATE ORDER FROM CART
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
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        Order savedOrder = orderRepository.save(order);

        // Clear cart after placing order
        cartItemRepository.deleteAll(cartItems);

        return mapToDTO(savedOrder);
    }

    // 2️⃣ PAY ORDER (FROM CART)
    @Override
    public OrderResponseDTO payOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().equals(user)) {
            throw new RuntimeException("Not your order");
        }
        if (order.getStatus().equals("PAID") || order.getStatus().equals("DELIVERED")) {
            throw new RuntimeException("Order already paid or delivered");
        }

        // Deduct stock
        for (CartItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        // Update order payment info
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId("PAY-" + System.currentTimeMillis());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        orderRepository.save(order);

        // Start Temporal workflow for delivery
        WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                .build();

        OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
        WorkflowClient.start(() -> workflow.processOrder(order.getId().toString(), order.getTotalPrice(), order.getUser().getId().toString()));

        return mapToDTO(order);
    }

    // 3️⃣ ORDER SINGLE ITEM (direct, no cart)
    @Override
    public OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getQuantity() < dto.getQuantity()) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        double totalPrice = product.getPrice() * dto.getQuantity();

        // Deduct stock immediately
        product.setQuantity(product.getQuantity() - dto.getQuantity());
        productRepository.save(product);

        // Create cart item for order
        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(dto.getQuantity());

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setItems(List.of(cartItem));
        order.setTotalPrice(totalPrice);
        order.setStatus("PAID"); // direct payment
        order.setPaymentId("PAY-" + System.currentTimeMillis());
        order.setOrderDate(LocalDateTime.now());
        order.setPaidAt(LocalDateTime.now());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        orderRepository.save(order);

        // Start Temporal workflow
        WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                .build();

        OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
        WorkflowClient.start(() -> workflow.processOrder(order.getId().toString(), totalPrice, user.getId().toString()));

        // Map to DTO for response
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setProductId(product.getId());
        itemDTO.setProductName(product.getName());
        itemDTO.setQuantity(dto.getQuantity());
        itemDTO.setPrice(product.getPrice());
        itemDTO.setTotalPrice(totalPrice);

        OrderResponseDTO responseDTO = new OrderResponseDTO();
        responseDTO.setOrderId(order.getId());
        responseDTO.setItems(List.of(itemDTO));
        responseDTO.setTotalAmount(totalPrice);
        responseDTO.setStatus(order.getStatus());
        responseDTO.setPaymentId(order.getPaymentId());
        responseDTO.setOrderDate(order.getOrderDate());
        responseDTO.setPaidAt(order.getPaidAt());
        responseDTO.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        return responseDTO;
    }

    // CANCEL ORDER
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

    // RETURN ORDER
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

    // GET USER ORDERS
    @Override
    public List<OrderResponseDTO> getUserOrders(String token) {
        User user = getUserFromToken(token);
        return orderRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // HELPER: MAP ORDER TO DTO
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
        dto.setOrderDate(order.getOrderDate());
        dto.setPaidAt(order.getPaidAt());
        dto.setPreparingAt(order.getPreparingAt());
        dto.setOutForDeliveryAt(order.getOutForDeliveryAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        return dto;
    }

    // HELPER: GET USER FROM TOKEN
    private User getUserFromToken(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
