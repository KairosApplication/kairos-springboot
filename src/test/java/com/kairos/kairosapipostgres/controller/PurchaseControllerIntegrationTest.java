package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.PurchaseStatus;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PurchaseControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private PurchaseRepository purchases;
    @Autowired private PurchaseProductRepository purchaseProducts;

    @Test
    void shouldCalculateTotalFromPurchaseItems() throws Exception {
        User account = users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                "12345678909", "ana.purchase@example.com", "password"));
        Customer customer = customers.saveAndFlush(new Customer(null, account));
        Category category = categories.saveAndFlush(new Category(null, "Food"));
        Product product = products.saveAndFlush(new Product(null, "Brand", BigDecimal.TEN, "Rice", category));
        Purchase purchase = purchases.saveAndFlush(new Purchase(null, customer, LocalDateTime.now(),
                PurchaseStatus.COMPLETED));
        purchaseProducts.saveAndFlush(new PurchaseProduct(null, product, purchase, 3,
                new BigDecimal("9.90")));

        mvc.perform(get("/api/v1/purchases/{id}/total", purchase.getId())
                .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value(purchase.getId()))
                .andExpect(jsonPath("$.total").value(29.70));
    }

    @Test
    void shouldEnforceManagerAccessAndReturnNotFound() throws Exception {
        mvc.perform(get("/api/v1/purchases/999/total"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/purchases/999/total").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/purchases/999/total").with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound());
    }
}
