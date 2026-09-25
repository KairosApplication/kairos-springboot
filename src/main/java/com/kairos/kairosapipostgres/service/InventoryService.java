package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.InventoryRequest;
import com.kairos.kairosapipostgres.dto.request.InventoryUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.InventoryResponse;
import com.kairos.kairosapipostgres.exception.InventoryNotFoundException;
import com.kairos.kairosapipostgres.exception.SectorNotFoundException;
import com.kairos.kairosapipostgres.mapper.InventoryMapper;
import com.kairos.kairosapipostgres.model.Inventory;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.repository.InventoryRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {
    private final InventoryRepository repository;
    private final SectorRepository sectorRepository;

    public InventoryService(InventoryRepository repository, SectorRepository sectorRepository) {
        this.repository = repository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public InventoryResponse save(InventoryRequest request) {
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        return InventoryMapper.toResponse(repository.save(InventoryMapper.toEntity(sector)));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findAll() {
        return repository.findAll().stream().map(InventoryMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryResponse> findById(Long id) {
        return Optional.of(InventoryMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"))));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findBySectorId(Long sectorId) {
        if (!sectorRepository.existsById(sectorId)) {
            throw new SectorNotFoundException("Sector not found");
        }
        return repository.findBySectorId(sectorId).stream().map(InventoryMapper::toResponse).toList();
    }

    @Transactional
    public Optional<InventoryResponse> update(Long id, InventoryUpdateRequest request) {
        Inventory inventory = repository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        if (request.sectorId() != null) {
            inventory.setSector(sectorRepository.findById(request.sectorId())
                    .orElseThrow(() -> new SectorNotFoundException("Sector not found")));
        }
        return Optional.of(InventoryMapper.toResponse(repository.save(inventory)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        repository.deleteById(id);
        return true;
    }
}
