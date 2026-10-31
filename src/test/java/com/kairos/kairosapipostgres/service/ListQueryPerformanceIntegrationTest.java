package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.dto.response.InventoryResponse;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ListQueryPerformanceIntegrationTest {
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private CustomerRepository customers;
    @Autowired private CompanyRepository companies;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private InventoryRepository inventories;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private RestockingRepository restockings;
    @Autowired private EmployeeService employeeService;
    @Autowired private CustomerService customerService;
    @Autowired private RestockingService restockingService;
    @Autowired private InventoryService inventoryService;

    private User user(int index) {
        return users.save(new User(null, "Pessoa " + index, "Teste", LocalDate.of(2000, 1, 1),
                String.format("%011d", index), "performance" + index + "@example.com", "test-hash"));
    }

    private Statistics resetStatistics() {
        em.flush();
        em.clear();
        Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }

    @Test
    void employeeAndCustomerListsUseOneQueryEvenWhenTheNumberOfPeopleGrows() {
        Company company = TestCompanyFactory.create(companies);
        for (int batch = 0; batch < 2; batch++) {
            for (int i = batch * 20 + 1; i <= (batch + 1) * 20; i++) {
                employees.save(new Employee(null, Position.STOCKER, user(i), company));
                customers.save(new Customer(null, user(i + 100), company));
            }
            Statistics statistics = resetStatistics();
            assertThat(employeeService.findAll()).hasSize((batch + 1) * 20)
                    .allSatisfy(employee -> assertThat(employee.userName()).startsWith("Pessoa "));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            statistics = resetStatistics();
            assertThat(customerService.findAll()).hasSize((batch + 1) * 20)
                    .allSatisfy(customer -> assertThat(customer.userName()).startsWith("Pessoa "));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    @Test
    void inventoryOptionsLoadSectorNamesInOneQuery() {
        Company company = TestCompanyFactory.create(companies);
        for (int i = 0; i < 30; i++) {
            Sector sector = sectors.save(new Sector(null, "Setor " + i, "Teste", company));
            inventories.save(new Inventory(null, sector, company));
        }
        Statistics statistics = resetStatistics();
        assertThat(inventoryService.findAll()).hasSize(30)
                .allSatisfy(inventory -> assertThat(inventory.sectorName()).startsWith("Setor "));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "performance1@example.com", roles = "MANAGER")
    void restockingListUsesTwoQueriesAndPreservesCompanyAndEmployeeRestrictions() {
        Company company = TestCompanyFactory.create(companies);
        Company other = TestCompanyFactory.create(companies);
        Employee manager = employees.save(new Employee(null, Position.MANAGER, user(1), company));
        Employee stocker = employees.save(new Employee(null, Position.STOCKER, user(2), company));
        employees.save(new Employee(null, Position.STOCKER, user(3), other));
        employees.save(new Employee(null, Position.CASHIER, user(4), company));
        Sector sector = sectors.save(new Sector(null, "Setor", "Teste", company));
        Sector otherSector = sectors.save(new Sector(null, "Outro", "Teste", other));
        Aisle aisle = aisles.save(new Aisle(null, sector, 1));
        Shelf shelf = shelves.save(new Shelf(null, aisle, 100));
        Inventory inventory = inventories.save(new Inventory(null, sector, company));
        Inventory foreignInventory = inventories.save(new Inventory(null, otherSector, other));
        Category category = categories.save(new Category(null, "Categoria"));
        Product product = products.save(new Product(null, "Marca", BigDecimal.TEN, "Produto", category, company));
        for (int i = 0; i < 20; i++) {
            restockings.save(new Restocking(null, inventory, i % 2 == 0 ? manager : stocker,
                    shelf, product, LocalDateTime.now(), 10, 0));
        }
        restockings.save(new Restocking(null, foreignInventory, manager, shelf, product,
                LocalDateTime.now(), 10, 0));
        Long managerId = manager.getId();
        Long stockerId = stocker.getId();
        Statistics statistics = resetStatistics();
        assertThat(restockingService.findAll()).hasSize(20);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        var managerOptions = restockingService.options();
        assertThat(managerOptions.people()).extracting(EmployeeResponse::id)
                .containsExactlyInAnyOrder(managerId, stockerId);
        assertThat(managerOptions.inventories()).extracting(InventoryResponse::id)
                .containsExactly(inventory.getId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "performance2@example.com", "unused", AuthorityUtils.createAuthorityList("ROLE_EMPLOYEE")));
        statistics = resetStatistics();
        assertThat(restockingService.findAll()).hasSize(10)
                .allSatisfy(record -> {
                    assertThat(record.employeeId()).isEqualTo(stockerId);
                    assertThat(record.employeeId()).isNotEqualTo(managerId);
                });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        var stockerOptions = restockingService.options();
        assertThat(stockerOptions.people()).extracting(EmployeeResponse::id).containsExactly(stockerId);
        assertThat(stockerOptions.inventories()).extracting(InventoryResponse::id)
                .containsExactly(inventory.getId());
    }
}
