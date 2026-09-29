package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.ShelfEmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.ShelfEmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ShelfEmployeeResponse;
import com.kairos.kairosapipostgres.exception.ShelfNotFoundException;
import com.kairos.kairosapipostgres.exception.ShelfEmployeeAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.ShelfEmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.mapper.ShelfEmployeeMapper;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.ShelfEmployee;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import com.kairos.kairosapipostgres.repository.ShelfEmployeeRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfEmployeeService {
    private final ShelfEmployeeRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ShelfRepository shelfRepository;

    public ShelfEmployeeService(ShelfEmployeeRepository repository, EmployeeRepository employeeRepository,
                                ShelfRepository shelfRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.shelfRepository = shelfRepository;
    }

    @Transactional
    public ShelfEmployeeResponse save(ShelfEmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        Shelf shelf = shelfRepository.findById(request.shelfId())
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        checkDuplicate(employee.getId(), shelf.getId(), null);
        return ShelfEmployeeMapper.toResponse(repository.save(ShelfEmployeeMapper.toEntity(employee, shelf)));
    }

    @Transactional(readOnly = true)
    public List<ShelfEmployeeResponse> findAll() {
        return repository.findAll().stream().map(ShelfEmployeeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ShelfEmployeeResponse> findById(Long id) {
        return Optional.of(ShelfEmployeeMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new ShelfEmployeeNotFoundException("Shelf employee assignment not found"))));
    }

    @Transactional(readOnly = true)
    public List<ShelfEmployeeResponse> findByEmployeeId(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found");
        }
        return repository.findByEmployeeId(employeeId).stream().map(ShelfEmployeeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ShelfEmployeeResponse> findByShelfId(Long shelfId) {
        if (!shelfRepository.existsById(shelfId)) {
            throw new ShelfNotFoundException("Shelf not found");
        }
        return repository.findByShelfId(shelfId).stream().map(ShelfEmployeeMapper::toResponse).toList();
    }

    @Transactional
    public Optional<ShelfEmployeeResponse> update(Long id, ShelfEmployeeUpdateRequest request) {
        ShelfEmployee assignment = repository.findById(id)
                .orElseThrow(() -> new ShelfEmployeeNotFoundException("Shelf employee assignment not found"));
        Employee employee = request.employeeId() == null ? assignment.getEmployee()
                : employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        Shelf shelf = request.shelfId() == null ? assignment.getShelf()
                : shelfRepository.findById(request.shelfId())
                    .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        checkDuplicate(employee.getId(), shelf.getId(), id);
        assignment.setEmployee(employee);
        assignment.setShelf(shelf);
        return Optional.of(ShelfEmployeeMapper.toResponse(repository.save(assignment)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ShelfEmployeeNotFoundException("Shelf employee assignment not found"));
        repository.deleteById(id);
        return true;
    }

    private void checkDuplicate(Long employeeId, Long shelfId, Long currentId) {
        repository.findByEmployeeIdAndShelfId(employeeId, shelfId)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ShelfEmployeeAlreadyExistsException("Employee already assigned to this shelf");
                });
    }
}
