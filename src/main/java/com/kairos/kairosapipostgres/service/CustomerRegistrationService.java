package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
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

    public CustomerRegistrationService(UserService users, UserRepository userRepository,
                                       CustomerRepository customers) {
        this.users = users;
        this.userRepository = userRepository;
        this.customers = customers;
    }

    @Transactional
    public UserResponse register(UserRequest request) {
        UserResponse created = users.save(request);
        customers.save(new Customer(null, userRepository.getReferenceById(created.id())));
        return created;
    }
}
