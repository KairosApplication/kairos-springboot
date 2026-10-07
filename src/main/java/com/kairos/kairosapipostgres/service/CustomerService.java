package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.dto.response.RecommendedProductResponse;
import com.kairos.kairosapipostgres.exception.CustomerAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.CustomerNotFoundException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.mapper.CustomerMapper;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import com.kairos.kairosapipostgres.repository.CompanyRepository;
import com.kairos.kairosapipostgres.exception.CompanyNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    public CustomerService (CustomerRepository repository, UserRepository userRepository,
                            ProductRepository productRepository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CustomerResponse save(CustomerRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (repository.existsByUserId(request.userId())) {
            throw new CustomerAlreadyExistsException("Customer already exists for this user");
        }

        var company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
        Customer customer = CustomerMapper.toEntity(request, user, company);
        return CustomerMapper.toResponse(repository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return repository.findAll().stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findById(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        return Optional.of(CustomerMapper.toResponse(customer));
    }

    @Transactional(readOnly = true)
    public List<RecommendedProductResponse> recommendProducts(Long customerId, int limit,
                                                                Authentication authentication) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        Customer customer = repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!manager && !customer.getUser().getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("Cannot view another customer's recommendations");
        }
        return productRepository.recommendForCustomer(customerId, limit).stream()
                .map(product -> new RecommendedProductResponse(product.getProductId(), product.getProductName(),
                        product.getCategoryId(), product.getCategoryName()))
                .toList();
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        repository.deleteById(id);
        return true;
    }
}
