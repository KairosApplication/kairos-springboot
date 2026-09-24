package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestockingControllerIntegrationTest {
    private static final String BASE = "/api/v1/restockings";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private InventoryRepository inventories;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private RestockingRepository restockings;

    @Test
    void shouldRecordFindUpdateAndRemoveRestocking() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Shelf shelf = shelves.saveAndFlush(new Shelf(null, aisle, 100));
        Inventory inventory = inventories.saveAndFlush(new Inventory(null, sector));
        User employeeUser = users.saveAndFlush(new User(null, "Davi", "Dias", LocalDate.of(2000, 2, 12),
                "52998224725", "davi@example.com", "hash", "01310-100", Plan.STANDART));
        Employee employee = employees.saveAndFlush(new Employee(null, Position.STOCKER, employeeUser));
        Category category = categories.saveAndFlush(new Category(null, "Alimentos"));
        Product product = products.saveAndFlush(new Product(null, "Marca", BigDecimal.TEN, "Arroz", category));
        String body = """
                {"inventoryId":%d,"employeeId":%d,"shelfId":%d,"productId":%d,
                 "restockedQuantity":10}
                """.formatted(inventory.getId(), employee.getId(), shelf.getId(), product.getId());

        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.restockedQuantity").value(10))
                .andExpect(jsonPath("$.damagedQuantity").value(0))
                .andExpect(jsonPath("$.restockingDate").isNotEmpty());
        Long id = restockings.findAll().getFirst().getId();
        mvc.perform(get(BASE + "/find/shelf").param("shelfId", shelf.getId().toString())
                .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"restockedQuantity\":12,\"damagedQuantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.damagedQuantity").value(2));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(restockings.count()).isZero();
    }

    @Test
    void shouldRejectInvalidQuantitiesBeforeLookingUpReferences() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"inventoryId":1,"employeeId":1,"shelfId":1,"productId":1,
                         "restockedQuantity":0,"damagedQuantity":-1}
                        """))
                .andExpect(status().isBadRequest());
    }
}
