package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMerchant(Merchant merchant);

    boolean existsByMerchantAndNameIgnoreCase(Merchant merchant, String name);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByTypeIgnoreCase(String type);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByColorIgnoreCase(String color);

    List<Product> findByCategoryIgnoreCaseAndColorIgnoreCase(String category, String color);

    List<Product> findByTypeIgnoreCaseAndColorIgnoreCase(String type, String color);

    List<Product> findByCategoryIgnoreCaseAndTypeIgnoreCase(String category, String type);

    List<Product> findByCategoryIgnoreCaseAndTypeIgnoreCaseAndColorIgnoreCase(String category, String type, String color);

   // for multiple search
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
            "(:type IS NULL OR LOWER(p.type) = LOWER(:type)) AND " +
            "(:color IS NULL OR LOWER(p.color) = LOWER(:color)) AND " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
    Page<Product> searchProducts(@Param("category") String category,
                                 @Param("type") String type,
                                 @Param("color") String color,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}
