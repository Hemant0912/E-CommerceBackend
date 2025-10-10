package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.OrderRepository;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.util.JwtUtil;
import com.example.E_shopping.util.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // new admin by super admin
    @Override
    public AuthResponseDTO createAdmin(String token, UserResponseDTO dto) {
        String email = jwtUtil.getEmailFromToken(token);
        User superAdmin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (!"SUPER_ADMIN".equals(superAdmin.getRole())) {
            throw new RuntimeException("Only SUPER_ADMIN can create new admins");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User admin = new User();
        admin.setName(dto.getName());
        admin.setEmail(dto.getEmail());
        admin.setMobile(dto.getMobile());
        admin.setPassword(encoder.encode("Admin@123")); // default password
        admin.setRole("ADMIN");
        admin.setPermissions(RolePermission.getPermissions("ADMIN"));
        admin.setAddress(dto.getAddress());
        admin.setLatestToken("");

        userRepository.save(admin);

        AuthResponseDTO res = new AuthResponseDTO();
        res.setId(admin.getId());
        res.setName(admin.getName());
        res.setEmail(admin.getEmail());
        res.setMobile(admin.getMobile());
        res.setRole(admin.getRole());
        return res;
    }

    @Override
    public List<User> getAllUsers(String token) {
        validateAdminOrSuperAdmin(token);
        return userRepository.findAll().stream()
                .filter(u -> "USER".equals(u.getRole()))
                .toList();
    }

    @Override
    public List<User> getAllMerchants(String token) {
        validateAdminOrSuperAdmin(token);
        return userRepository.findAll().stream()
                .filter(u -> "MERCHANT".equals(u.getRole()))
                .toList();
    }

    @Override
    public List<ProductResponseDTO> getAllProducts(String token) {
        validateAdminOrSuperAdmin(token);

        return productRepository.findAll().stream()
                .map(product -> {
                    ProductResponseDTO dto = new ProductResponseDTO();
                    dto.setId(product.getId());
                    dto.setName(product.getName());
                    dto.setDescription(product.getDescription());
                    dto.setType(product.getType());
                    dto.setCategory(product.getCategory());
                    dto.setColor(product.getColor());
                    dto.setPrice(product.getPrice());
                    dto.setQuantity(product.getQuantity());

                    if (product.getMerchant() != null) {
                        dto.setMerchantId(product.getMerchant().getId());
                        dto.setMerchantName(product.getMerchant().getName());
                    }
                    return dto;
                }).toList();
    }


    @Override
    public Object getAllOrders(String token) {
        validateAdminOrSuperAdmin(token);
        return orderRepository.findAll().stream().map(order -> {
            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setOrderId(order.getOrderId());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalPrice());
            dto.setPaymentId(order.getPaymentId());
            dto.setOrderDate(order.getOrderDate());
            dto.setPaidAt(order.getPaidAt());
            dto.setPreparingAt(order.getPreparingAt());
            dto.setOutForDeliveryAt(order.getOutForDeliveryAt());
            dto.setDeliveredAt(order.getDeliveredAt());
            dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
            dto.setRefundAt(order.getRefundAt());
            dto.setRefundStatus(order.getRefundStatus());

            List<OrderItemDTO> items = order.getItems().stream().map(item -> {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setPrice(item.getProduct().getPrice());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setTotalPrice(item.getProduct().getPrice() * item.getQuantity());
                return itemDTO;
            }).toList();

            dto.setItems(items);
            return dto;
        }).toList();
    }


    private void validateAdminOrSuperAdmin(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (!("ADMIN".equals(admin.getRole()) || "SUPER_ADMIN".equals(admin.getRole()))) {
            throw new RuntimeException("Access denied");
        }
    }
}
