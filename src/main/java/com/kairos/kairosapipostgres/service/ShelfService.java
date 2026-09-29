package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.ShelfRequest;
import com.kairos.kairosapipostgres.dto.request.ShelfUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ShelfResponse;
import com.kairos.kairosapipostgres.exception.AisleNotFoundException;
import com.kairos.kairosapipostgres.exception.ShelfNotFoundException;
import com.kairos.kairosapipostgres.mapper.ShelfMapper;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfService {
    private final ShelfRepository repository;
    private final AisleRepository aisleRepository;

    public ShelfService(ShelfRepository repository, AisleRepository aisleRepository) {
        this.repository = repository;
        this.aisleRepository = aisleRepository;
    }

    @Transactional
    public ShelfResponse save(ShelfRequest request) {
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        return ShelfMapper.toResponse(repository.save(ShelfMapper.toEntity(request, aisle)));
    }

    @Transactional(readOnly = true)
    public List<ShelfResponse> findAll() {
        return repository.findAll().stream().map(ShelfMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ShelfResponse> findById(Long id) {
        Shelf shelf = repository.findById(id)
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        return Optional.of(ShelfMapper.toResponse(shelf));
    }

    @Transactional(readOnly = true)
    public List<ShelfResponse> findByAisleId(Long aisleId) {
        if (!aisleRepository.existsById(aisleId)) {
            throw new AisleNotFoundException("Aisle not found");
        }
        return repository.findByAisleId(aisleId).stream().map(ShelfMapper::toResponse).toList();
    }

    @Transactional
    public Optional<ShelfResponse> update(Long id, ShelfUpdateRequest request) {
        Shelf shelf = repository.findById(id)
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        if (request.aisleId() != null) {
            shelf.setAisle(aisleRepository.findById(request.aisleId())
                    .orElseThrow(() -> new AisleNotFoundException("Aisle not found")));
        }
        if (request.maximumCapacity() != null) {
            shelf.setMaximumCapacity(request.maximumCapacity());
        }
        return Optional.of(ShelfMapper.toResponse(repository.save(shelf)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        repository.deleteById(id);
        return true;
    }
}
