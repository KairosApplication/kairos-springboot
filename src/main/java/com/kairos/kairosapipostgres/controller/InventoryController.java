package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.InventoryRequest;
import com.kairos.kairosapipostgres.dto.request.InventoryUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.InventoryResponse;
import com.kairos.kairosapipostgres.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<InventoryResponse> register(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<InventoryResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<InventoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/sector")
    public ResponseEntity<List<InventoryResponse>> findBySectorId(@RequestParam Long sectorId) {
        return ResponseEntity.ok(service.findBySectorId(sectorId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<InventoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
