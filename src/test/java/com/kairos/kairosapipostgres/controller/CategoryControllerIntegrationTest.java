package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository repository;

    @Test
    void shouldRunCrudAndUpdateSameRecord() throws Exception {
        mockMvc.perform(post("/api/v1/categories/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Bebidas"))
                .andExpect(jsonPath("$.categoria").doesNotExist());
        Long id = repository.findByCategory("Bebidas").orElseThrow().getId();
        long count = repository.count();
        mockMvc.perform(patch("/api/v1/categories/update/{id}", id).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Alimentos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.category").value("Alimentos"));
        repository.flush();
        assertThat(repository.count()).isEqualTo(count);
        assertThat(repository.findById(id).orElseThrow().getCategory()).isEqualTo("Alimentos");
        mockMvc.perform(get("/api/v1/categories/find/{id}", id).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Alimentos"));
        mockMvc.perform(get("/api/v1/categories/list").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Alimentos"));
        mockMvc.perform(delete("/api/v1/categories/delete/{id}", id).with(user("tester")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void shouldReturnNotFoundAndConflict() throws Exception {
        mockMvc.perform(get("/api/v1/categories/find/{id}", -1L).with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
        repository.saveAndFlush(new Category(null, "Bebidas"));
        mockMvc.perform(post("/api/v1/categories/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category already exists"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"category\":null}", "{\"category\":\"\"}", "{\"category\":\"   \"}"})
    void shouldRejectRegistrationWithoutCategoryName(String body) throws Exception {
        mockMvc.perform(post("/api/v1/categories/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.category").value("Categoria é obrigatória"));

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"category\":null}", "{\"category\":\"\"}", "{\"category\":\"   \"}"})
    void shouldRejectUpdateWithoutCategoryName(String body) throws Exception {
        Category category = repository.saveAndFlush(new Category(null, "Bebidas"));

        mockMvc.perform(patch("/api/v1/categories/update/{id}", category.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.category").value("Categoria é obrigatória"));

        assertThat(repository.findById(category.getId()).orElseThrow().getCategory()).isEqualTo("Bebidas");
    }

    @Test
    void shouldAllowUpdatingCategoryWithItsCurrentName() throws Exception {
        Category category = repository.saveAndFlush(new Category(null, "Bebidas"));

        mockMvc.perform(patch("/api/v1/categories/update/{id}", category.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId()))
                .andExpect(jsonPath("$.category").value("Bebidas"));

        repository.flush();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectUpdateWithAnotherCategoryName() throws Exception {
        Category category = repository.saveAndFlush(new Category(null, "Bebidas"));
        repository.saveAndFlush(new Category(null, "Alimentos"));

        mockMvc.perform(patch("/api/v1/categories/update/{id}", category.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Alimentos\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category already exists"));

        assertThat(repository.findById(category.getId()).orElseThrow().getCategory()).isEqualTo("Bebidas");
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingOrDeletingMissingCategory() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/update/{id}", -1L).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        mockMvc.perform(delete("/api/v1/categories/delete/{id}", -1L).with(user("tester")).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }
}
