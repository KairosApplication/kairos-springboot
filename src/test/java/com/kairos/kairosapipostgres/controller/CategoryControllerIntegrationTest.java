package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository repository;

    @Test
    void shouldRunCrudAndUpdateSameRecord() throws Exception {
        mockMvc.perform(post("/api/v1/categories/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isCreated());
        Long id = repository.findByCategory("Bebidas").orElseThrow().getId();
        long count = repository.count();
        mockMvc.perform(patch("/api/v1/categories/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Alimentos\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        repository.flush();
        assertThat(repository.count()).isEqualTo(count);
        assertThat(repository.findById(id).orElseThrow().getCategory()).isEqualTo("Alimentos");
        mockMvc.perform(get("/api/v1/categories/find/{id}", id).with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoria").value("Alimentos"));
        mockMvc.perform(get("/api/v1/categories/list").with(user("tester"))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/categories/delete/{id}", id).with(user("tester")))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void shouldReturnNotFoundAndConflict() throws Exception {
        mockMvc.perform(get("/api/v1/categories/find/{id}", -1L).with(user("tester")))
                .andExpect(status().isNotFound());
        repository.saveAndFlush(new Category(null, "Bebidas"));
        mockMvc.perform(post("/api/v1/categories/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Bebidas\"}"))
                .andExpect(status().isConflict());
    }
}
