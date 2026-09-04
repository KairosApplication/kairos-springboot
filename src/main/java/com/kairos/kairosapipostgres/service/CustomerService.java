package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.exception.CustomerAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.CustomerNotFoundException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.mapper.CustomerMapper;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerResponse save(CustomerRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (customerRepository.existsByUserId(request.userId())) {
            throw new CustomerAlreadyExistsException("Customer already exists for this user");
        }

        Customer customer = CustomerMapper.toEntity(request, user);
        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public void deleteById(Long id) {
        Customer customer = findEntityById(id);
        customerRepository.delete(customer);
    }

    private Customer findEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    }
}
