package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.AlertRequest;
import com.kairos.kairosapipostgres.dto.request.AlertUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.AlertResponse;
import com.kairos.kairosapipostgres.exception.*;
import com.kairos.kairosapipostgres.mapper.AlertMapper;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService {
    private final AlertRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ShelfRepository shelfRepository;
    private final ProductRepository productRepository;

    public AlertService(AlertRepository repository, EmployeeRepository employeeRepository,
                        ShelfRepository shelfRepository, ProductRepository productRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.shelfRepository = shelfRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public AlertResponse save(AlertRequest request) {
        Shelf shelf = shelf(request.shelfId());
        Employee employee = request.employeeId() == null ? null : employee(request.employeeId());
        Product product = request.productId() == null ? null : product(request.productId());
        return AlertMapper.toResponse(repository.save(AlertMapper.toEntity(request, employee, shelf, product)));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> findAll() {
        return repository.findAll().stream().map(AlertMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<AlertResponse> findById(Long id) {
        return Optional.of(AlertMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found"))));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> findByShelfId(Long shelfId) {
        shelf(shelfId);
        return repository.findByShelfId(shelfId).stream().map(AlertMapper::toResponse).toList();
    }

    @Transactional
    public Optional<AlertResponse> update(Long id, AlertUpdateRequest request) {
        Alert alert = repository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found"));
        if (request.stockoutDate() != null) {
            alert.setStockoutDate(request.stockoutDate());
        }
        if (request.employeeId() != null) {
            alert.setEmployee(employee(request.employeeId()));
        }
        if (request.shelfId() != null) {
            alert.setShelf(shelf(request.shelfId()));
        }
        if (request.productId() != null) {
            alert.setProduct(product(request.productId()));
        }
        if (request.description() != null) {
            alert.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            alert.setStatus(request.status());
        }
        if (request.resolutionDate() != null) {
            alert.setResolutionDate(request.resolutionDate());
        }
        return Optional.of(AlertMapper.toResponse(repository.save(alert)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new AlertNotFoundException("Alert not found"));
        repository.deleteById(id);
        return true;
    }

    private Employee employee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
    }

    private Shelf shelf(Long id) {
        return shelfRepository.findById(id)
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
    }

    private Product product(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }
}
