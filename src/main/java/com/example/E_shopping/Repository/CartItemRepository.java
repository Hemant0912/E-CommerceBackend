package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Entity.User; // ✅ Use your entity here
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    List<CartItem> findByProduct(Product product);
}
