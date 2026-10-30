package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class StoreMapControllerIntegrationTest {
    private static final String EMAIL = "map-manager@kairos.example";
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private CompanyRepository companies;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private ProductShelfRepository productShelves;
    private Company otherCompany;
    private Employee manager;

    @BeforeEach
    void fixtures() {
        Company company = TestCompanyFactory.create(companies);
        otherCompany = TestCompanyFactory.create(companies);
        User user = users.save(new User(null, "Map", "Manager", LocalDate.of(1990, 1, 1),
                "12345678901", EMAIL, "{noop}test-only"));
        manager = employees.save(new Employee(null, Position.MANAGER, user, company));
        Shelf ownShelf = shelf(company, "Own sector", 1);
        Shelf otherShelf = shelf(otherCompany, "Other sector", 2);
        Category category = categories.save(new Category(null, "Test category"));
        Product ownProduct = products.save(new Product(null, "Brand", BigDecimal.ONE, "Own product", category, company));
        Product otherProduct = products.save(new Product(null, "Brand", BigDecimal.ONE, "Other product", category, otherCompany));
        productShelves.save(new ProductShelf(null, ownProduct, ownShelf, 0));
        productShelves.save(new ProductShelf(null, otherProduct, otherShelf, 8));
        productShelves.save(new ProductShelf(null, otherProduct, ownShelf, 4));
        entityManager.flush();
    }

    private Shelf shelf(Company company, String name, int position) {
        Sector sector = sectors.save(new Sector(null, name, "Test", company));
        Aisle aisle = aisles.save(new Aisle(null, sector, position));
        return shelves.save(new Shelf(null, aisle, 100));
    }

    @Test
    void returnsOnlyTheCurrentCompanyInSixQueriesEvenIfAnotherCompanyIsRequested() throws Exception {
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(get("/api/v1/store-map").param("companyId", otherCompany.getId().toString())
                        .with(user(EMAIL).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.sectors.length()").value(1))
                .andExpect(jsonPath("$.sectors[0].name").value("Own sector"))
                .andExpect(jsonPath("$.aisles.length()").value(1))
                .andExpect(jsonPath("$.shelves.length()").value(1))
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Own product"))
                .andExpect(jsonPath("$.productShelves.length()").value(1))
                .andExpect(jsonPath("$.productShelves[0].productQuantity").value(0));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/store-map")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonManagerRoles() throws Exception {
        mvc.perform(get("/api/v1/store-map").with(user(EMAIL).roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnAccountWithoutEmployeeMapping() throws Exception {
        mvc.perform(get("/api/v1/store-map").with(user("unknown@kairos.example").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnEmployeeWithoutCompany() throws Exception {
        manager.setCompany(null);
        employees.saveAndFlush(manager);
        mvc.perform(get("/api/v1/store-map").with(user(EMAIL).roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsEmptyCollectionsForACompanyWithoutRegistrations() throws Exception {
        manager.setCompany(TestCompanyFactory.create(companies));
        employees.saveAndFlush(manager);
        mvc.perform(get("/api/v1/store-map").with(user(EMAIL).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sectors").isEmpty())
                .andExpect(jsonPath("$.aisles").isEmpty()).andExpect(jsonPath("$.shelves").isEmpty())
                .andExpect(jsonPath("$.products").isEmpty()).andExpect(jsonPath("$.productShelves").isEmpty());
    }
}
