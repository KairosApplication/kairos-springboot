package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.ProductShelfRequest;
import com.kairos.kairosapipostgres.dto.request.ProductShelfUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ProductShelfResponse;
import com.kairos.kairosapipostgres.exception.ShelfNotFoundException;
import com.kairos.kairosapipostgres.exception.ProductShelfAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.ProductShelfNotFoundException;
import com.kairos.kairosapipostgres.exception.ProductNotFoundException;
import com.kairos.kairosapipostgres.mapper.ProductShelfMapper;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.ProductShelf;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import com.kairos.kairosapipostgres.repository.ProductShelfRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductShelfService {
    private final ProductShelfRepository repository;
    private final ProductRepository productRepository;
    private final ShelfRepository shelfRepository;

    public ProductShelfService(ProductShelfRepository repository, ProductRepository productRepository,
                                ShelfRepository shelfRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.shelfRepository = shelfRepository;
    }

    @Transactional
    public ProductShelfResponse save(ProductShelfRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        Shelf shelf = shelfRepository.findById(request.shelfId())
                .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        checkDuplicate(product.getId(), shelf.getId(), null);
        return ProductShelfMapper.toResponse(repository.save(ProductShelfMapper.toEntity(request, product, shelf)));
    }

    @Transactional(readOnly = true)
    public List<ProductShelfResponse> findAll() {
        return repository.findAll().stream().map(ProductShelfMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductShelfResponse> findById(Long id) {
        return Optional.of(ProductShelfMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new ProductShelfNotFoundException("Shelf product assignment not found"))));
    }

    @Transactional(readOnly = true)
    public List<ProductShelfResponse> findByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Product not found");
        }
        return repository.findByProductId(productId).stream().map(ProductShelfMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductShelfResponse> findByShelfId(Long shelfId) {
        if (!shelfRepository.existsById(shelfId)) {
            throw new ShelfNotFoundException("Shelf not found");
        }
        return repository.findByShelfId(shelfId).stream().map(ProductShelfMapper::toResponse).toList();
    }

    @Transactional
    public Optional<ProductShelfResponse> update(Long id, ProductShelfUpdateRequest request) {
        ProductShelf assignment = repository.findById(id)
                .orElseThrow(() -> new ProductShelfNotFoundException("Shelf product assignment not found"));
        Product product = request.productId() == null ? assignment.getProduct()
                : productRepository.findById(request.productId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        Shelf shelf = request.shelfId() == null ? assignment.getShelf()
                : shelfRepository.findById(request.shelfId())
                    .orElseThrow(() -> new ShelfNotFoundException("Shelf not found"));
        checkDuplicate(product.getId(), shelf.getId(), id);
        assignment.setProduct(product);
        assignment.setShelf(shelf);
        if (request.productQuantity() != null) {
            assignment.setProductQuantity(request.productQuantity());
        }
        return Optional.of(ProductShelfMapper.toResponse(repository.save(assignment)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ProductShelfNotFoundException("Shelf product assignment not found"));
        repository.deleteById(id);
        return true;
    }

    private void checkDuplicate(Long productId, Long shelfId, Long currentId) {
        repository.findByProductIdAndShelfId(productId, shelfId)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ProductShelfAlreadyExistsException("Product already assigned to this shelf");
                });
    }
}
