package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.CustomerDamageReportRequest;
import com.kairos.kairosapipostgres.service.CustomerDamageReportService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/damage-reports")
public class CustomerDamageReportController {
    private final CustomerDamageReportService service;

    public CustomerDamageReportController(CustomerDamageReportService service) {
        this.service = service;
    }

    private <T> ResponseEntity<T> ok(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }

    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities(Authentication auth) {
        return ok(service.capabilities(auth));
    }

    @GetMapping("/options")
    public ResponseEntity<?> options(Authentication auth) {
        return ok(service.options(auth));
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        return ok(service.list(auth));
    }

    @PostMapping
    public ResponseEntity<?> report(Authentication auth, @Valid @RequestBody CustomerDamageReportRequest request) {
        return ok(service.report(auth, request));
    }
}
