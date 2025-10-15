package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.*;
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

    // add cart
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartItemResponseDTO>> addToCart(
            @RequestHeader("X-Auth") String token,
            @RequestBody CartItemRequestDTO dto) {
        CartItemResponseDTO item = cartService.addToCart(token, dto);
        return ResponseEntity.ok(new ApiResponse<>("success", "Product added to cart", item, null));
    }

    // update cart quantity
    @PutMapping("/update/{productId}")
    public ResponseEntity<CartItemResponseDTO> updateCartItem(
            @RequestHeader("X-Auth") String token,
            @PathVariable Long productId,
            @RequestBody CartItemRequestDTO dto) {
        return ResponseEntity.ok(cartService.updateCartItem(token, productId, dto));
    }

    // remove cart item
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<String>> removeCartItem(
            @RequestHeader("X-Auth") String token,
            @PathVariable Long productId) {
        cartService.removeCartItem(token, productId);
        return ResponseEntity.ok(new ApiResponse<>("success", "Product removed from cart", "removed", null));
    }
    // to see all cart item with price
    @GetMapping("/all")
    public ResponseEntity<CartResponseWithTotalDTO> getCart(@RequestHeader("X-Auth") String token) {
        return ResponseEntity.ok(cartService.getCartWithTotal(token));
    }
    // for payment
    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponseDTO>> checkout(@RequestHeader("X-Auth") String token) {
        List<OrderResponseDTO> responseDTOs = cartService.checkout(token);
        return ResponseEntity.ok(responseDTOs);
    }





}
