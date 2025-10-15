package com.example.E_shopping.Controller;
import com.example.E_shopping.Dto.ApiResponse;
import com.example.E_shopping.Dto.ProductRequestDTO;
import com.example.E_shopping.Dto.ProductResponseDTO;
import com.example.E_shopping.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    //  add product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_ADD_PRODUCT')")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> addProduct(@RequestBody ProductRequestDTO dto) {
        ProductResponseDTO product = productService.addProduct(dto);
        return ResponseEntity.ok(new ApiResponse<>("success", "Product added successfully", product, null));
    }


    //  update product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_UPDATE_PRODUCT')")
    @PutMapping("/update/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id,
                                                            @RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    //  delete product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_DELETE_PRODUCT')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse<>("success", "Product deleted successfully", "Deleted", null));
    }

    //  user can view product by id
    @PreAuthorize("hasRole('USER') or hasAuthority('PERMISSION_VIEW_PRODUCT')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    //  user can view all product
    @PreAuthorize("hasRole('USER') or hasAuthority('PERMISSION_VIEW_PRODUCT')")
    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // merchant sees only his own product
    @PreAuthorize("hasRole('MERCHANT')")
    @GetMapping("/merchant")
    public ResponseEntity<List<ProductResponseDTO>> listMerchantProducts(@RequestHeader("X-auth") String token) {
        return ResponseEntity.ok(productService.listProducts(token));
    }

    //  single search
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERMISSION_VIEW_PRODUCT')")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String keyword
    ) {
        List<ProductResponseDTO> results = productService.searchProducts(category, type, color, keyword);
        return ResponseEntity.ok(results);
    }
}
