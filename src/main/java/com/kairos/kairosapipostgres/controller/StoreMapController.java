package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.response.StoreMapResponse;
import com.kairos.kairosapipostgres.service.StoreMapService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store-map")
public class StoreMapController {
    private final StoreMapService service;

    public StoreMapController(StoreMapService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<StoreMapResponse> find() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.findForCurrentCompany());
    }
}
