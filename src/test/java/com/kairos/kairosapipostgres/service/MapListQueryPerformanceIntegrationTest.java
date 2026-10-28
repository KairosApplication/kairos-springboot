package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class MapListQueryPerformanceIntegrationTest {
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private AisleService aisleService;
    @Autowired private ShelfService shelfService;
    @Autowired private ProductService productService;
    private Statistics statistics;

    @BeforeEach
    void seedDistinctRelationsAndClearPersistenceContext() {
        for (int index = 1; index <= 5; index++) {
            Sector sector = sectors.save(new Sector(null, "Map sector " + index, "Test"));
            Aisle aisle = aisles.save(new Aisle(null, sector, index));
            shelves.save(new Shelf(null, aisle, 100));
            Category category = categories.save(new Category(null, "Map category " + index));
            products.save(new Product(null, "Test brand", BigDecimal.ONE, "Map product " + index, category));
        }
        entityManager.flush();
        entityManager.clear();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void shouldListAislesWithSectorNamesInOneQuery() {
        var result = aisleService.findAll();
        assertThat(result).hasSize(5);
        assertThat(result).allSatisfy(item -> assertThat(item.sectorName()).startsWith("Map sector "));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void shouldListShelvesWithAislePositionsInOneQuery() {
        var result = shelfService.findAll();
        assertThat(result).hasSize(5);
        assertThat(result).extracting(item -> item.aislePosition()).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void shouldListProductsWithCategoriesInOneQuery() {
        var result = productService.findAll();
        assertThat(result).hasSize(5);
        assertThat(result).allSatisfy(item -> assertThat(item.category().category()).startsWith("Map category "));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
