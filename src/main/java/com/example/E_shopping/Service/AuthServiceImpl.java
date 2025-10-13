package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.*;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.util.JwtUtil;
import com.example.E_shopping.util.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private MerchantRepository merchantRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final long TOKEN_EXPIRATION_SECONDS = 24 * 60 * 60; // 24 hours

  // register
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

    //  login
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

   // logout
    @Override
    public void logoutUser(String token) {
        if (token == null || !jwtUtil.validateToken(token))
            throw new IllegalArgumentException("Invalid token");

        String email = jwtUtil.getEmailFromToken(token);
        String redisKey = "USER_TOKEN:" + email; // matches single-token key
        redisTemplate.delete(redisKey); // remove the single token
    }

    // token
    @Override
    public String generateAndSaveToken(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String redisKey = "USER_TOKEN:" + user.getEmail(); // single-token key

        redisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    public boolean isUserTokenValid(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        String redisKey = "USER_TOKEN:" + email;
        Object latestToken = redisTemplate.opsForValue().get(redisKey);
        return latestToken != null && token.equals(latestToken) && jwtUtil.validateToken(token);
    }

    // -------------------- MERCHANT METHODS --------------------
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
        String redisKey = "MERCHANT_TOKEN:" + email;
        redisTemplate.delete(redisKey);
    }

    @Override
    public String generateAndSaveToken(Merchant merchant) {
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT");
        String redisKey = "MERCHANT_TOKEN:" + merchant.getEmail();

        redisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    // -------------------- UTILS --------------------
    @Override public boolean validatePassword(String raw, String encoded) { return passwordEncoder.matches(raw, encoded); }
    @Override public String encodePassword(String raw) { return passwordEncoder.encode(raw); }
    @Override public String generateToken(User u) { return jwtUtil.generateToken(u.getEmail(), u.getRole()); }
    @Override public String generateToken(Merchant m) { return jwtUtil.generateToken(m.getEmail(), "MERCHANT"); }
    @Override public User getUserByEmail(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    @Override public User getUserByMobile(String mobile) { return userRepository.findByMobile(mobile).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    @Override public Merchant getMerchantByEmail(String email) { return merchantRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Merchant not found")); }
    @Override public Merchant getMerchantByMobile(String mobile) { return merchantRepository.findByMobile(mobile).orElseThrow(() -> new IllegalArgumentException("Merchant not found")); }
}
