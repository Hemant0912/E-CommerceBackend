package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByEmail(String email);
    Optional<Merchant> findByMobile(String mobile);
}
