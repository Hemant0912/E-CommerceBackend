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
    public ResponseEntity<CartItemResponseDTO> addToCart(
            @RequestHeader("X-Auth") String token,
            @RequestBody CartItemRequestDTO dto) {
        return ResponseEntity.ok(cartService.addToCart(token, dto));
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
    public ResponseEntity<String> removeCartItem(
            @RequestHeader("X-Auth") String token,
            @PathVariable Long productId) {
        cartService.removeCartItem(token, productId);
        return ResponseEntity.ok("Product removed from cart");
    }
    // to see all cart item with price
    @GetMapping("/all")
    public ResponseEntity<CartResponseWithTotalDTO> getCart(@RequestHeader("X-Auth") String token) {
        return ResponseEntity.ok(cartService.getCartWithTotal(token));
    }
    // for payment
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(@RequestHeader("X-Auth") String token) {
        OrderResponseDTO responseDTO = cartService.checkout(token); // server calculates total
        return ResponseEntity.ok(responseDTO);
    }




}
