package com.example.E_shopping.Service;

import org.springframework.security.core.context.SecurityContextHolder;
import com.example.E_shopping.Dto.ProductRequestDTO;
import com.example.E_shopping.Dto.ProductResponseDTO;
import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Repository.MerchantRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ✅ Add Product (Merchant only)
    @Override
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setType(dto.getType());
        product.setCategory(dto.getCategory()); // ✅ FIXED - category now saved
        product.setColor(dto.getColor());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        product.setMerchant(merchant);

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    // ✅ Merchant can view their listed products
    @Override
    public List<ProductResponseDTO> listProducts(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        return productRepository.findByMerchant(merchant)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ Update Product
    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setType(dto.getType());
        product.setCategory(dto.getCategory());
        product.setColor(dto.getColor());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());

        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    // ✅ Delete Product
    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ✅ Get product by ID
    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDTO(product);
    }

    // ✅ Get all products (Paginated)
    @Override
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToDTO);
    }

    // ✅ Search by Category
    @Override
    public List<ProductResponseDTO> searchByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ Search by Type
    @Override
    public List<ProductResponseDTO> searchByType(String type) {
        return productRepository.findByTypeIgnoreCase(type)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ Search by Keyword (name)
    @Override
    public List<ProductResponseDTO> searchByKeyword(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ Mapper Method
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
}
