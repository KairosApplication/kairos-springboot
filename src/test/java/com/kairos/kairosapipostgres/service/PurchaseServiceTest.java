package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.exception.PurchaseNotFoundException;
import com.kairos.kairosapipostgres.model.Purchase;
import com.kairos.kairosapipostgres.repository.PurchaseProductRepository;
import com.kairos.kairosapipostgres.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    @Mock private PurchaseRepository repository;
    @Mock private PurchaseProductRepository purchaseProductRepository;

    private PurchaseService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(repository, purchaseProductRepository);
    }

    @Test
    void shouldCalculateTotalFromRecordedItemAmounts() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Purchase()));
        when(purchaseProductRepository.calculateTotal(1L)).thenReturn(new BigDecimal("29.70"));

        assertThat(service.calculateTotal(1L).total()).isEqualByComparingTo("29.70");
    }

    @Test
    void shouldReturnZeroForPurchaseWithoutItems() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Purchase()));

        assertThat(service.calculateTotal(1L).total()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRejectMissingPurchase() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateTotal(1L))
                .isInstanceOf(PurchaseNotFoundException.class)
                .hasMessage("Purchase not found");
        verify(purchaseProductRepository, never()).calculateTotal(1L);
    }
}
