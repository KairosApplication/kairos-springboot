package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.AisleRequest;
import com.kairos.kairosapipostgres.dto.request.AisleUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.AisleResponse;
import com.kairos.kairosapipostgres.service.AisleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/aisles")
public class AisleController {
    private final AisleService service;

    public AisleController(AisleService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<AisleResponse> register(@Valid @RequestBody AisleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<AisleResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<AisleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/sector")
    public ResponseEntity<List<AisleResponse>> findBySectorId(@RequestParam Long sectorId) {
        return ResponseEntity.ok(service.findBySectorId(sectorId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<AisleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AisleUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
