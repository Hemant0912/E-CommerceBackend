package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.Merchant;
import com.example.E_shopping.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMerchant(Merchant merchant);

    // ✅ New search filters
    List<Product> findByCategoryIgnoreCase(String category);
    List<Product> findByTypeIgnoreCase(String type);
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
