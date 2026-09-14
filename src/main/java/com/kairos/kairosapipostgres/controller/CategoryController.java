package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.CategoryRequest;
import com.kairos.kairosapipostgres.dto.request.CategoryUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.CategoryResponse;
import com.kairos.kairosapipostgres.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/registration")
    public ResponseEntity<CategoryResponse> register(
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<CategoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
