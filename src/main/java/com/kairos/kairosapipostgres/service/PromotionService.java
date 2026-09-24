package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.PromotionRequest;
import com.kairos.kairosapipostgres.dto.request.PromotionUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.PromotionResponse;
import com.kairos.kairosapipostgres.exception.InvalidPromotionPeriodException;
import com.kairos.kairosapipostgres.exception.ProductNotFoundException;
import com.kairos.kairosapipostgres.exception.PromotionNotFoundException;
import com.kairos.kairosapipostgres.exception.ShelfNotFoundException;
import com.kairos.kairosapipostgres.mapper.PromotionMapper;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.Promotion;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import com.kairos.kairosapipostgres.repository.PromotionRepository;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {
    private final PromotionRepository repository;
    private final ShelfRepository shelfRepository;
    private final ProductRepository productRepository;

    public PromotionService(PromotionRepository repository, ShelfRepository shelfRepository,
                            ProductRepository productRepository) {
        this.repository = repository;
        this.shelfRepository = shelfRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public PromotionResponse save(PromotionRequest request) {
        validatePeriod(request.promotionStartDate(), request.promotionEndDate());
        Shelf shelf = request.shelfId() == null ? null : shelf(request.shelfId());
        Product product = request.productId() == null ? null : product(request.productId());
        return PromotionMapper.toResponse(repository.save(PromotionMapper.toEntity(request, shelf, product)));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> findAll() {
        return repository.findAll().stream().map(PromotionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PromotionResponse> findById(Long id) {
        return Optional.of(PromotionMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("Promotion not found"))));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> findByShelfId(Long shelfId) {
        shelf(shelfId);
        return repository.findByShelfId(shelfId).stream().map(PromotionMapper::toResponse).toList();
    }

    @Transactional
    public Optional<PromotionResponse> update(Long id, PromotionUpdateRequest request) {
        Promotion promotion = repository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException("Promotion not found"));
        LocalDateTime start = request.promotionStartDate() == null
                ? promotion.getPromotionStartDate() : request.promotionStartDate();
        LocalDateTime end = request.promotionEndDate() == null
                ? promotion.getPromotionEndDate() : request.promotionEndDate();
        validatePeriod(start, end);
        if (request.shelfId() != null) {
            promotion.setShelf(shelf(request.shelfId()));
        }
        if (request.productId() != null) {
            promotion.setProduct(product(request.productId()));
        }
        promotion.setPromotionStartDate(start);
        promotion.setPromotionEndDate(end);
        if (request.description() != null) {
            promotion.setDescription(request.description());
        }
        if (request.discountPercentage() != null) {
            promotion.setDiscountPercentage(request.discountPercentage());
        }
        return Optional.of(PromotionMapper.toResponse(repository.save(promotion)));
    }

    @Transactional
    public boolean deleteById(Long id) {
        repository.findById(id).orElseThrow(() -> new PromotionNotFoundException("Promotion not found"));
        repository.deleteById(id);
        return true;
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidPromotionPeriodException("Promotion end date must be after start date");
        }
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
