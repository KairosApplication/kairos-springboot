package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.ProductRequest;
import com.kairos.kairosapipostgres.dto.request.ProductUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ProductResponse;
import com.kairos.kairosapipostgres.exception.CategoryNotFoundException;
import com.kairos.kairosapipostgres.exception.ProductAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.ProductNotFoundException;
import com.kairos.kairosapipostgres.mapper.ProductMapper;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;

    public ProductService (ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.repository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse save (ProductRequest product) {
        Category category = categoryRepository.findById (product.categoryId ())
                .orElseThrow(() -> new CategoryNotFoundException ("Category not found"));
        Product productEntity = ProductMapper.toEntity(product, category);
        if (repository.existsByName (productEntity.getName())) {
            throw new ProductAlreadyExistsException ("Product already exists");
        }

        return ProductMapper.toResponse(repository.save(productEntity));
    }

    @Transactional
    public Optional<ProductResponse> update (Long id, ProductUpdateRequest product) {
        Product productEntity = repository.findById (id).orElseThrow (() -> new ProductNotFoundException ("Product not found"));

        String name = product.name() != null ? product.name().trim() : productEntity.getName();
        String brand = product.brand() != null ? product.brand().trim() : productEntity.getBrand();
        BigDecimal price = product.price() != null ? product.price() : productEntity.getPrice();
        repository.findByName(name.trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ProductAlreadyExistsException("Product name already in use");
                });
        Category category = product.categoryId() != null
                ? categoryRepository.findById(product.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"))
                : productEntity.getCategory();

        productEntity.setName(name);
        productEntity.setBrand(brand);
        productEntity.setCategory(category);
        productEntity.setPrice(price);
        return Optional.of(ProductMapper.toResponse(repository.save(productEntity)));
    }

    @Transactional
    public boolean deleteById (Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException ("Product not found"));
        repository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findById (Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return Optional.of(ProductMapper.toResponse(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll () {
        return repository.findAll ().stream ().map (ProductMapper::toResponse).toList ();
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findByName (String name) {
        Product product = repository.findByName(name)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return Optional.of(ProductMapper.toResponse(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByBrand (String brand) {
        return repository.findByBrand(brand.trim())
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByMaxPrice(BigDecimal maxPrice) {
        return repository.findByPriceLessThanEqual(maxPrice)
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

}
