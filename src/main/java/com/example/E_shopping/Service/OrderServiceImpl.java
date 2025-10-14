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
import com.example.E_shopping.Temporal.OrderWorkflow;
import com.example.E_shopping.config.TemporalService;
import com.example.E_shopping.util.JwtUtil;
import com.example.E_shopping.util.OrderIdGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TemporalService temporalService;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final long ORDER_PENDING_TTL_HOURS = 2;
    private static final String ORDER_SERVICE_CB = "orderServiceCircuitBreaker";

    @Override
    @Transactional
    @CircuitBreaker(name = ORDER_SERVICE_CB, fallbackMethod = "createOrderFallback")
    public List<OrderResponseDTO> createOrder(String token) {
        User user = getUserFromToken(token);
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        List<OrderResponseDTO> responseList = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getQuantity() < item.getQuantity())
                throw new RuntimeException("Insufficient stock for " + product.getName());

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);

            Order order = new Order();
            order.setUser(user);
            order.setItems(List.of(item));
            order.setTotalPrice(product.getPrice() * item.getQuantity());
            order.setOrderId(OrderIdGenerator.generateOrderId());
            order.setStatus("PAID");
            order.setPaymentId(OrderIdGenerator.generatePaymentId());
            order.setOrderDate(LocalDateTime.now());
            order.setPaidAt(LocalDateTime.now());
            order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

            Order savedOrder = orderRepository.save(order);
            responseList.add(mapToDTO(savedOrder));

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                    WorkflowOptions options = WorkflowOptions.newBuilder()
                            .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                            .build();
                    OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                    WorkflowClient.start(() -> workflow.processOrder(savedOrder.getOrderId(),
                            savedOrder.getTotalPrice(), user.getId().toString()));
                }
            });

            redisTemplate.opsForValue().set("ORDER_PENDING:" + order.getOrderId(), order,
                    ORDER_PENDING_TTL_HOURS, TimeUnit.HOURS);
        }

        // clear cart items (DB + Redis)
        cartItemRepository.deleteAll(cartItems);
        redisTemplate.delete("CART:" + user.getId());

        return responseList;
    }



    public OrderResponseDTO createOrderFallback(String token, Throwable t) {
        t.printStackTrace(); // full exception log
        throw new RuntimeException("Order service temporarily unavailable. Reason: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = ORDER_SERVICE_CB, fallbackMethod = "payOrderFallback")
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
        redisTemplate.delete("ORDER_PENDING:" + orderId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() -> workflow.processOrder(order.getOrderId(),
                        order.getTotalPrice(), user.getId().toString()));
            }
        });

        return mapToDTO(order);
    }

    public OrderResponseDTO payOrderFallback(String token, String orderId, Throwable t) {
        t.printStackTrace();
        throw new RuntimeException("Payment service temporarily unavailable. Reason: " + t.getMessage(), t);
    }

    @Override
    @Transactional
    public void cancelOrder(String token, String orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if ("DELIVERED".equals(order.getStatus()))
            throw new RuntimeException("Cannot cancel delivered order");

        order.setStatus("CANCELLED");
        order.setRefundStatus("PENDING");
        orderRepository.save(order);
        redisTemplate.delete("ORDER_PENDING:" + orderId);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = ORDER_SERVICE_CB, fallbackMethod = "returnOrderFallback")
    public void returnOrder(String token, String orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if (!"DELIVERED".equals(order.getStatus()))
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
                WorkflowClient.start(() -> workflow.scheduleRefund(order.getOrderId(),
                        user.getId().toString(), 1));
            }
        });
    }

    public void returnOrderFallback(String token, String orderId, Throwable t) {
        t.printStackTrace();
        throw new RuntimeException("Return service temporarily unavailable. Reason: " + t.getMessage(), t);
    }

    @Override
    public List<OrderResponseDTO> getUserOrders(String token) {
        User user = getUserFromToken(token);
        return orderRepository.findByUser(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<OrderResponseDTO> getUserOrdersPaginated(String token, int page, int size, String sortBy) {
        User user = getUserFromToken(token);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return orderRepository.findByUser(user, pageable).map(this::mapToDTO);
    }
    @Override
    @Transactional
    @CircuitBreaker(name = ORDER_SERVICE_CB, fallbackMethod = "orderSingleItemFallback")
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
                WorkflowClient.start(() -> workflow.processOrder(savedOrder.getOrderId(),
                        savedOrder.getTotalPrice(), user.getId().toString()));
            }
        });

        return mapToDTO(savedOrder);
    }

    public OrderResponseDTO orderSingleItemFallback(String token, IndividualOrderRequestDTO dto, Throwable t) {
        t.printStackTrace();
        throw new RuntimeException("Order service temporarily unavailable. Reason: " + t.getMessage(), t);
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
