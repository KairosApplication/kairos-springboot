package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.SectorRequest;
import com.kairos.kairosapipostgres.dto.request.SectorUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.SectorResponse;
import com.kairos.kairosapipostgres.exception.SectorAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.SectorNotFoundException;
import com.kairos.kairosapipostgres.mapper.SectorMapper;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class SectorService {

    private final SectorRepository repository;

    public SectorService(SectorRepository sectorRepository) {
        this.repository = sectorRepository;
    }

    @Transactional
    public SectorResponse save(SectorRequest sector) {
        Sector sectorEntity = SectorMapper.toEntity(sector);
        if (repository.existsByName(sectorEntity.getName())) {
            throw new SectorAlreadyExistsException ("Sector already exists");
        }

        return SectorMapper.toResponse(repository.save(sectorEntity));
    }  

    @Transactional(readOnly = true)
    public List<SectorResponse> findAll() {
        return repository.findAll().stream()
                .map(SectorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SectorResponse> findById(Long id) {
        Sector sector = repository.findById(id)
                .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        return Optional.of(SectorMapper.toResponse(sector));
    }

    @Transactional(readOnly = true)
    public Optional<SectorResponse> findByName(String name) {
        Sector sectorEntity = repository.findByName(name.trim())
                .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        return Optional.of(SectorMapper.toResponse(sectorEntity));
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> findByType(String type) {
        return repository.findByType(type.trim())
                .stream()
                .map(SectorMapper::toResponse)
                .toList();
    }

    @Transactional
    public Optional<SectorResponse> update(Long id, SectorUpdateRequest request) {
        Sector sector = repository.findById(id)
                .orElseThrow(() -> new SectorNotFoundException ("Sector not found"));

        String name = request.name() != null ? request.name().trim() : sector.getName();
        String type = request.type() != null ? request.type().trim() : sector.getType();

        repository.findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new SectorAlreadyExistsException("Sector already exists");
                });

        sector.setName(name);
        sector.setType(type);
        return Optional.of(SectorMapper.toResponse(repository.save(sector)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new SectorNotFoundException("Sector not found"));
        repository.deleteById(id);
        
        return true;        
    }




}
