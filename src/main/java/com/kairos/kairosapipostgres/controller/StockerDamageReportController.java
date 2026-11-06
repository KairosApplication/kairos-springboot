package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.service.StockerDamageReportService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts/customer-reports")
public class StockerDamageReportController {
    private final StockerDamageReportService service;

    public StockerDamageReportController(StockerDamageReportService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(auth));
    }

    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<?> resolve(Authentication auth, @PathVariable long alertId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.resolve(auth, alertId));
    }
}
