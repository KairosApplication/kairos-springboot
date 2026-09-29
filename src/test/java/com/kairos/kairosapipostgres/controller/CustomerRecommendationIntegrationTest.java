package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Plan;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerRecommendationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private PurchaseRepository purchases;
    @Autowired private PurchaseProductRepository purchaseProducts;

    @Test
    void shouldRecommendUnpurchasedProductsInMostPurchasedCategories() throws Exception {
        User account = users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                "12345678909", "ana.recommend@example.com", "password", "12345678", Plan.STANDART));
        Customer customer = customers.saveAndFlush(new Customer(null, account));
        Category food = categories.saveAndFlush(new Category(null, "Food"));
        Category cleaning = categories.saveAndFlush(new Category(null, "Cleaning"));
        Product boughtFood = product("Rice", food);
        Product recommendedFood = product("Beans", food);
        Product boughtCleaning = product("Soap", cleaning);
        Product recommendedCleaning = product("Detergent", cleaning);
        product("Unrelated", categories.saveAndFlush(new Category(null, "Other")));

        Purchase completed = purchases.saveAndFlush(new Purchase(null, customer, null, PurchaseStatus.COMPLETED));
        item(completed, boughtFood, 4);
        item(completed, boughtCleaning, 1);
        Purchase cancelled = purchases.saveAndFlush(new Purchase(null, customer, null, PurchaseStatus.CANCELLED));
        item(cancelled, recommendedCleaning, 10);

        String path = "/api/v1/customers/{id}/recommendations";
        mvc.perform(get(path, customer.getId()).param("limit", "2")
                .with(user(account.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(recommendedFood.getId()))
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[1].productId").value(recommendedCleaning.getId()));
        mvc.perform(get(path, customer.getId()).param("limit", "1")
                .with(user(account.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(path, customer.getId()).with(user("other@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(path, customer.getId()).param("limit", "0")
                .with(user(account.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    private Product product(String name, Category category) {
        return products.saveAndFlush(new Product(null, "Brand", BigDecimal.TEN, name, category));
    }

    private void item(Purchase purchase, Product product, int quantity) {
        purchaseProducts.saveAndFlush(new PurchaseProduct(null, product, purchase, quantity, null,
                BigDecimal.TEN));
    }
}
