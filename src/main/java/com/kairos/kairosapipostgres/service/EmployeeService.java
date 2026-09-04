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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public EmployeeResponse save(EmployeeRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (employeeRepository.existsByUserId(request.userId())) {
            throw new EmployeeAlreadyExistsException("Employee already exists for this user");
        }

        Employee employee = EmployeeMapper.toEntity(request, user);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return EmployeeMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = findEntityById(id);
        employee.setPosition(request.position());
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteById(Long id) {
        Employee employee = findEntityById(id);
        employeeRepository.delete(employee);
    }

    private Employee findEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
    }
}
