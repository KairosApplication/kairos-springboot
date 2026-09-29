package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.RestockingRequest;
import com.kairos.kairosapipostgres.dto.request.RestockingUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.RestockingResponse;
import com.kairos.kairosapipostgres.exception.*;
import com.kairos.kairosapipostgres.mapper.RestockingMapper;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RestockingService {
    private final RestockingRepository repository;
    private final InventoryRepository inventoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ShelfRepository shelfRepository;
    private final ProductRepository productRepository;

    public RestockingService(RestockingRepository repository, InventoryRepository inventoryRepository,
                             EmployeeRepository employeeRepository, ShelfRepository shelfRepository,
                             ProductRepository productRepository) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository;
        this.employeeRepository = employeeRepository;
        this.shelfRepository = shelfRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public RestockingResponse save(RestockingRequest request) {
        Inventory inventory = inventory(request.inventoryId());
        Employee employee = employee(request.employeeId());
        Shelf shelf = shelf(request.shelfId());
        Product product = product(request.productId());
        return RestockingMapper.toResponse(repository.save(
                RestockingMapper.toEntity(request, inventory, employee, shelf, product)));
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findAll() {
        return repository.findAll().stream().map(RestockingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RestockingResponse> findById(Long id) {
        return Optional.of(RestockingMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new RestockingNotFoundException("Restocking not found"))));
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findByShelfId(Long shelfId) {
        shelf(shelfId);
        return repository.findByShelfId(shelfId).stream().map(RestockingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findByInventoryId(Long inventoryId) {
        inventory(inventoryId);
        return repository.findByInventoryId(inventoryId).stream().map(RestockingMapper::toResponse).toList();
    }

    @Transactional
    public Optional<RestockingResponse> update(Long id, RestockingUpdateRequest request) {
        Restocking restocking = repository.findById(id)
                .orElseThrow(() -> new RestockingNotFoundException("Restocking not found"));
        if (request.inventoryId() != null) {
            restocking.setInventory(inventory(request.inventoryId()));
        }
        if (request.employeeId() != null) {
            restocking.setEmployee(employee(request.employeeId()));
        }
        if (request.shelfId() != null) {
            restocking.setShelf(shelf(request.shelfId()));
        }
        if (request.productId() != null) {
            restocking.setProduct(product(request.productId()));
        }
        if (request.restockedQuantity() != null) {
            restocking.setRestockedQuantity(request.restockedQuantity());
        }
        if (request.damagedQuantity() != null) {
            restocking.setDamagedQuantity(request.damagedQuantity());
        }
        return Optional.of(RestockingMapper.toResponse(repository.save(restocking)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new RestockingNotFoundException("Restocking not found"));
        repository.deleteById(id);
        return true;
    }

    private Inventory inventory(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
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
