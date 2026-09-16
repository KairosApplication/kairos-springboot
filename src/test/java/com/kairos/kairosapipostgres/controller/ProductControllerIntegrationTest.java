package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.repository.CategoryRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository products;
    @Autowired private CategoryRepository categories;
    private Long categoryId;
    private static final String BASE = "/api/v1/products";

    @BeforeEach
    void setUp() {
        products.deleteAll();
        categories.deleteAll();
        categoryId = categories.save(new Category(null, "Alimentos")).getId();
    }

    @AfterEach
    void cleanUp() {
        products.deleteAll();
        categories.deleteAll();
    }

    private Long create(String name, String price) throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":%d,"brand":"Marca","price":%s,"name":"%s"}
                        """.formatted(categoryId, price, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.id").value(categoryId));
        return products.findByName(name).orElseThrow().getId();
    }

    @Test
    void shouldCreateProductsWithSameBrandAndCategoryAndFilterThem() throws Exception {
        Long id = create("Arroz", "19.90");
        create("Feijao", "20.00");
        assertThat(products.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("19.90");
        mvc.perform(get(BASE + "/list/Marca").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(BASE + "/find/price").param("maxPrice", "19.90").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE + "/find/name").param("name", "Arroz").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(BASE + "/find/{id}", id).with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category.category").value("Alimentos"));
        mvc.perform(get(BASE + "/list").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldPatchOnlyProvidedFieldsAndDeleteProduct() throws Exception {
        Long id = create("Arroz", "19.90");
        create("Feijao", "20.00");
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":18.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(18.50))
                .andExpect(jsonPath("$.brand").value("Marca"))
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.category.id").value(categoryId));
        Long other = categories.save(new Category(null, "Outra")).getId();
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"categoryId\":" + other + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category.id").value(other));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("tester")))
                .andExpect(status().isNoContent());
        assertThat(products.existsById(id)).isFalse();
        assertThat(categories.existsById(other)).isTrue();
    }

    @Test
    void shouldRejectDuplicateNamesOnCreateAndUpdate() throws Exception {
        create("Arroz", "19.90");
        Long id = create("Feijao", "20.00");
        mvc.perform(post(BASE + "/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":%d,"brand":"Outra","price":1,"name":" Arroz "}
                        """.formatted(categoryId)))
                .andExpect(status().isConflict());
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Arroz\"}"))
                .andExpect(status().isConflict());
        assertThat(products.findById(id).orElseThrow().getName()).isEqualTo("Feijao");
    }

    @Test
    void shouldReturnNotFoundForMissingProductAndCategory() throws Exception {
        mvc.perform(get(BASE + "/find/{id}", -1).with(user("tester")))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":999999,\"brand\":\"Marca\",\"price\":1,\"name\":\"Arroz\"}"))
                .andExpect(status().isNotFound());
        Long id = create("Arroz", "19.90");
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"categoryId\":999999}"))
                .andExpect(status().isNotFound());
        assertThat(products.findById(id).orElseThrow().getCategory().getId()).isEqualTo(categoryId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"price\":-1}", "{\"price\":1.999}", "{\"categoryId\":0}"})
    void shouldRejectInvalidRegistration(String body) throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(products.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"   \"}", "{\"brand\":\"\"}", "{\"price\":-1}", "{\"price\":1.999}", "{\"categoryId\":0}"})
    void shouldRejectInvalidPatch(String body) throws Exception {
        Long id = create("Arroz", "19.90");
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("19.90");
    }

    @Test
    void shouldRequireAuthenticationAndAcceptCorsPreflight() throws Exception {
        mvc.perform(get(BASE + "/list")).andExpect(status().isUnauthorized());
        mvc.perform(options(BASE + "/registration")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get(BASE + "/find/price").with(user("tester")).param("maxPrice", "-1"))
                .andExpect(status().isBadRequest());
    }
}
