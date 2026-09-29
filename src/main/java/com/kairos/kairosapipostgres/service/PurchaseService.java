package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.response.PurchaseTotalResponse;
import com.kairos.kairosapipostgres.exception.PurchaseNotFoundException;
import com.kairos.kairosapipostgres.repository.PurchaseProductRepository;
import com.kairos.kairosapipostgres.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PurchaseService {
    private final PurchaseRepository repository;
    private final PurchaseProductRepository purchaseProductRepository;

    public PurchaseService(PurchaseRepository repository, PurchaseProductRepository purchaseProductRepository) {
        this.repository = repository;
        this.purchaseProductRepository = purchaseProductRepository;
    }

    @Transactional(readOnly = true)
    public PurchaseTotalResponse calculateTotal(Long purchaseId) {
        repository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException("Purchase not found"));
        BigDecimal total = purchaseProductRepository.calculateTotal(purchaseId);
        return new PurchaseTotalResponse(purchaseId, total == null ? BigDecimal.ZERO : total);
    }
}
