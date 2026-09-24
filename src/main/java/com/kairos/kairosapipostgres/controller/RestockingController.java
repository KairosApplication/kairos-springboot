package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.RestockingRequest;
import com.kairos.kairosapipostgres.dto.request.RestockingUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.RestockingResponse;
import com.kairos.kairosapipostgres.service.RestockingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/restockings")
public class RestockingController {
    private final RestockingService service;

    public RestockingController(RestockingService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<RestockingResponse> register(@Valid @RequestBody RestockingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<RestockingResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<RestockingResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/shelf")
    public ResponseEntity<List<RestockingResponse>> findByShelfId(@RequestParam Long shelfId) {
        return ResponseEntity.ok(service.findByShelfId(shelfId));
    }

    @GetMapping("/find/inventory")
    public ResponseEntity<List<RestockingResponse>> findByInventoryId(@RequestParam Long inventoryId) {
        return ResponseEntity.ok(service.findByInventoryId(inventoryId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<RestockingResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RestockingUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
