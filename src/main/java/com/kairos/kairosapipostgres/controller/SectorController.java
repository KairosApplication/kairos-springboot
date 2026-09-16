package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.SectorRequest;
import com.kairos.kairosapipostgres.dto.request.SectorUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.SectorResponse;
import com.kairos.kairosapipostgres.service.SectorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @PostMapping("/registration")
    public ResponseEntity<SectorResponse> register(@Valid @RequestBody SectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<SectorResponse>> list() {
        return ResponseEntity.ok(sectorService.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<SectorResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(sectorService.findById(id));
    }

    @GetMapping("/find/name")
    public ResponseEntity<Optional<SectorResponse>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(sectorService.findByName(name));
    }

    @GetMapping("/find/type")
    public ResponseEntity<List<SectorResponse>> findByType(@RequestParam String type) {
        return ResponseEntity.ok(sectorService.findByType(type));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<SectorResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SectorUpdateRequest request
    ) {
        return ResponseEntity.ok(sectorService.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectorService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
