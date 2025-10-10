package com.example.E_shopping.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @ManyToOne
    private User user;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<CartItem> items;

    private double totalPrice;
    private String status;
    private String paymentId;
    private LocalDateTime orderDate;
    private LocalDateTime paidAt;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime preparingAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;

    private LocalDateTime refundAt;
    private String refundStatus;
}
