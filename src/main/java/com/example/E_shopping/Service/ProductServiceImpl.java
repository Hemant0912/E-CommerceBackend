package com.example.E_shopping.Service;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Repository.CartItemRepository;
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

    @Autowired
    private CartItemRepository cartItemRepository;

    // add product merchant
    @Override
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        // ✅ Check if product already exists for this merchant (case-insensitive)
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

        // for quantity handling
        if (dto.getQuantity() != null) {
            product.setQuantity(dto.getQuantity());
        } else if (dto.getStock() != null) {
            product.setQuantity(dto.getStock());
        } else {
            product.setQuantity(0); // default if nothing provided
        }

        product.setMerchant(merchant);

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }



    // merchant can view the product
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

        // Update only non-null fields
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getType() != null) product.setType(dto.getType());
        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getColor() != null) product.setColor(dto.getColor());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());

        if (dto.getQuantity() != null) {
            product.setQuantity(dto.getQuantity());
        } else if (dto.getStock() != null) { // if quantity is null but stock is provided
            product.setQuantity(dto.getStock());
        }

        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }



    // delete product
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Delete only cart items related to this product
        List<CartItem> relatedCartItems = cartItemRepository.findByProduct(product);
        if (!relatedCartItems.isEmpty()) {
            cartItemRepository.deleteAll(relatedCartItems);
        }

        // Now delete the product
        productRepository.delete(product);
    }

    // get product by id
    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDTO(product);
    }

    // get all product
    @Override
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToDTO);
    }

    // search by category
    // Inside ProductServiceImpl.java
    @Override
    public List<ProductResponseDTO> searchByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ New method for color filtering
    public List<ProductResponseDTO> searchByColor(String color) {
        return productRepository.findByColorIgnoreCase(color)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ✅ New method for combined filter
    public List<ProductResponseDTO> searchByCategoryAndColor(String category, String color) {
        return productRepository.findByCategoryIgnoreCaseAndColorIgnoreCase(category, color)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<ProductResponseDTO> searchByTypeAndColor(String type, String color) {
        return productRepository.findByTypeIgnoreCaseAndColorIgnoreCase(type, color)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // search by its type
    @Override
    public List<ProductResponseDTO> searchByType(String type) {
        return productRepository.findByTypeIgnoreCase(type)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // search by name
    @Override
    public List<ProductResponseDTO> searchByKeyword(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
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
}
