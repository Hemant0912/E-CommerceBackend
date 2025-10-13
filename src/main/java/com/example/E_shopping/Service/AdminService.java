package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;

import java.util.List;

public interface AdminService {
    AuthResponseDTO createAdmin(String token, UserRequestDTO dto);
    List<AdminViewUserDTO> getAllUsers(String token);
    List<AdminViewMerchantDTO> getAllMerchants(String token);
    List<ProductResponseDTO> getAllProducts(String token);
    Object getAllOrders(String token);
}


