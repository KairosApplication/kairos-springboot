package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.ShelfEmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.ShelfEmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ShelfEmployeeResponse;
import com.kairos.kairosapipostgres.service.ShelfEmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/shelf-employees")
public class ShelfEmployeeController {
    private final ShelfEmployeeService service;

    public ShelfEmployeeController(ShelfEmployeeService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<ShelfEmployeeResponse> register(@Valid @RequestBody ShelfEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ShelfEmployeeResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Optional<ShelfEmployeeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/find/employee")
    public ResponseEntity<List<ShelfEmployeeResponse>> findByEmployeeId(@RequestParam Long employeeId) {
        return ResponseEntity.ok(service.findByEmployeeId(employeeId));
    }

    @GetMapping("/find/shelf")
    public ResponseEntity<List<ShelfEmployeeResponse>> findByShelfId(@RequestParam Long shelfId) {
        return ResponseEntity.ok(service.findByShelfId(shelfId));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Optional<ShelfEmployeeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ShelfEmployeeUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
