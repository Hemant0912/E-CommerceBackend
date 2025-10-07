package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Temporal.OrderWorkflow;
import com.example.E_shopping.util.JwtUtil;
import io.temporal.client.WorkflowClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WorkflowClient workflowClient;


    @Autowired
    private OrderRepository orderRepository;

    // Add to cart
    @Override
    public CartItemResponseDTO addToCart(String token, CartItemRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElse(new CartItem(null, user, product, 0));

        cartItem.setQuantity(cartItem.getQuantity() + dto.getQuantity());
        cartItemRepository.save(cartItem);

        return mapToDTO(cartItem);
    }

    // update cart
    @Override
    public CartItemResponseDTO updateCartItem(String token, Long productId, CartItemRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart"));

        cartItem.setQuantity(dto.getQuantity());
        cartItemRepository.save(cartItem);

        return mapToDTO(cartItem);
    }

    // remove from cart
    @Override
    public void removeCartItem(String token, Long productId) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart"));

        cartItemRepository.delete(cartItem);
    }

    // see all cart items
    @Override
    public List<CartItemResponseDTO> getCartItems(String token) {
        User user = getUserFromToken(token);
        return cartItemRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // see cart with total amount
    @Override
    public CartResponseWithTotalDTO getCartWithTotal(String token) {
        User user = getUserFromToken(token);

        List<CartItemResponseDTO> items = cartItemRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        double totalAmount = items.stream()
                .mapToDouble(CartItemResponseDTO::getTotalPrice)
                .sum();

        CartResponseWithTotalDTO response = new CartResponseWithTotalDTO();
        response.setItems(items);
        response.setTotalAmount(totalAmount);

        return response;
    }
// for payment
    public OrderResponseDTO checkout(String token) {
        User user = getUserFromToken(token);

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        double totalPrice = cartItems.stream()
                .mapToDouble(ci -> ci.getQuantity() * ci.getProduct().getPrice())
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setItems(cartItems);
        order.setTotalPrice(totalPrice);
        order.setStatus("PENDING");
        order.setPaymentId("PAY-" + System.currentTimeMillis());
        orderRepository.save(order); // Save to DB to get orderId

       // temporal will work
        OrderWorkflow workflow = workflowClient.newWorkflowStub(
                OrderWorkflow.class,
                io.temporal.client.WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build()
        );

        WorkflowClient.start(() -> workflow.processOrder(
                order.getId() != null ? order.getId().toString() : String.valueOf(System.currentTimeMillis()), // orderId
                totalPrice,  // total price
                user.getId().toString() // userId
        ));

       // this is for response
        OrderResponseDTO responseDTO = new OrderResponseDTO();
        responseDTO.setOrderId(order.getId());
        responseDTO.setItems(cartItems.stream().map(ci -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setProductId(ci.getProduct().getId());
            itemDTO.setProductName(ci.getProduct().getName());
            itemDTO.setQuantity(ci.getQuantity());
            itemDTO.setPrice(ci.getProduct().getPrice());
            itemDTO.setTotalPrice(ci.getQuantity() * ci.getProduct().getPrice());
            return itemDTO;
        }).toList());
        responseDTO.setTotalAmount(totalPrice);
        responseDTO.setStatus(order.getStatus());
        responseDTO.setPaymentId(order.getPaymentId());

        return responseDTO;
    }



    private CartItemResponseDTO mapToDTO(CartItem cartItem) {
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setPrice(cartItem.getProduct().getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setTotalPrice(cartItem.getQuantity() * cartItem.getProduct().getPrice());
        return dto;
    }

    private User getUserFromToken(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

}
