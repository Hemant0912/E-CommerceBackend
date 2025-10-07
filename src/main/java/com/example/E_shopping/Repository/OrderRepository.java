package com.example.E_shopping.Repository;
import com.example.E_shopping.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.E_shopping.Entity.User;


import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}

