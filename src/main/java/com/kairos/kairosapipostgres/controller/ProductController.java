package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.ProductRequest;
import com.kairos.kairosapipostgres.dto.request.ProductUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ProductResponse;
import com.kairos.kairosapipostgres.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController (ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/registration")
    public ResponseEntity<ProductResponse> register(
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse product = productService.save(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/list/{brand}")
    public ResponseEntity<List<ProductResponse>> listBrand(
            @PathVariable String brand
    ) {
        return ResponseEntity.ok(productService.findByBrand (brand));
    }

    @GetMapping("/find/price")
    public ResponseEntity<List<ProductResponse>> findByMaxPrice(
            @RequestParam @DecimalMin("0.0") BigDecimal maxPrice
    ) {
        return ResponseEntity.ok(productService.findByMaxPrice(maxPrice));
    }

    @GetMapping("find/{id}")
    public ResponseEntity<Optional<ProductResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/find/name")
    public ResponseEntity<Optional<ProductResponse>> findByName(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(productService.findByName(name));
    }

    @PatchMapping("update/{id}")
    public ResponseEntity<Optional<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(
                productService.update (id, request)
        );
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteById (id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
    
}
