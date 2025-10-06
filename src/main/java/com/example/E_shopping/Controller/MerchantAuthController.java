package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant")
public class MerchantAuthController {

    @Autowired
    private MerchantService merchantService;

    // Signup
    @PostMapping("/signup")
    public ResponseEntity<MerchantResponseDTO> signup(@Valid @RequestBody MerchantRequestDTO dto) {
        MerchantResponseDTO response = merchantService.registerMerchant(dto);
        return ResponseEntity.ok(response);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<MerchantResponseDTO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        Merchant merchant = merchantService.getMerchantByEmailOrMobile(dto.getEmailOrMobile());

        if (!merchantService.validatePassword(dto.getPassword(), merchant.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = merchantService.generateAndSaveMerchantToken(merchant);

        MerchantResponseDTO response = new MerchantResponseDTO();
        response.setId(String.valueOf(merchant.getId()));
        response.setRole("MERCHANT");
        response.setName(merchant.getName());
        response.setEmail(merchant.getEmail());
        response.setMobile(merchant.getMobile());

        return ResponseEntity.ok()
                .header("X-Auth", token)
                .body(response);
    }

    // Logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("X-Auth") String token) {
        merchantService.logoutMerchant(token);
        return ResponseEntity.ok("Logged out successfully");
    }
}
