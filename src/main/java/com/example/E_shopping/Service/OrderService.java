package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.IndividualOrderRequestDTO;
import com.example.E_shopping.Dto.OrderResponseDTO;
import com.example.E_shopping.Entity.Order;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(String token);
    OrderResponseDTO payOrder(String token, Long orderId);
    void cancelOrder(String token, Long orderId);
    void returnOrder(String token, Long orderId);
    List<OrderResponseDTO> getUserOrders(String token);
    OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto);

}


