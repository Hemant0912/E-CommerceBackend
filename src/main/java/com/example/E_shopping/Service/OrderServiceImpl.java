package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.*;
import com.example.E_shopping.Repository.*;
import com.example.E_shopping.Temporal.OrderWorkflow;
import com.example.E_shopping.config.TemporalService;
import com.example.E_shopping.util.JwtUtil;
import com.example.E_shopping.util.OrderIdGenerator;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TemporalService temporalService;

    // ✅ Create a new order from cart
    @Override
    @Transactional
    public OrderResponseDTO createOrder(String token) {
        User user = getUserFromToken(token);

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        Map<Long, Integer> productQuantities = new HashMap<>();
        for (CartItem item : cartItems) {
            productQuantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }

        double totalPrice = 0;
        List<CartItem> finalItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            int quantity = entry.getValue();

            if (product.getQuantity() < quantity)
                throw new RuntimeException("Insufficient stock for " + product.getName());

            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);

            finalItems.add(new CartItem(null, user, product, quantity));
            totalPrice += product.getPrice() * quantity;
        }

        Order order = new Order();
        order.setUser(user);
        order.setItems(finalItems);
        order.setTotalPrice(totalPrice);
        order.setOrderId(OrderIdGenerator.generateOrderId());
        order.setStatus("PAID");
        order.setPaymentId(OrderIdGenerator.generatePaymentId());
        order.setOrderDate(LocalDateTime.now());
        order.setPaidAt(LocalDateTime.now());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(savedOrder.getOrderId(),
                                savedOrder.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        return mapToDTO(savedOrder);
    }

    // ✅ Pay for existing order
    @Override
    @Transactional
    public OrderResponseDTO payOrder(String token, String orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if (order.getStatus().equals("PAID") || order.getStatus().equals("DELIVERED"))
            throw new RuntimeException("Order already paid or delivered");

        for (CartItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (product.getQuantity() < item.getQuantity())
                throw new RuntimeException("Insufficient stock for: " + product.getName());
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId(OrderIdGenerator.generatePaymentId());
        orderRepository.save(order);

        cartItemRepository.deleteAll(order.getItems());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(order.getOrderId(),
                                order.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        return mapToDTO(order);
    }

    // ✅ Order single product
    @Override
    @Transactional
    public OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getQuantity() < dto.getQuantity())
            throw new RuntimeException("Insufficient stock for product: " + product.getName());

        product.setQuantity(product.getQuantity() - dto.getQuantity());
        productRepository.save(product);

        CartItem cartItem = new CartItem(null, user, product, dto.getQuantity());

        Order order = new Order();
        order.setUser(user);
        order.setItems(List.of(cartItem));
        order.setTotalPrice(product.getPrice() * dto.getQuantity());
        order.setOrderId(OrderIdGenerator.generateOrderId());
        order.setStatus("PAID");
        order.setPaymentId(OrderIdGenerator.generatePaymentId());
        order.setOrderDate(LocalDateTime.now());
        order.setPaidAt(LocalDateTime.now());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        Order savedOrder = orderRepository.save(order);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(savedOrder.getOrderId(),
                                savedOrder.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        return mapToDTO(savedOrder);
    }

    // ✅ Cancel order
    @Override
    @Transactional
    public void cancelOrder(String token, String orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if (order.getStatus().equals("DELIVERED"))
            throw new RuntimeException("Cannot cancel delivered order");

        order.setStatus("CANCELLED");
        order.setRefundStatus("PENDING");
        orderRepository.save(order);
    }

    // ✅ Return order
    @Override
    @Transactional
    public void returnOrder(String token, String orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if (!order.getStatus().equals("DELIVERED"))
            throw new RuntimeException("Only delivered orders can be returned");

        order.setStatus("RETURNED");
        order.setRefundStatus("PENDING");
        orderRepository.save(order);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.scheduleRefund(order.getOrderId(), user.getId().toString(), 1)); // 1-day delay
            }
        });
    }

    // ✅ Fetch all user orders
    @Override
    public List<OrderResponseDTO> getUserOrders(String token) {
        User user = getUserFromToken(token);
        return orderRepository.findByUser(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private User getUserFromToken(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(order.getStatus());
        dto.setPaymentId(order.getPaymentId());
        dto.setTotalAmount(order.getTotalPrice());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaidAt(order.getPaidAt());
        dto.setRefundAt(order.getRefundAt());
        dto.setRefundStatus(order.getRefundStatus());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        List<OrderItemDTO> items = order.getItems().stream().map(ci -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setProductId(ci.getProduct().getId());
            itemDTO.setProductName(ci.getProduct().getName());
            itemDTO.setPrice(ci.getProduct().getPrice());
            itemDTO.setQuantity(ci.getQuantity());
            itemDTO.setTotalPrice(ci.getQuantity() * ci.getProduct().getPrice());
            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
}
