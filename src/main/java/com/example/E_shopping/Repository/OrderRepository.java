package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.Order;
import com.example.E_shopping.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    Optional<Order> findByOrderId(String orderId);

    // Pagination + Sorting
    Page<Order> findByUser(User user, Pageable pageable);
}
