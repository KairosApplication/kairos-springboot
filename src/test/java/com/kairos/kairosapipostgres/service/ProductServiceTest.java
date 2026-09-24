package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.ProductRequest;
import com.kairos.kairosapipostgres.dto.request.ProductUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.ProductResponse;
import com.kairos.kairosapipostgres.exception.CategoryNotFoundException;
import com.kairos.kairosapipostgres.exception.ProductAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.ProductNotFoundException;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryRepository);
    }

    @Test
    void shouldCreateProductForExistingCategory() {
        Category category = category(10L, "Alimentos");
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productRepository.existsByName("Arroz")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductResponse response = service.save(new ProductRequest(
                10L, " Marca ", new BigDecimal("19.90"), " Arroz "
        ));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.brand()).isEqualTo("Marca");
        assertThat(response.name()).isEqualTo("Arroz");
        assertThat(response.category().id()).isEqualTo(10L);
    }

    @Test
    void shouldRejectProductForMissingCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(new ProductRequest(
                99L, "Marca", BigDecimal.ONE, "Arroz"
        )))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateProductName() {
        when(categoryRepository.findById(10L))
                .thenReturn(Optional.of(category(10L, "Alimentos")));
        when(productRepository.existsByName("Arroz")).thenReturn(true);

        assertThatThrownBy(() -> service.save(new ProductRequest(
                10L, "Marca", BigDecimal.ONE, " Arroz "
        )))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessage("Product already exists");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOnlyProvidedProductFields() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        Category newCategory = category(20L, "Limpeza");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Feijao")).thenReturn(Optional.empty());
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = service.update(1L, new ProductUpdateRequest(
                20L, " Nova Marca ", new BigDecimal("25.50"), " Feijao "
        )).orElseThrow();

        assertThat(response.name()).isEqualTo("Feijao");
        assertThat(response.brand()).isEqualTo("Nova Marca");
        assertThat(response.price()).isEqualByComparingTo("25.50");
        assertThat(response.category().id()).isEqualTo(20L);
    }

    @Test
    void shouldKeepExistingValuesOnEmptyUpdate() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Arroz")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = service.update(
                1L, new ProductUpdateRequest(null, null, null, null)
        ).orElseThrow();

        assertThat(response.name()).isEqualTo("Arroz");
        assertThat(response.brand()).isEqualTo("Marca");
        assertThat(response.price()).isEqualByComparingTo("19.90");
        assertThat(response.category().id()).isEqualTo(10L);
    }

    @Test
    void shouldRejectUpdatingToAnotherProductName() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        Product duplicate = product(2L, "Marca", "20.00", "Feijao", product.getCategory());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Feijao")).thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> service.update(
                1L, new ProductUpdateRequest(null, null, null, "Feijao")
        ))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessage("Product name already in use");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateForMissingCategory() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Arroz")).thenReturn(Optional.of(product));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                1L, new ProductUpdateRequest(99L, null, null, null)
        ))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void shouldRejectUpdateForMissingProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                99L, new ProductUpdateRequest(null, null, null, "Arroz")
        ))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void shouldDeleteExistingProduct() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(service.deleteById(1L)).isTrue();

        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldRejectDeletingMissingProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindProductByIdAndName() {
        Product product = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Arroz")).thenReturn(Optional.of(product));

        assertThat(service.findById(1L).orElseThrow().name()).isEqualTo("Arroz");
        assertThat(service.findByName("Arroz").orElseThrow().id()).isEqualTo(1L);
    }

    @Test
    void shouldRejectMissingProductSearches() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        when(productRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.findByName("Inexistente"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldListAndFilterProducts() {
        Product rice = product(1L, "Marca", "19.90", "Arroz", category(10L, "Alimentos"));
        Product beans = product(2L, "Marca", "20.00", "Feijao", rice.getCategory());
        when(productRepository.findAll()).thenReturn(List.of(rice, beans));
        when(productRepository.findByBrand("Marca")).thenReturn(List.of(rice, beans));
        when(productRepository.findByPriceLessThanEqual(new BigDecimal("19.90")))
                .thenReturn(List.of(rice));

        assertThat(service.findAll()).hasSize(2);
        assertThat(service.findByBrand(" Marca ")).extracting(ProductResponse::id)
                .containsExactly(1L, 2L);
        assertThat(service.findByMaxPrice(new BigDecimal("19.90")))
                .extracting(ProductResponse::name)
                .containsExactly("Arroz");
    }

    private Category category(Long id, String name) {
        return new Category(id, name);
    }

    private Product product(
            Long id,
            String brand,
            String price,
            String name,
            Category category
    ) {
        return new Product(id, brand, new BigDecimal(price), name, category);
    }
}
