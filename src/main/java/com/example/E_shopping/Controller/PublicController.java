package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.UserRequestDTO;
import com.example.E_shopping.Dto.UserResponseDTO;
import com.example.E_shopping.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private AuthService authService;

    // Register user
    @PostMapping("/sign-up")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = authService.register(dto);
        return ResponseEntity.ok(response);
    }
}
