package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.PromotionRequest;
import com.kairos.kairosapipostgres.dto.request.PromotionUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.PromotionResponse;
import com.kairos.kairosapipostgres.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {
    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<PromotionResponse> register(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<PromotionResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<PromotionResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/shelf")
    public ResponseEntity<List<PromotionResponse>> findByShelfId(@RequestParam Long shelfId) {
        return ResponseEntity.ok(service.findByShelfId(shelfId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<PromotionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PromotionUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
