package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.IndividualOrderRequestDTO;
import com.example.E_shopping.Dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(String token);
    OrderResponseDTO payOrder(String token, String orderId);
    void cancelOrder(String token, String orderId);
    void returnOrder(String token, String orderId);
    List<OrderResponseDTO> getUserOrders(String token);
    OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto);
}
