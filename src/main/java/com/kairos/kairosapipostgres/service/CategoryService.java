package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CategoryRequest;
import com.kairos.kairosapipostgres.dto.request.CategoryUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.CategoryResponse;
import com.kairos.kairosapipostgres.exception.CategoryAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.CategoryNotFoundException;
import com.kairos.kairosapipostgres.mapper.CategoryMapper;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse save(CategoryRequest request) {
        if (repository.existsByCategory(request.category())) {
            throw new CategoryAlreadyExistsException("Category already exists");
        }

        Category category = CategoryMapper.toEntity(request);
        return CategoryMapper.toResponse(repository.save(category));
    }

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> findByCategory(String category) {
        Category categoryEntity = repository.findByCategory(category)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        return Optional.of(CategoryMapper.toResponse(categoryEntity));
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> findById(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        return Optional.of(CategoryMapper.toResponse(category));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public Optional<CategoryResponse> update(Long id, CategoryUpdateRequest request) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        repository.findByCategory(request.category())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException("Category already exists");
                });

        category.setCategory(request.category());
        return Optional.of(CategoryMapper.toResponse(repository.save(category)));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        repository.deleteById(id);
        return true;
    }
}
