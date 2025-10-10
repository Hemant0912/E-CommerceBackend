package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.util.JwtUtil;
import com.example.E_shopping.util.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

   // for user registerr
    @Override
    public UserResponseDTO register(UserRequestDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already exists");
        if (userRepository.findByMobile(dto.getMobile()).isPresent())
            throw new IllegalArgumentException("Mobile already exists");

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setAddress(dto.getAddress());
        user.setLatestToken("");
        user.setPermissions(RolePermission.getPermissions("USER"));

        User saved = userRepository.save(user);

        return new UserResponseDTO(
                String.valueOf(saved.getId()),
                saved.getName(),
                saved.getEmail(),
                saved.getMobile(),
                saved.getRole(),
                saved.getAddress()
        );
    }

    @Override
    public AuthResponseDTO loginUser(AuthRequestDTO dto) {
        User user = dto.getEmailOrMobile().contains("@") ?
                userRepository.findByEmail(dto.getEmailOrMobile())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password")) :
                userRepository.findByMobile(dto.getEmailOrMobile())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid mobile or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
            throw new IllegalArgumentException("Invalid credentials");

        String token = generateAndSaveToken(user);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());
        response.setRole(user.getRole());
        response.setAddress(user.getAddress());
        response.setCartItemCount(0);
        response.setToken(token);

        return response;
    }

    @Override
    public void logoutUser(String token) {
        if (token == null || !jwtUtil.validateToken(token))
            throw new IllegalArgumentException("Invalid token");

        String email = jwtUtil.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!token.equals(user.getLatestToken()))
            throw new IllegalArgumentException("Token already invalidated");

        user.setLatestToken("");
        userRepository.save(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public User getUserByMobile(String mobile) {
        return userRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public String generateAndSaveToken(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        user.setLatestToken(token); // overwrite old token
        userRepository.save(user);
        return token;
    }

    @Override
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
        merchant.setLatestToken("");

        Merchant saved = merchantRepository.save(merchant);

        return new MerchantResponseDTO(
                String.valueOf(saved.getId()),
                saved.getName(),
                saved.getEmail(),
                saved.getMobile(),
                "MERCHANT"
        );
    }

    @Override
    public AuthResponseDTO loginMerchant(AuthRequestDTO dto) {
        Merchant merchant = dto.getEmailOrMobile().contains("@") ?
                merchantRepository.findByEmail(dto.getEmailOrMobile())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password")) :
                merchantRepository.findByMobile(dto.getEmailOrMobile())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid mobile or password"));

        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword()))
            throw new IllegalArgumentException("Invalid credentials");

        String token = generateAndSaveToken(merchant);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(merchant.getId());
        response.setName(merchant.getName());
        response.setEmail(merchant.getEmail());
        response.setMobile(merchant.getMobile());
        response.setRole("MERCHANT");
        response.setToken(token);

        return response;
    }

    @Override
    public void logoutMerchant(String token) {
        if (token == null || !jwtUtil.validateToken(token))
            throw new IllegalArgumentException("Invalid token");

        String email = jwtUtil.getEmailFromToken(token);
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        if (!token.equals(merchant.getLatestToken()))
            throw new IllegalArgumentException("Token already invalidated");

        merchant.setLatestToken("");
        merchantRepository.save(merchant);
    }

    @Override
    public Merchant getMerchantByEmail(String email) {
        return null;
    }

    @Override
    public Merchant getMerchantByMobile(String mobile) {
        return null;
    }

    @Override
    public String generateAndSaveToken(Merchant merchant) {
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT");
        merchant.setLatestToken(token);
        merchantRepository.save(merchant);
        return token;
    }
    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String generateToken(User user) {
        return jwtUtil.generateToken(user.getEmail(), user.getRole());
    }

    @Override
    public String generateToken(Merchant merchant) {
        return jwtUtil.generateToken(merchant.getEmail(), "MERCHANT");
    }
    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

}
