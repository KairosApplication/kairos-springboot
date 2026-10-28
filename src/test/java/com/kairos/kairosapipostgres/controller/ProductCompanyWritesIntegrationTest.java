package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductCompanyWritesIntegrationTest {
    private static final String EMAIL = "product-manager@kairos.example";
    private static final String BASE = "/api/v1/products";
    @Autowired private MockMvc mvc;
    @Autowired private CompanyRepository companies;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    private Company company;
    private Company otherCompany;
    private Employee manager;
    private Category category;

    @BeforeEach
    void fixtures() {
        company = TestCompanyFactory.create(companies);
        otherCompany = TestCompanyFactory.create(companies);
        User user = users.save(new User(null, "Product", "Manager", LocalDate.of(1990, 1, 1),
                "12345678901", EMAIL, "{noop}test-only"));
        manager = employees.save(new Employee(null, Position.MANAGER, user, company));
        category = categories.saveAndFlush(new Category(null, "Test category"));
    }

    @Test
    void registersInTheAuthenticatedCompanyIgnoringAnExternalCompanySelection() throws Exception {
        mvc.perform(post(BASE + "/company/registration").param("companyId", otherCompany.getId().toString())
                        .with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("New product")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("New product"));
        assertThat(products.findByName("New product").orElseThrow().getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    void updatesAndDeletesAnOwnedProductWithoutChangingItsCompany() throws Exception {
        Product product = product(company, "Owned product");
        mvc.perform(patch(BASE + "/company/update/{id}", product.getId())
                        .with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":2.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(2.5));
        assertThat(products.findById(product.getId()).orElseThrow().getCompany().getId()).isEqualTo(company.getId());
        mvc.perform(delete(BASE + "/company/delete/{id}", product.getId()).with(user(EMAIL).roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(products.existsById(product.getId())).isFalse();
    }

    @Test
    void rejectsUpdatingOrDeletingAnotherCompanyProduct() throws Exception {
        Product product = product(otherCompany, "Other product");
        mvc.perform(patch(BASE + "/update/{id}", product.getId())
                        .with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/delete/{id}", product.getId()).with(user(EMAIL).roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(products.findById(product.getId()).orElseThrow().getName()).isEqualTo("Other product");
    }

    @Test
    void rejectsAdoptingOrDeletingALegacyProductWithoutCompany() throws Exception {
        Product product = product(null, "Legacy product");
        mvc.perform(patch(BASE + "/update/{id}", product.getId())
                        .with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/delete/{id}", product.getId()).with(user(EMAIL).roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(products.findById(product.getId()).orElseThrow().getCompany()).isNull();
    }

    @Test
    void rejectsRegistrationForAManagerWithoutCompanyOrEmployee() throws Exception {
        manager.setCompany(null);
        employees.saveAndFlush(manager);
        mvc.perform(post(BASE + "/registration").with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("Rejected product")))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/registration").with(user("missing@kairos.example").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("Rejected product")))
                .andExpect(status().isForbidden());
        assertThat(products.findByName("Rejected product")).isEmpty();
    }

    @Test
    void keepsValidationAndCsrfProtection() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user(EMAIL).roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("No csrf")))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/registration").with(user(EMAIL).roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.price").exists());
    }

    private Product product(Company owner, String name) {
        return products.saveAndFlush(new Product(null, "Brand", BigDecimal.ONE, name, category, owner));
    }

    private String body(String name) {
        return "{\"categoryId\":%d,\"brand\":\"Brand\",\"price\":1.25,\"name\":\"%s\"}".formatted(category.getId(), name);
    }
}
