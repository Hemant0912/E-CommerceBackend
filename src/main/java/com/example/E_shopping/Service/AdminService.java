package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.AuthResponseDTO;
import com.example.E_shopping.Dto.ProductResponseDTO;
import com.example.E_shopping.Dto.UserResponseDTO;
import com.example.E_shopping.Entity.User;

import java.util.List;

public interface AdminService {
    AuthResponseDTO createAdmin(String token, UserResponseDTO dto);
    List<User> getAllUsers(String token);
    List<User> getAllMerchants(String token);
    List <ProductResponseDTO> getAllProducts(String token);
    Object getAllOrders(String token);
}
