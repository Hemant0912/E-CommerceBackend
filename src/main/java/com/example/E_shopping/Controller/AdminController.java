package com.example.E_shopping.Controller;
import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.ProductRepository;
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
    private UserRepository userRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> loginAdmin(@RequestBody AuthRequestDTO dto) {
        AuthResponseDTO auth = authService.loginUser(dto);

        if (!"ADMIN".equals(auth.getRole()) && !"SUPER_ADMIN".equals(auth.getRole())) {
            return ResponseEntity.status(403).body(null);
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
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutAdmin(@RequestHeader("X-Auth") String token) {
        authService.logoutUser(token);
        return ResponseEntity.ok("logged out");
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(@RequestHeader("X-Auth") String token,
                                              @RequestBody UserRequestDTO dto) {
        String email = jwtUtil.getEmailFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        if (!"SUPER_ADMIN".equals(role)) {
            return ResponseEntity.status(403).body("You cannot create admins");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setPassword(authService.encodePassword(dto.getPassword()));
        user.setRole("ADMIN");
        user.setPermissions(null);
        user.setAddress(dto.getAddress());
        user.setLatestToken("");

        userRepository.save(user);
        return ResponseEntity.ok("Admin created ");
    }

    // view all users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader("X-Auth") String token) {
        String role = jwtUtil.getRoleFromToken(token);
        if (!role.equals("ADMIN") && !role.equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userRepository.findAll());
    }

    // View All merchants
    @GetMapping("/merchants")
    public ResponseEntity<List<Merchant>> getAllMerchants(@RequestHeader("X-Auth") String token) {
        String role = jwtUtil.getRoleFromToken(token);
        if (!role.equals("ADMIN") && !role.equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(merchantRepository.findAll());
    }

    // view products
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(@RequestHeader("X-Auth") String token) {
        String role = jwtUtil.getRoleFromToken(token);
        if (!role.equals("ADMIN") && !role.equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        List<ProductResponseDTO> products = adminService.getAllProducts(token);
        return ResponseEntity.ok(products);
    }


    // view orders
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(@RequestHeader("X-Auth") String token) {
        String role = jwtUtil.getRoleFromToken(token);
        if (!role.equals("ADMIN") && !role.equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        Object orders = adminService.getAllOrders(token);
        return ResponseEntity.ok(orders);
    }

}
