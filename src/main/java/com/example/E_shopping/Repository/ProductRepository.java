package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Optional: helper methods for more combinations if needed
    List<Product> findByCategoryIgnoreCaseAndTypeIgnoreCase(String category, String type);
    List<Product> findByCategoryIgnoreCaseAndTypeIgnoreCaseAndColorIgnoreCase(String category, String type, String color);
}
