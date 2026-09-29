package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.model.Inventory;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.ProductInventory;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.InventoryRepository;
import com.kairos.kairosapipostgres.repository.ProductInventoryRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
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
class InventoryControllerIntegrationTest {
    private static final String BASE = "/api/v1/inventories";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private InventoryRepository inventories;
    @Autowired private ProductInventoryRepository productInventories;
    @Autowired private ProductRepository products;
    @Autowired private CategoryRepository categories;

    @Test
    void shouldCreateFindMoveAndDeleteInventory() throws Exception {
        Sector first = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Sector second = sectors.saveAndFlush(new Sector(null, "Vendas", "Operacional"));
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d}".formatted(first.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sectorId").value(first.getId()));
        Long id = inventories.findAll().getFirst().getId();
        mvc.perform(get(BASE + "/find/sector").param("sectorId", first.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d}".formatted(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sectorId").value(second.getId()));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(inventories.count()).isZero();
    }

    @Test
    void shouldRejectUnknownSector() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sectorId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCheckProductAvailabilityFromInventoryStock() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Inventory inventory = inventories.saveAndFlush(new Inventory(null, sector));
        Category category = categories.saveAndFlush(new Category(null, "Alimentos"));
        Product product = products.saveAndFlush(new Product(null, "Marca", BigDecimal.TEN, "Arroz", category));
        productInventories.saveAndFlush(new ProductInventory(null, product, inventory, 5));

        String path = BASE + "/{inventoryId}/products/{productId}/availability";
        mvc.perform(get(path, inventory.getId(), product.getId()).param("quantity", "5")
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        mvc.perform(get(path, inventory.getId(), product.getId()).param("quantity", "6")
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(content().string("false"));
        mvc.perform(get(path, inventory.getId(), product.getId()).param("quantity", "0")
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(content().string("false"));
        mvc.perform(get(path, inventory.getId(), product.getId() + 1000).param("quantity", "1")
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(content().string("false"));
    }
}
