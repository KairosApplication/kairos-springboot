package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import com.kairos.kairosapipostgres.utils.CpfValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class ManagerBootstrap implements ApplicationRunner {
    private final UserService users;
    private final UserRepository userRepository;
    private final EmployeeRepository employees;
    private final String email;
    private final String password;
    private final String cpf;
    private final String name;
    private final String lastName;
    private final String birthDate;
    private final String zipCode;

    public ManagerBootstrap(UserService users, UserRepository userRepository,
                            EmployeeRepository employees,
                            @Value("${BOOTSTRAP_MANAGER_EMAIL:}") String email,
                            @Value("${BOOTSTRAP_MANAGER_PASSWORD:}") String password,
                            @Value("${BOOTSTRAP_MANAGER_CPF:}") String cpf,
                            @Value("${BOOTSTRAP_MANAGER_NAME:}") String name,
                            @Value("${BOOTSTRAP_MANAGER_LAST_NAME:}") String lastName,
                            @Value("${BOOTSTRAP_MANAGER_BIRTH_DATE:}") String birthDate,
                            @Value("${BOOTSTRAP_MANAGER_ZIP_CODE:}") String zipCode) {
        this.users = users;
        this.userRepository = userRepository;
        this.employees = employees;
        this.email = email;
        this.password = password;
        this.cpf = cpf;
        this.name = name;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.zipCode = zipCode;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (email.isBlank()) {
            return;
        }
        if (employees.existsByPosition(Position.MANAGER)) {
            return;
        }
        if (password.isBlank() || !CpfValidator.isValid(cpf) || name.isBlank()
                || lastName.isBlank() || birthDate.isBlank() || zipCode.isBlank()) {
            throw new IllegalStateException("Bootstrap manager configuration is incomplete");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Bootstrap manager email is already registered");
        }
        UserRequest request = new UserRequest(name, lastName,
                LocalDate.parse(birthDate), password, zipCode,
                Plan.STANDART, email, cpf);
        UserResponse created = users.save(request);
        employees.save(new Employee(null, Position.MANAGER,
                userRepository.getReferenceById(created.id())));
    }
}
