package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CategoryRequest;
import com.kairos.kairosapipostgres.dto.response.CategoryResponse;
import com.kairos.kairosapipostgres.exception.CategoryAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.CategoryNotFoundException;
import com.kairos.kairosapipostgres.mapper.CategoryMapper;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService (CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CategoryResponse save(CategoryRequest category) {
        Category categoryEntity = CategoryMapper.toEntity(category);
        if (repository.existsByCategory(categoryEntity.getCategory())) {
            throw new CategoryAlreadyExistsException ("Category already exists");
        }
        return CategoryMapper.toResponse(repository.save(categoryEntity));
    }

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

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById (id)
                        .orElseThrow (() -> new CategoryNotFoundException ("Category not found"));
        repository.deleteById (id);
        return true;
    }

    @Transactional
    public Optional<CategoryResponse> update(Long id, CategoryRequest category) {
        Category categoryEntity = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        String categoryName = (category.category ()) != null ? category.category () : categoryEntity.getCategory ();

        repository.findByCategory(categoryName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException("Category already exists");
                });

        categoryEntity.setCategory (categoryName);
        return Optional.of (CategoryMapper.toResponse(repository
                .save(categoryEntity)));
    }



}
