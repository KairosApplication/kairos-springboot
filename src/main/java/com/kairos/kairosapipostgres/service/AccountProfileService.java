package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.response.AccountProfileResponse;
import com.kairos.kairosapipostgres.model.Company;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileService {
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final CustomerRepository customers;

    public AccountProfileService(UserRepository users, EmployeeRepository employees, CustomerRepository customers) {
        this.users = users;
        this.employees = employees;
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public AccountProfileResponse current(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated account required");
        }
        var user = users.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Account not available"));
        boolean employeeRole = auth.getAuthorities().stream().anyMatch(role ->
                "ROLE_EMPLOYEE".equals(role.getAuthority()) || "ROLE_MANAGER".equals(role.getAuthority()));
        boolean customerRole = auth.getAuthorities().stream().anyMatch(role -> "ROLE_CUSTOMER".equals(role.getAuthority()));
        Long employeeId = null;
        Long customerId = null;
        Company company = null;
        String position = null;
        if (employeeRole) {
            var employee = employees.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("Employee account not available"));
            employeeId = employee.getId();
            company = employee.getCompany();
            position = employee.getPosition() == null ? null : employee.getPosition().name();
        } else if (customerRole) {
            var customer = customers.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("Customer account not available"));
            customerId = customer.getId();
            company = customer.getCompany();
        } else {
            throw new AccessDeniedException("Account role not available");
        }
        return new AccountProfileResponse(user.getId(), user.getEmail(), user.getName(), user.getLastName(),
                user.getBirthDate(), user.getCpf(), employeeId, customerId,
                company == null ? null : company.getId(), company == null ? null : company.getName(), position);
    }
}
