package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.AisleRequest;
import com.kairos.kairosapipostgres.dto.request.AisleUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.AisleResponse;
import com.kairos.kairosapipostgres.exception.AisleAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.AisleNotFoundException;
import com.kairos.kairosapipostgres.exception.SectorNotFoundException;
import com.kairos.kairosapipostgres.mapper.AisleMapper;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AisleService {
    private final AisleRepository repository;
    private final SectorRepository sectorRepository;

    public AisleService(AisleRepository repository, SectorRepository sectorRepository) {
        this.repository = repository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public AisleResponse save(AisleRequest request) {
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        if (repository.existsBySectorIdAndPosition(sector.getId(), request.position())) {
            throw new AisleAlreadyExistsException("Aisle already exists for this sector and position");
        }
        return AisleMapper.toResponse(repository.save(AisleMapper.toEntity(request, sector)));
    }

    @Transactional(readOnly = true)
    public List<AisleResponse> findAll() {
        return repository.findAll().stream().map(AisleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<AisleResponse> findById(Long id) {
        Aisle aisle = repository.findById(id)
                .orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        return Optional.of(AisleMapper.toResponse(aisle));
    }

    @Transactional(readOnly = true)
    public List<AisleResponse> findBySectorId(Long sectorId) {
        if (!sectorRepository.existsById(sectorId)) {
            throw new SectorNotFoundException("Sector not found");
        }
        return repository.findBySectorIdOrderByPosition(sectorId).stream()
                .map(AisleMapper::toResponse).toList();
    }

    @Transactional
    public Optional<AisleResponse> update(Long id, AisleUpdateRequest request) {
        Aisle aisle = repository.findById(id)
                .orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        Sector sector = request.sectorId() == null ? aisle.getSector()
                : sectorRepository.findById(request.sectorId())
                    .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        Integer position = request.position() == null ? aisle.getPosition() : request.position();
        repository.findBySectorIdAndPosition(sector.getId(), position)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AisleAlreadyExistsException("Aisle already exists for this sector and position");
                });
        aisle.setSector(sector);
        aisle.setPosition(position);
        return Optional.of(AisleMapper.toResponse(repository.save(aisle)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        repository.deleteById(id);
        return true;
    }
}
