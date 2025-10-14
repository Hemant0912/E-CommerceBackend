package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.IndividualOrderRequestDTO;
import com.example.E_shopping.Dto.OrderResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    List<OrderResponseDTO> createOrder(String token);
    OrderResponseDTO payOrder(String token, String orderId);
    void cancelOrder(String token, String orderId);
    void returnOrder(String token, String orderId);
    List<OrderResponseDTO> getUserOrders(String token);
    Page<OrderResponseDTO> getUserOrdersPaginated(String token, int page, int size, String sortBy);
    OrderResponseDTO orderSingleItem(String token, IndividualOrderRequestDTO dto);
}
