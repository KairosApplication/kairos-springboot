package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.RestockingRequest;
import com.kairos.kairosapipostgres.dto.request.RestockingUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.RestockingResponse;
import com.kairos.kairosapipostgres.dto.response.RestockingOptionsResponse;
import com.kairos.kairosapipostgres.mapper.EmployeeMapper;
import com.kairos.kairosapipostgres.mapper.InventoryMapper;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.exception.*;
import com.kairos.kairosapipostgres.mapper.RestockingMapper;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.Clock;

@Service
public class RestockingService {
    private final RestockingRepository repository;
    private final InventoryRepository inventoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ShelfRepository shelfRepository;
    private final ProductRepository productRepository;
    private final RestockingAccess access;
    private final RestockingHistoryOutbox history;
    private final Clock clock;
    private final ProductInventoryRepository productInventories;
    private final ProductShelfRepository productShelves;

    public RestockingService(RestockingRepository repository, InventoryRepository inventoryRepository,
                             EmployeeRepository employeeRepository, ShelfRepository shelfRepository,
                             ProductRepository productRepository, RestockingAccess access,
                             RestockingHistoryOutbox history, Clock clock,
                             ProductInventoryRepository productInventories, ProductShelfRepository productShelves) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository;
        this.employeeRepository = employeeRepository;
        this.shelfRepository = shelfRepository;
        this.productRepository = productRepository;
        this.access = access;
        this.history = history;
        this.clock = clock;
        this.productInventories = productInventories;
        this.productShelves = productShelves;
    }

    @Transactional(readOnly = true)
    public RestockingOptionsResponse options() {
        Employee actor = access.actor();
        Long companyId = actor.getCompany().getId();
        List<Employee> people = actor.getPosition() == Position.MANAGER
                ? employeeRepository.findByCompanyIdAndPositionIn(companyId, List.of(Position.MANAGER, Position.STOCKER))
                : List.of(actor);
        return new RestockingOptionsResponse(people.stream().map(EmployeeMapper::toResponse).toList(),
                inventoryRepository.findByCompanyIdAndSectorCompanyId(companyId, companyId).stream()
                        .map(InventoryMapper::toResponse).toList());
    }

    @Transactional
    public RestockingResponse save(RestockingRequest request) {
        access.target(request.employeeId());
        Inventory inventory = inventory(request.inventoryId());
        Employee employee = employee(request.employeeId());
        Shelf shelf = shelf(request.shelfId());
        Product product = product(request.productId());
        Restocking value = RestockingMapper.toEntity(request, inventory, employee, shelf, product);
        value.setRestockingDate(clock.instant().atZone(RestockingWeek.ZONE).toLocalDateTime());
        access.require(value);
        validateQuantities(value);
        moveStock(value);
        value = repository.save(value);
        history.enqueue(value, true);
        return RestockingMapper.toResponse(value);
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findAll() {
        Employee actor = access.actor();
        return repository.findByEmployeeCompanyId(actor.getCompany().getId()).stream()
                .filter(value -> access.visible(value, actor)).map(RestockingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RestockingResponse> findById(Long id) {
        Restocking value = repository.findById(id).orElseThrow(() -> new RestockingNotFoundException("Restocking not found"));
        access.require(value);
        return Optional.of(RestockingMapper.toResponse(value));
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findByShelfId(Long shelfId) {
        access.requireCompany(shelf(shelfId).getAisle().getSector().getCompany());
        Employee actor = access.actor();
        return repository.findByShelfId(shelfId).stream().filter(value -> access.visible(value, actor)).map(RestockingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RestockingResponse> findByInventoryId(Long inventoryId) {
        access.requireCompany(inventory(inventoryId).getCompany());
        Employee actor = access.actor();
        return repository.findByInventoryId(inventoryId).stream().filter(value -> access.visible(value, actor)).map(RestockingMapper::toResponse).toList();
    }

    @Transactional
    public Optional<RestockingResponse> update(Long id, RestockingUpdateRequest request) {
        Restocking restocking = repository.locked(id)
                .orElseThrow(() -> new RestockingNotFoundException("Restocking not found"));
        access.require(restocking);
        history.bypass(restocking);
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
        access.require(restocking);
        validateQuantities(restocking);
        history.bypass(restocking);
        return Optional.of(RestockingMapper.toResponse(repository.save(restocking)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        Restocking value = repository.locked(id).orElseThrow(() -> new RestockingNotFoundException("Restocking not found"));
        access.require(value);
        history.bypass(value);
        repository.deleteById(id);
        return true;
    }

    private Inventory inventory(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
    }

    private void moveStock(Restocking value) {
        // Serialize the destination even when its product assignment does not exist yet.
        shelfRepository.locked(value.getShelf().getId())
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        var source = productInventories.locked(value.getInventory().getId(), value.getProduct().getId())
                .orElseThrow(() -> new InvalidRestockingException("Product is not available in this inventory"));
        int quantity = value.getRestockedQuantity();
        if (source.getProductQuantity() < quantity) {
            throw new InvalidRestockingException("Insufficient inventory quantity");
        }
        int usable = quantity - value.getDamagedQuantity();
        if (usable > 0) {
            var destination = productShelves.locked(value.getProduct().getId(), value.getShelf().getId())
                    .orElseGet(() -> new ProductShelf(null, value.getProduct(), value.getShelf(), 0));
            long newQuantity = (long) destination.getProductQuantity() + usable;
            if (newQuantity > Integer.MAX_VALUE) {
                throw new InvalidRestockingException("Shelf quantity exceeds the supported maximum");
            }
            destination.setProductQuantity((int) newQuantity);
            productShelves.save(destination);
        }
        source.setProductQuantity(source.getProductQuantity() - quantity);
    }

    private void validateQuantities(Restocking value) {
        if (value.getRestockedQuantity() <= 0 || value.getDamagedQuantity() < 0
                || value.getDamagedQuantity() > value.getRestockedQuantity()) {
            throw new InvalidRestockingException("Damaged quantity must be between zero and restocked quantity");
        }
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
