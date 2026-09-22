package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.InvalidEmployeePositionException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.mapper.EmployeeMapper;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Position;
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
    private final UserService userService;

    public EmployeeService(
            EmployeeRepository repository,
            UserRepository userRepository,
            UserService userService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public EmployeeResponse save(EmployeeRequest request) {
        validateRegistrablePosition(request.position());

        UserResponse createdUser = userService.save(request.user());

        User user = userRepository.findById(createdUser.id())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

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
        validateRegistrablePosition(request.position());
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        employee.setPosition(request.position());
        return Optional.of(EmployeeMapper.toResponse(repository.save(employee)));
    }

    private void validateRegistrablePosition(Position position) {
        if (position != Position.CASHIER && position != Position.STOCKER) {
            throw new InvalidEmployeePositionException("The employee position must be CASHIER or STOCKER");
        }
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        repository.deleteById(id);
        return true;
    }
}
