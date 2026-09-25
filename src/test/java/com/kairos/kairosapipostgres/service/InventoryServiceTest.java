package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.repository.InventoryRepository;
import com.kairos.kairosapipostgres.repository.ProductInventoryRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock private InventoryRepository inventoryRepository;
    @Mock private SectorRepository sectorRepository;
    @Mock private ProductInventoryRepository productInventoryRepository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(inventoryRepository, sectorRepository, productInventoryRepository);
    }

    @Test
    void shouldReturnTrueWhenInventoryHasEnoughProduct() {
        when(productInventoryRepository.existsByInventoryIdAndProductIdAndProductQuantityGreaterThanEqual(
                1L, 2L, 3)).thenReturn(true);

        assertThat(service.isProductAvailable(1L, 2L, 3)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenInventoryHasInsufficientOrMissingProduct() {
        assertThat(service.isProductAvailable(1L, 2L, 3)).isFalse();
        verify(productInventoryRepository).existsByInventoryIdAndProductIdAndProductQuantityGreaterThanEqual(
                1L, 2L, 3);
    }

    @Test
    void shouldRejectNonPositiveQuantityWithoutQuery() {
        assertThat(service.isProductAvailable(1L, 2L, 0)).isFalse();
        assertThat(service.isProductAvailable(1L, 2L, -1)).isFalse();
        assertThat(service.isProductAvailable(1L, 2L, null)).isFalse();
        verify(productInventoryRepository, never())
                .existsByInventoryIdAndProductIdAndProductQuantityGreaterThanEqual(any(), any(), any());
    }
}
