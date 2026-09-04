package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService service;


    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody EmployeeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.save(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
