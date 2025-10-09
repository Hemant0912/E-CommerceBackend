package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.*;
import com.example.E_shopping.util.OrderIdGenerator;
import com.example.E_shopping.Repository.*;
import com.example.E_shopping.Temporal.OrderWorkflow;
import com.example.E_shopping.config.TemporalService;
import com.example.E_shopping.util.JwtUtil;
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

    @Override
    @Transactional
    public OrderResponseDTO createOrder(String token) {
        User user = getUserFromToken(token);

        // 1️⃣ Fetch all cart items
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        // 2️⃣ Merge duplicates in memory
        Map<Long, Integer> productQuantities = new HashMap<>();
        for (CartItem item : cartItems) {
            productQuantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }

        List<CartItem> finalItems = new ArrayList<>();

        double totalPrice = 0;

        // 3️⃣ Deduct stock and prepare final list
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for " + product.getName());
            }

            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);

            CartItem item = new CartItem(null, user, product, quantity);
            finalItems.add(item);
            totalPrice += product.getPrice() * quantity;
        }

        // 4️⃣ Create new order
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

        // 5️⃣ Clear user's cart completely
        cartItemRepository.deleteAll(cartItems);

        // 6️⃣ Start Temporal workflow
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(
                                savedOrder.getId().toString(),
                                savedOrder.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        // 7️⃣ Return order with correct orderId & payment info
        return mapToDTO(savedOrder);
    }




    // ✅ payOrder implementation
    @Override
    @Transactional
    public OrderResponseDTO payOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user))
            throw new RuntimeException("Not your order");
        if (order.getStatus().equals("PAID") || order.getStatus().equals("DELIVERED"))
            throw new RuntimeException("Order already paid or delivered");

        // Deduct stock
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

        // Clear cart items
        cartItemRepository.deleteAll(order.getItems());

        // Temporal workflow
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(order.getId().toString(),
                                order.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        return mapToDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getQuantity() < dto.getQuantity())
            throw new RuntimeException("Insufficient stock for product: " + product.getName());

        // Deduct stock
        product.setQuantity(product.getQuantity() - dto.getQuantity());
        productRepository.save(product);

        // ✅ Create CartItem properly linked to user & product
        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(dto.getQuantity());
        cartItem.setUser(user);

        // ✅ Create and link order
        Order order = new Order();
        order.setUser(user);
        order.setItems(List.of(cartItem)); // link the single item
        order.setTotalPrice(product.getPrice() * dto.getQuantity());
        order.setOrderId(OrderIdGenerator.generateOrderId());
        order.setStatus("PAID");
        order.setPaymentId(OrderIdGenerator.generatePaymentId());
        order.setOrderDate(LocalDateTime.now());
        order.setPaidAt(LocalDateTime.now());
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));

        // ✅ Save order (will cascade CartItem)
        Order savedOrder = orderRepository.save(order);

        // ✅ Start Temporal workflow after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowClient client = WorkflowClient.newInstance(temporalService.getService());
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build();
                OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options);
                WorkflowClient.start(() ->
                        workflow.processOrder(
                                savedOrder.getId().toString(),
                                savedOrder.getTotalPrice(),
                                user.getId().toString()));
            }
        });

        return mapToDTO(savedOrder);
    }


    @Override @Transactional
    public void cancelOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user)) throw new RuntimeException("Not your order");
        if (order.getStatus().equals("DELIVERED")) throw new RuntimeException("Cannot cancel delivered order");

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    @Override @Transactional
    public void returnOrder(String token, Long orderId) {
        User user = getUserFromToken(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().equals(user)) throw new RuntimeException("Not your order");
        if (!order.getStatus().equals("DELIVERED")) throw new RuntimeException("Only delivered orders can be returned");

        order.setStatus("RETURNED");
        orderRepository.save(order);
    }

    @Override
    public List<OrderResponseDTO> getUserOrders(String token) {
        User user = getUserFromToken(token);
        return orderRepository.findByUser(user).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getOrderId() != null ? order.getOrderId() : "ORD-" + order.getId());
        dto.setStatus(order.getStatus());
        dto.setPaymentId(order.getPaymentId());
        dto.setTotalAmount(order.getTotalPrice());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaidAt(order.getPaidAt());
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

    private User getUserFromToken(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
