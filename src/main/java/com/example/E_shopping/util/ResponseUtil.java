package com.example.E_shopping.util;

import com.example.E_shopping.Dto.ApiResponse;

public class ResponseUtil {

    public static <T> ApiResponse<T> success(T result, String message) {
        return new ApiResponse<>("success", message, result, null);
    }

    public static <T> ApiResponse<T> failure(String message, Object error) {
        return new ApiResponse<>("failure", message, null, error);
    }
}
