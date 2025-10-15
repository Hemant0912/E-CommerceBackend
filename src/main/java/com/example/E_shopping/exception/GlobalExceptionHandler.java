package com.example.E_shopping.exception;

import com.example.E_shopping.Dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        ApiResponse<Object> response = new ApiResponse<>();
        response.setStatus("failure");
        response.setMessage(errorMessage);
        response.setResult(null);
        response.setError(null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle illegal arguments (like "Email already exists")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.setStatus("failure");
        response.setMessage(ex.getMessage());
        response.setResult(null);
        response.setError(null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.setStatus("failure");
        response.setMessage("Something went wrong");
        response.setResult(null);
        response.setError(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
