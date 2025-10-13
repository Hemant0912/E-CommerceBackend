package com.example.E_shopping.filter;
import com.example.E_shopping.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader("X-Auth");

        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmailFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            Set<GrantedAuthority> authorities = new HashSet<>();
            String redisKey;

            switch (role) {
                case "USER" -> redisKey = "USER_TOKEN:" + email;
                case "MERCHANT" -> redisKey = "MERCHANT_TOKEN:" + email;
                case "ADMIN", "SUPER_ADMIN" -> redisKey = "USER_TOKEN:" + email; // optional separate key
                default -> redisKey = null;
            }

            boolean valid = redisKey != null && token.equals(redisTemplate.opsForValue().get(redisKey));

            if (!valid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token expired or invalid due to new login\"}");
                return;
            }
            // for roles perm
            switch (role) {
                case "USER" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_PRODUCT"));
                }
                case "MERCHANT" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_ADD_PRODUCT"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_UPDATE_PRODUCT"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_DELETE_PRODUCT"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_PRODUCT"));
                }
                case "ADMIN" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_USERS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_MERCHANTS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_PRODUCTS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_ORDERS"));
                }
                case "SUPER_ADMIN" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_USERS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_MERCHANTS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_PRODUCTS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_ORDERS"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_CREATE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_ALL"));
                }
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}

