package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.EmployeeAisleRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeAisleUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeAisleResponse;
import com.kairos.kairosapipostgres.exception.AisleNotFoundException;
import com.kairos.kairosapipostgres.exception.EmployeeAisleAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.EmployeeAisleNotFoundException;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.mapper.EmployeeAisleMapper;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.EmployeeAisle;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.EmployeeAisleRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeAisleService {
    private final EmployeeAisleRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AisleRepository aisleRepository;

    public EmployeeAisleService(EmployeeAisleRepository repository, EmployeeRepository employeeRepository,
                                AisleRepository aisleRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.aisleRepository = aisleRepository;
    }

    @Transactional
    public EmployeeAisleResponse save(EmployeeAisleRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        checkDuplicate(employee.getId(), aisle.getId(), null);
        return EmployeeAisleMapper.toResponse(repository.save(EmployeeAisleMapper.toEntity(employee, aisle)));
    }

    @Transactional(readOnly = true)
    public List<EmployeeAisleResponse> findAll() {
        return repository.findAll().stream().map(EmployeeAisleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeAisleResponse> findById(Long id) {
        return Optional.of(EmployeeAisleMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new EmployeeAisleNotFoundException("Employee aisle assignment not found"))));
    }

    @Transactional(readOnly = true)
    public List<EmployeeAisleResponse> findByEmployeeId(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found");
        }
        return repository.findByEmployeeId(employeeId).stream().map(EmployeeAisleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeAisleResponse> findByAisleId(Long aisleId) {
        if (!aisleRepository.existsById(aisleId)) {
            throw new AisleNotFoundException("Aisle not found");
        }
        return repository.findByAisleId(aisleId).stream().map(EmployeeAisleMapper::toResponse).toList();
    }

    @Transactional
    public Optional<EmployeeAisleResponse> update(Long id, EmployeeAisleUpdateRequest request) {
        EmployeeAisle assignment = repository.findById(id)
                .orElseThrow(() -> new EmployeeAisleNotFoundException("Employee aisle assignment not found"));
        Employee employee = request.employeeId() == null ? assignment.getEmployee()
                : employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        Aisle aisle = request.aisleId() == null ? assignment.getAisle()
                : aisleRepository.findById(request.aisleId())
                    .orElseThrow(() -> new AisleNotFoundException("Aisle not found"));
        checkDuplicate(employee.getId(), aisle.getId(), id);
        assignment.setEmployee(employee);
        assignment.setAisle(aisle);
        return Optional.of(EmployeeAisleMapper.toResponse(repository.save(assignment)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new EmployeeAisleNotFoundException("Employee aisle assignment not found"));
        repository.deleteById(id);
        return true;
    }

    private void checkDuplicate(Long employeeId, Long aisleId, Long currentId) {
        repository.findByEmployeeIdAndAisleId(employeeId, aisleId)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new EmployeeAisleAlreadyExistsException("Employee already assigned to this aisle");
                });
    }
}
