package com.example.E_shopping.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<CartItem> items;

    private double totalPrice;

    private String status; // PENDING, PAID, PREPARING, OUT_FOR_DELIVERY, DELIVERED, ...

    private String paymentId;

    // Estimated delivery date/time (for UI)
    private LocalDateTime estimatedDeliveryDate;

    // timestamps for stages
    private LocalDateTime paidAt;
    private LocalDateTime preparingAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;

    private LocalDateTime orderDate;

}
