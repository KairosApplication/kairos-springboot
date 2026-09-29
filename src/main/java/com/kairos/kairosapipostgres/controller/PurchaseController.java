package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.response.PurchaseTotalResponse;
import com.kairos.kairosapipostgres.service.PurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {
    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping("/{id}/total")
    public ResponseEntity<PurchaseTotalResponse> calculateTotal(@PathVariable Long id) {
        return ResponseEntity.ok(service.calculateTotal(id));
    }
}
