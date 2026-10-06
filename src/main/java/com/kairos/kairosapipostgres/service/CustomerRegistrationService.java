package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.request.UserRegistrationRequest;
import com.kairos.kairosapipostgres.exception.CompanyNotFoundException;
import com.kairos.kairosapipostgres.repository.CompanyRepository;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {
    private final UserService users;
    private final UserRepository userRepository;
    private final CustomerRepository customers;
    private final CompanyRepository companies;

    public CustomerRegistrationService(UserService users, UserRepository userRepository,
                                       CustomerRepository customers, CompanyRepository companies) {
        this.users = users;
        this.userRepository = userRepository;
        this.customers = customers;
        this.companies = companies;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        var company = companies.findById(request.companyId())
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
        UserResponse created = users.save(new UserRequest(request.name(), request.lastName(),
                request.birthDate(), request.password(), request.email(), request.cpf()));
        customers.save(new Customer(null, userRepository.getReferenceById(created.id()), company));
        return created;
    }
}
