package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.ProductShelfRequest;
import com.kairos.kairosapipostgres.dto.request.ProductShelfUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ProductShelfResponse;
import com.kairos.kairosapipostgres.service.ProductShelfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/product-shelves")
public class ProductShelfController {
    private final ProductShelfService service;

    public ProductShelfController(ProductShelfService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<ProductShelfResponse> register(@Valid @RequestBody ProductShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProductShelfResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<ProductShelfResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/product")
    public ResponseEntity<List<ProductShelfResponse>> findByProductId(@RequestParam Long productId) {
        return ResponseEntity.ok(service.findByProductId(productId));
    }

    @GetMapping("/find/shelf")
    public ResponseEntity<List<ProductShelfResponse>> findByShelfId(@RequestParam Long shelfId) {
        return ResponseEntity.ok(service.findByShelfId(shelfId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<ProductShelfResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductShelfUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
