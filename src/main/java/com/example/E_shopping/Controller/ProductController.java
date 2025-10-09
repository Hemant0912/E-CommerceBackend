package com.example.E_shopping.Controller;
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

    // ✅ Merchant add product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_ADD_PRODUCT')")
    @PostMapping("/add")
    public ResponseEntity<ProductResponseDTO> addProduct(@RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.addProduct(dto));
    }

    // ✅ Merchant update product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_UPDATE_PRODUCT')")
    @PutMapping("/update/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id,
                                                            @RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    // ✅ Merchant delete product
    @PreAuthorize("hasRole('MERCHANT') or hasAuthority('PERMISSION_DELETE_PRODUCT')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    // ✅ User can view product by ID
    @PreAuthorize("hasRole('USER') or hasAuthority('PERMISSION_VIEW_PRODUCT')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ✅ User can view all products
    @PreAuthorize("hasRole('USER') or hasAuthority('PERMISSION_VIEW_PRODUCT')")
    @GetMapping("/all")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // ✅ Merchant sees only his own products
    @PreAuthorize("hasRole('MERCHANT')")
    @GetMapping("/merchant")
    public ResponseEntity<List<ProductResponseDTO>> listMerchantProducts(@RequestHeader("X-auth") String token) {
        return ResponseEntity.ok(productService.listProducts(token));
    }

    // ✅ Single unified search endpoint
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
