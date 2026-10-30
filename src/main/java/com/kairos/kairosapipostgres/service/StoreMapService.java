package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.response.StoreMapResponse;
import com.kairos.kairosapipostgres.mapper.*;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreMapService {
    private final RestockingAccess access;
    private final SectorRepository sectors;
    private final AisleRepository aisles;
    private final ShelfRepository shelves;
    private final ProductRepository products;
    private final ProductShelfRepository productShelves;

    public StoreMapService(RestockingAccess access, SectorRepository sectors, AisleRepository aisles,
                           ShelfRepository shelves, ProductRepository products, ProductShelfRepository productShelves) {
        this.access = access;
        this.sectors = sectors;
        this.aisles = aisles;
        this.shelves = shelves;
        this.products = products;
        this.productShelves = productShelves;
    }

    @Transactional(readOnly = true)
    public StoreMapResponse findForCurrentCompany() {
        long companyId = access.actor().getCompany().getId();
        return new StoreMapResponse(
                sectors.findByCompanyId(companyId).stream().map(SectorMapper::toResponse).toList(),
                aisles.findBySectorCompanyId(companyId).stream().map(AisleMapper::toResponse).toList(),
                shelves.findByAisleSectorCompanyId(companyId).stream().map(ShelfMapper::toResponse).toList(),
                products.findByCompanyId(companyId).stream().map(ProductMapper::toResponse).toList(),
                productShelves.findByShelfAisleSectorCompanyIdAndProductCompanyId(companyId, companyId)
                        .stream().map(ProductShelfMapper::toResponse).toList());
    }
}
