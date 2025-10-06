package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.CartItemRequestDTO;
import com.example.E_shopping.Dto.CartItemResponseDTO;

import java.util.List;

public interface CartService {

    CartItemResponseDTO addToCart(String token, CartItemRequestDTO dto);

    CartItemResponseDTO updateCartItem(String token, Long productId, CartItemRequestDTO dto);

    void removeCartItem(String token, Long productId);

    List<CartItemResponseDTO> getCartItems(String token);
}
