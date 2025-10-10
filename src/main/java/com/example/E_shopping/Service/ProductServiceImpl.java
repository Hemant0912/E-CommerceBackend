package com.example.E_shopping.Service;
import com.example.E_shopping.Dto.ProductRequestDTO;
import com.example.E_shopping.Dto.ProductResponseDTO;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Override
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        if (productRepository.existsByMerchantAndNameIgnoreCase(merchant, dto.getName())) {
            throw new IllegalArgumentException("Product with the same name already exists for this merchant");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setType(dto.getType());
        product.setCategory(dto.getCategory());
        product.setColor(dto.getColor());
        product.setPrice(dto.getPrice());

        if (dto.getQuantity() != null) product.setQuantity(dto.getQuantity());
        else if (dto.getStock() != null) product.setQuantity(dto.getStock());
        else product.setQuantity(0);

        product.setMerchant(merchant);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Override
    public List<ProductResponseDTO> listProducts(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        return productRepository.findByMerchant(merchant)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getType() != null) product.setType(dto.getType());
        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getColor() != null) product.setColor(dto.getColor());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getQuantity() != null) product.setQuantity(dto.getQuantity());
        else if (dto.getStock() != null) product.setQuantity(dto.getStock());

        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        List<CartItem> relatedCartItems = cartItemRepository.findByProduct(product);
        if (!relatedCartItems.isEmpty()) cartItemRepository.deleteAll(relatedCartItems);

        productRepository.delete(product);
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDTO(product);
    }

    @Override
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToDTO);
    }

    private ProductResponseDTO mapToDTO(Product product) {
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
    }
    @Override
    public List<ProductResponseDTO> searchProducts(String category, String type, String color, String keyword) {
        List<Product> products = productRepository.findAll();

        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }
        if (type != null && !type.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        if (color != null && !color.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getColor() != null && p.getColor().equalsIgnoreCase(color))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }
        return products.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

}
