package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CartServiceImpl implements CartService {

    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService;
    @Autowired private JwtUtil jwtUtil;

    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final long CART_TTL_HOURS = 48;

    @Override
    public CartItemResponseDTO addToCart(String token, CartItemRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem existing = cartItemRepository.findByUserAndProduct(user, product).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            cartItemRepository.save(existing);
            cacheCart(user.getId());
            return mapToDTO(existing);
        }

        CartItem newItem = new CartItem(null, user, product, dto.getQuantity());
        cartItemRepository.save(newItem);
        cacheCart(user.getId());
        return mapToDTO(newItem);
    }

    @Override
    public CartItemResponseDTO updateCartItem(String token, Long productId, CartItemRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Product not in cart"));

        cartItem.setQuantity(dto.getQuantity());
        cartItemRepository.save(cartItem);
        cacheCart(user.getId());
        return mapToDTO(cartItem);
    }

    @Override
    public void removeCartItem(String token, Long productId) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Product not in cart"));

        cartItemRepository.delete(cartItem);
        cacheCart(user.getId());
    }

    @Override
    public List<CartItemResponseDTO> getCartItems(String token) {
        User user = getUserFromToken(token);
        return getCartFromCache(user.getId());
    }

    @Override
    public CartResponseWithTotalDTO getCartWithTotal(String token) {
        List<CartItemResponseDTO> items = getCartItems(token);
        double totalAmount = items.stream().mapToDouble(CartItemResponseDTO::getTotalPrice).sum();

        CartResponseWithTotalDTO response = new CartResponseWithTotalDTO();
        response.setItems(items);
        response.setTotalAmount(totalAmount);
        return response;
    }

    @Override
    @Transactional
    public List<OrderResponseDTO> checkout(String token) {
        User user = getUserFromToken(token);
        List<OrderResponseDTO> orders = orderService.createOrder(token);
        redisTemplate.delete("CART:" + user.getId());
        return orders;
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
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void cacheCart(Long userId) {
        List<CartItemResponseDTO> items = cartItemRepository.findByUser(userRepository.findById(userId).get())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
        redisTemplate.opsForValue().set("CART:" + userId, items, CART_TTL_HOURS, TimeUnit.HOURS);
    }

    private List<CartItemResponseDTO> getCartFromCache(Long userId) {
        Object cachedData = redisTemplate.opsForValue().get("CART:" + userId);

        if (cachedData == null) {
            cacheCart(userId);
            cachedData = redisTemplate.opsForValue().get("CART:" + userId);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<CartItemResponseDTO> items = mapper.convertValue(
                cachedData,
                new TypeReference<List<CartItemResponseDTO>>() {}
        );

        return items;
    }
}
