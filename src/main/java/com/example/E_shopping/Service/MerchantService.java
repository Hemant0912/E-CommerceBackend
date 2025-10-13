package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.MerchantLoginDTO;
import com.example.E_shopping.Dto.MerchantRequestDTO;
import com.example.E_shopping.Dto.MerchantResponseDTO;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class MerchantService {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final long TOKEN_EXPIRATION_HOURS = 6;


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


    public String loginMerchant(MerchantLoginDTO dto) {
        Merchant merchant = getMerchantByEmailOrMobile(dto.getEmailOrMobile());

        if (!validatePassword(dto.getPassword(), merchant.getPassword()))
            throw new IllegalArgumentException("Invalid credentials");
        // new token
        return generateAndSaveTokenInRedis(merchant);
    }

    public MerchantResponseDTO getMerchantDetails(String email) {
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        return new MerchantResponseDTO(
                String.valueOf(merchant.getId()),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getMobile(),
                "MERCHANT"
        );
    }

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

    public String generateAndSaveTokenInRedis(Merchant merchant) {
        String token = jwtUtil.generateToken(merchant.getEmail(), "MERCHANT");
        String redisKey = "MERCHANT_TOKEN:" + merchant.getEmail();

        // Remove old token if exists
        redisTemplate.delete(redisKey);

        // Save new token in Redis with expiry
        redisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);

        return token;
    }

    public void logoutMerchant(String token) {
        if (!jwtUtil.validateToken(token))
            throw new IllegalArgumentException("Invalid token");

        String email = jwtUtil.getEmailFromToken(token);
        String redisKey = "MERCHANT_TOKEN:" + email;

        redisTemplate.delete(redisKey);
    }
}
