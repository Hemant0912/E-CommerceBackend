package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.ProductRequestDTO;
import com.example.E_shopping.Dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {
    ProductResponseDTO addProduct(ProductRequestDTO dto);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto);
    void deleteProduct(Long id);
    ProductResponseDTO getProductById(Long id);
    Page<ProductResponseDTO> getAllProducts(Pageable pageable);
    List<ProductResponseDTO> listProducts(String token);

    // ✅ Search methods
    List<ProductResponseDTO> searchByCategory(String category);
    List<ProductResponseDTO> searchByType(String type);
    List<ProductResponseDTO> searchByKeyword(String keyword);
}
