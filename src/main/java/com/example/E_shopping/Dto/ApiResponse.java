package com.example.E_shopping.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;  // "success" or "failure"
    private String message; // Any message you want to send
    private T result;       // Generic type for actual response data
    private Object error;   // Error details if any
}
