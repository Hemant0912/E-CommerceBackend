package com.example.E_shopping.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminViewUserDTO {
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private String address;
    private List<String> permissions;
}
