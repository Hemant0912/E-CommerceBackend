package com.example.E_shopping.Repository;

import com.example.E_shopping.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Find user by mobile
    Optional<User> findByMobile(String mobile);
}
