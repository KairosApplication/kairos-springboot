package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.ShelfRequest;
import com.kairos.kairosapipostgres.dto.request.ShelfUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ShelfResponse;
import com.kairos.kairosapipostgres.service.ShelfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/shelves")
public class ShelfController {
    private final ShelfService service;

    public ShelfController(ShelfService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<ShelfResponse> register(@Valid @RequestBody ShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ShelfResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<ShelfResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/aisle")
    public ResponseEntity<List<ShelfResponse>> findByAisleId(@RequestParam Long aisleId) {
        return ResponseEntity.ok(service.findByAisleId(aisleId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<ShelfResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ShelfUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
