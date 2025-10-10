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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService; // delegate checkout

    @Autowired private JwtUtil jwtUtil;

    @Override
    public CartItemResponseDTO addToCart(String token, CartItemRequestDTO dto) {
        User user = getUserFromToken(token);
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // for quantity increasing
        CartItem existing = cartItemRepository.findByUserAndProduct(user, product).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            cartItemRepository.save(existing);
            return mapToDTO(existing);
        }

        // new item creation
        CartItem newItem = new CartItem(null, user, product, dto.getQuantity());
        cartItemRepository.save(newItem);
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
    }

    @Override
    public List<CartItemResponseDTO> getCartItems(String token) {
        User user = getUserFromToken(token);
        return cartItemRepository.findByUser(user)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
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

    // payment one
    @Override
    @Transactional
    public OrderResponseDTO checkout(String token) {
        return orderService.createOrder(token);
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
}
