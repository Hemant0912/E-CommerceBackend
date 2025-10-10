package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class AuthController {

    @Autowired
    private AuthService authService;

    // user login
    @PostMapping("/user/login")
    public ResponseEntity<UserResponseDTO> loginUser(@Valid @RequestBody AuthRequestDTO dto) {
        AuthResponseDTO auth = authService.loginUser(dto);
        UserResponseDTO response = new UserResponseDTO();
        response.setId(String.valueOf(auth.getId()));
        response.setName(auth.getName());
        response.setEmail(auth.getEmail());
        response.setMobile(auth.getMobile());
        response.setRole(auth.getRole());
        response.setAddress(auth.getAddress());

        return ResponseEntity.ok()
                .header("X-Auth", auth.getToken()) // jwt in x-auth in header
                .body(response);
    }

    @PostMapping("/user/logout")
    public ResponseEntity<String> logoutUser(@RequestHeader("X-Auth") String token) {
        authService.logoutUser(token);
        return ResponseEntity.ok("User logged out successfully");
    }
}
