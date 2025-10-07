package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // signup
    public MerchantResponseDTO registerMerchant(MerchantRequestDTO dto) {
        if (merchantRepository.findByEmail(dto.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already exists");
        if (merchantRepository.findByMobile(dto.getMobile()).isPresent())
            throw new IllegalArgumentException("Mobile already exists");

        Merchant merchant = new Merchant();
        merchant.setName(dto.getName());
        merchant.setEmail(dto.getEmail());
        merchant.setMobile(dto.getMobile());
        merchant.setPassword(passwordEncoder.encode(dto.getPassword()));

        Merchant saved = merchantRepository.save(merchant);

        return new MerchantResponseDTO(
                String.valueOf(saved.getId()),
                saved.getName(),
                saved.getEmail(),
                saved.getMobile(),
                "MERCHANT"
        );
    }

    //login
    public MerchantResponseDTO loginMerchant(MerchantLoginDTO dto) {
        Merchant merchant = getMerchantByEmailOrMobile(dto.getEmailOrMobile());
        if (!validatePassword(dto.getPassword(), merchant.getPassword()))
            throw new IllegalArgumentException("Invalid credentials");

        String token = generateAndSaveMerchantToken(merchant);

        return new MerchantResponseDTO(
                String.valueOf(merchant.getId()),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getMobile(),
                "MERCHANT"
        );
    }

    // Helper: get merchant by email or mobile
    public Merchant getMerchantByEmailOrMobile(String emailOrMobile) {
        if (emailOrMobile.matches("^[6-9]\\d{9}$")) {
            return merchantRepository.findByMobile(emailOrMobile)
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        } else {
            return merchantRepository.findByEmail(emailOrMobile)
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        }
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String generateAndSaveMerchantToken(Merchant merchant) {
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT");
        merchant.setLatestToken(token);
        merchantRepository.save(merchant);
        return token;
    }

    public void logoutMerchant(String token) {
        if (!jwtUtil.validateToken(token))
            throw new IllegalArgumentException("Invalid token");
        String email = jwtUtil.getEmailFromToken(token);
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setLatestToken(null);
        merchantRepository.save(merchant);
    }
}
