package com.example.E_shopping.Service;

import com.example.E_shopping.Dto.CartItemRequestDTO;
import com.example.E_shopping.Dto.CartItemResponseDTO;
import com.example.E_shopping.Entity.CartItem;
import com.example.E_shopping.Entity.Product;
import com.example.E_shopping.Entity.User;
import com.example.E_shopping.Repository.CartItemRepository;
import com.example.E_shopping.Repository.ProductRepository;
import com.example.E_shopping.Repository.UserRepository;
import com.example.E_shopping.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


