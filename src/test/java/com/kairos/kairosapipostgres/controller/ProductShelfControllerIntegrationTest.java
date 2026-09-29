package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductShelfControllerIntegrationTest {
    private static final String BASE = "/api/v1/product-shelves";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private ProductShelfRepository stock;

    @Test
    void shouldPlaceUpdateAndRemoveProductFromShelf() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Shelf shelf = shelves.saveAndFlush(new Shelf(null, aisle, 100));
        Category category = categories.saveAndFlush(new Category(null, "Alimentos"));
        Product product = products.saveAndFlush(new Product(null, "Marca", BigDecimal.TEN, "Arroz", category));
        String body = request(product.getId(), shelf.getId(), 10);

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.productQuantity").value(10));
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        ProductShelf placed = stock.findByProductIdAndShelfId(product.getId(), shelf.getId()).orElseThrow();
        mvc.perform(get(BASE + "/find/shelf").param("shelfId", shelf.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", placed.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"productQuantity\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productQuantity").value(20));
        mvc.perform(delete(BASE + "/delete/{id}", placed.getId())
                .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(stock.count()).isZero();
    }

    @Test
    void shouldRejectMissingProductAndNonpositiveQuantity() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request(999999L, 1L, 1)))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request(1L, 1L, 0)))
                .andExpect(status().isBadRequest());
    }

    private String request(Long productId, Long shelfId, Integer quantity) {
        return "{\"productId\":%d,\"shelfId\":%d,\"productQuantity\":%d}"
                .formatted(productId, shelfId, quantity);
    }
}
