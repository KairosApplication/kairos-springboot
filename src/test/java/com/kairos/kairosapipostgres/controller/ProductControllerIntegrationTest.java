package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    private static final String BASE = "/api/v1/products";

    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository products;
    @Autowired private CategoryRepository categories;

    private Category category;

    @BeforeEach
    void setUp() {
        products.deleteAll();
        categories.deleteAll();
        category = categories.save(new Category(null, "Alimentos"));
    }

    @AfterEach
    void cleanUp() {
        products.deleteAll();
        categories.deleteAll();
    }

    @Test
    void shouldAllowCustomerToReadAndFilterProducts() throws Exception {
        Product rice = create("Arroz", "19.90");
        create("Feijao", "20.00");

        mvc.perform(get(BASE + "/list/Marca").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(BASE + "/find/price").param("maxPrice", "19.90")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE + "/find/name").param("name", "Arroz")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(rice.getId()));
        mvc.perform(get(BASE + "/find/{id}", rice.getId())
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.category").value("Alimentos"));
        mvc.perform(get(BASE + "/list").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRejectAnonymousAndNonCustomerReads() throws Exception {
        mvc.perform(get(BASE + "/list"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/list").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/list").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldKeepProductWritesBlockedByDenyAll() throws Exception {
        Product product = create("Arroz", "19.90");

        mvc.perform(post(BASE + "/registration")
                        .with(user("customer").roles("CUSTOMER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"brand":"Marca","price":20,"name":"Feijao"}
                                """.formatted(category.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(patch(BASE + "/update/{id}", product.getId())
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":18.50}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/delete/{id}", product.getId())
                        .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(products.count()).isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getPrice())
                .isEqualByComparingTo("19.90");
    }

    @Test
    void shouldValidateCustomerReadParametersAndAcceptCorsPreflight() throws Exception {
        mvc.perform(get(BASE + "/find/price")
                        .with(user("customer").roles("CUSTOMER"))
                        .param("maxPrice", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(options(BASE + "/registration")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private Product create(String name, String price) {
        return products.saveAndFlush(new Product(
                null,
                "Marca",
                new BigDecimal(price),
                name,
                category
        ));
    }
}
