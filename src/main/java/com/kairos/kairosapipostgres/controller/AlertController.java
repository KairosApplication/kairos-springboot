package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.AlertRequest;
import com.kairos.kairosapipostgres.dto.request.AlertUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.AlertResponse;
import com.kairos.kairosapipostgres.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<AlertResponse> register(@Valid @RequestBody AlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<AlertResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<AlertResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/shelf")
    public ResponseEntity<List<AlertResponse>> findByShelfId(@RequestParam Long shelfId) {
        return ResponseEntity.ok(service.findByShelfId(shelfId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<AlertResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AlertUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
