package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.exception.EmployeeAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.mapper.EmployeeMapper;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;
    private final UserRepository userRepository;

    public EmployeeService (EmployeeRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EmployeeResponse save(EmployeeRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (repository.existsByUserId(request.userId())) {
            throw new EmployeeAlreadyExistsException("Employee already exists for this user");
        }

        Employee employee = EmployeeMapper.toEntity(request, user);
        return EmployeeMapper.toResponse(repository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return repository.findAll().stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeResponse> findById(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        return Optional.of(EmployeeMapper.toResponse(employee));
    }

    @Transactional
    public Optional<EmployeeResponse> update(Long id, EmployeeUpdateRequest request) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        employee.setPosition(request.position());
        return Optional.of(EmployeeMapper.toResponse(repository.save(employee)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        repository.deleteById(id);
        return true;
    }
}
