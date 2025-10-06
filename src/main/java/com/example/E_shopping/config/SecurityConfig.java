package com.example.E_shopping.config;

import com.example.E_shopping.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // Enable @PreAuthorize on methods
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public APIs (signup/login allowed for both user/merchant)
                        .requestMatchers("/public/**",
                                "/merchant/signup", "/merchant/login",
                                "/user/signup", "/user/login").permitAll()
                        // ✅ Everything else needs authentication
                        .anyRequest().authenticated()
                );

        // ✅ Add JWT filter to check X-auth before processing requests
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
