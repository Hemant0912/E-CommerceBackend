package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Service.AdminService;
import com.example.E_shopping.Service.AuthService;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDTO>> loginAdmin(@RequestBody AuthRequestDTO dto) {
        AuthResponseDTO auth = authService.loginUser(dto);

        if (!"ADMIN".equals(auth.getRole()) && !"SUPER_ADMIN".equals(auth.getRole())) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>( "failure", "Unauthorized role", null, null));
        }

        UserResponseDTO response = new UserResponseDTO();
        response.setId(String.valueOf(auth.getId()));
        response.setName(auth.getName());
        response.setEmail(auth.getEmail());
        response.setMobile(auth.getMobile());
        response.setRole(auth.getRole());
        response.setAddress(auth.getAddress());

        return ResponseEntity.ok()
                .header("X-Auth", auth.getToken())
                .body(new ApiResponse<>( "success", "Login successful", response, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logoutAdmin(@RequestHeader("X-Auth") String token) {
        authService.logoutUser(token);
        return ResponseEntity.ok(new ApiResponse<>("success", "Logged out successfully", "logged out", null));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(@RequestHeader("X-Auth") String token,
                                              @RequestBody UserRequestDTO dto) {
        String role = jwtUtil.getRoleFromToken(token);

        if (!"SUPER_ADMIN".equals(role)) {
            return ResponseEntity.status(403).body("You cannot create admins");
        }

        adminService.createAdmin(token, dto);
        return ResponseEntity.ok("Admin created");
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminViewUserDTO>>> getAllUsers(@RequestHeader("X-Auth") String token) {
        List<AdminViewUserDTO> users = adminService.getAllUsers(token);
        return ResponseEntity.ok(new ApiResponse<>("success", "", users, null));
    }

    @GetMapping("/merchants")
    public ResponseEntity<List<AdminViewMerchantDTO>> getAllMerchants(@RequestHeader("X-Auth") String token) {
        List<AdminViewMerchantDTO> merchants = adminService.getAllMerchants(token);
        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts(@RequestHeader("X-Auth") String token) {
        List<ProductResponseDTO> products = adminService.getAllProducts(token);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(@RequestHeader("X-Auth") String token) {
        Object orders = adminService.getAllOrders(token);
        return ResponseEntity.ok(orders);
    }
}
