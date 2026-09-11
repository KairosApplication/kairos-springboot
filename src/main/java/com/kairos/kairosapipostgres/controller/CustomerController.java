package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController (CustomerService customerService) {
        this.customerService = customerService;
    }


    @PostMapping("/registration")
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody CustomerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(customerService.save(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("find/{id}")
    public ResponseEntity<Optional<CustomerResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.deleteById(id);
        return ResponseEntity
                .status (HttpStatus.NO_CONTENT)
                .build();
    }
}
