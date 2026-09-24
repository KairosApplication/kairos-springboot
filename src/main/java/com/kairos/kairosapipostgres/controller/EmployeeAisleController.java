package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.EmployeeAisleRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeAisleUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeAisleResponse;
import com.kairos.kairosapipostgres.service.EmployeeAisleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/employee-aisles")
public class EmployeeAisleController {
    private final EmployeeAisleService service;

    public EmployeeAisleController(EmployeeAisleService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<EmployeeAisleResponse> register(@Valid @RequestBody EmployeeAisleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EmployeeAisleResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<EmployeeAisleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/employee")
    public ResponseEntity<List<EmployeeAisleResponse>> findByEmployeeId(@RequestParam Long employeeId) {
        return ResponseEntity.ok(service.findByEmployeeId(employeeId));
    }

    @GetMapping("/find/aisle")
    public ResponseEntity<List<EmployeeAisleResponse>> findByAisleId(@RequestParam Long aisleId) {
        return ResponseEntity.ok(service.findByAisleId(aisleId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<EmployeeAisleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeAisleUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
