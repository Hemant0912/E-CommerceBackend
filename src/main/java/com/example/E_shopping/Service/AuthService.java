package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Entity.Merchant;

public interface AuthService {

    UserResponseDTO register(UserRequestDTO dto);

    AuthResponseDTO loginUser(AuthRequestDTO dto);

    void logoutUser(String token);

    User getUserByEmail(String email);

    User getUserByMobile(String mobile);

    String generateAndSaveToken(User user);

    boolean validatePassword(String rawPassword, String encodedPassword);

    MerchantResponseDTO registerMerchant(MerchantRequestDTO dto);

    AuthResponseDTO loginMerchant(AuthRequestDTO dto);

    void logoutMerchant(String token);

    Merchant getMerchantByEmail(String email);

    Merchant getMerchantByMobile(String mobile);

    String generateAndSaveToken(Merchant merchant);

    String generateToken(User user);

    String generateToken(Merchant merchant);

    String encodePassword(String rawPassword);

}
