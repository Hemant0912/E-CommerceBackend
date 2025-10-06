package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.CartItemRequestDTO;
import com.example.E_shopping.Dto.CartItemResponseDTO;
import com.example.E_shopping.Service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Add to cart
    @PostMapping("/add")
    public ResponseEntity<CartItemResponseDTO> addToCart(
            @RequestHeader("X-Auth") String token,
            @RequestBody CartItemRequestDTO dto) {
        return ResponseEntity.ok(cartService.addToCart(token, dto));
    }

    // Update cart item quantity
    @PutMapping("/update/{productId}")
    public ResponseEntity<CartItemResponseDTO> updateCartItem(
            @RequestHeader("X-Auth") String token,
            @PathVariable Long productId,
            @RequestBody CartItemRequestDTO dto) {
        return ResponseEntity.ok(cartService.updateCartItem(token, productId, dto));
    }

    // Remove cart item
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<String> removeCartItem(
            @RequestHeader("X-Auth") String token,
            @PathVariable Long productId) {
        cartService.removeCartItem(token, productId);
        return ResponseEntity.ok("Product removed from cart");
    }

    // Get all cart items
    @GetMapping
    public ResponseEntity<List<CartItemResponseDTO>> getCartItems(
            @RequestHeader("X-Auth") String token) {
        return ResponseEntity.ok(cartService.getCartItems(token));
    }
}
