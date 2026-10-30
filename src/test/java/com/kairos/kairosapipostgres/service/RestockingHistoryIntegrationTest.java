package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.dto.request.*;
import com.kairos.kairosapipostgres.dto.response.*;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"test.history=true", "app.restocking-redis.enabled=true",
        "app.restocking-redis.retry-delay=86400000"})
@AutoConfigureMockMvc
class RestockingHistoryIntegrationTest {
    @Autowired private CompanyRepository companies;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private InventoryRepository inventories;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private RestockingRepository restockings;
    @Autowired private ProductInventoryRepository productInventories;
    @Autowired private ProductShelfRepository productShelves;
    @Autowired private RestockingHistoryDeliveryRepository deliveries;
    @Autowired private RestockingHistoryBypassRepository bypasses;
    @Autowired private RestockingService service;
    @Autowired private RestockingWeeklyHistory history;
    @Autowired private RestockingHistoryWorker worker;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private DataSource dataSource;
    @Autowired private Clock clock;
    @Autowired private MockMvc mvc;
    @MockitoBean private RestockingRedisClient client;
    private Company company;
    private Employee stocker;
    private Employee manager;
    private Employee cashier;
    private Employee outsider;
    private Shelf shelf;
    private Inventory inventory;
    private Product product;

    @BeforeEach
    void fixtures() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            company = TestCompanyFactory.create(companies);
            stocker = employee(company, Position.STOCKER);
            manager = employee(company, Position.MANAGER);
            cashier = employee(company, Position.CASHIER);
            outsider = employee(TestCompanyFactory.create(companies), Position.STOCKER);
            var sector = sectors.save(new Sector(null, "History " + UUID.randomUUID(), "Stock", company));
            shelf = shelves.save(new Shelf(null, aisles.save(new Aisle(null, sector, 1)), 100));
            inventory = inventories.save(new Inventory(null, sector, company));
            product = products.save(new Product(null, "Brand", BigDecimal.TEN, "History " + UUID.randomUUID(),
                    categories.save(new Category(null, "Category")), company));
            productInventories.save(new ProductInventory(null, product, inventory, 100));
        });
        as(stocker);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            deliveries.deleteAll();
            bypasses.deleteAll();
            restockings.deleteAll();
            productShelves.deleteAll();
            productInventories.deleteAll();
            products.deleteAll();
            categories.deleteAll();
            shelves.deleteAll();
            aisles.deleteAll();
            inventories.deleteAll();
            sectors.deleteAll();
            employees.deleteAll();
            users.deleteAll();
            companies.deleteAll();
        });
    }

    @Test
    void usesTheDatabaseSelectedByTheTestProfile() throws Exception {
        String expected = "postgresql".equals(System.getProperty("integration.database")) ? "PostgreSQL" : "H2";
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo(expected);
        }
    }

    @Test
    void publishesOnlyAfterPostgresCommitWithTheRealGeneratedIdAndTimezone() throws Exception {
        doAnswer(call -> {
            var event = call.getArgument(0, RestockingCacheEvent.class);
            try (var connection = dataSource.getConnection();
                 var query = connection.prepareStatement("select count(*) from restockings where id = ?")) {
                query.setLong(1, event.restockingId());
                try (var result = query.executeQuery()) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(1);
                }
            }
            assertThat(event.companyId()).isEqualTo(company.getId());
            assertThat(event.restockingDate().toInstant()).isBetween(clock.instant().minusSeconds(30), clock.instant().plusSeconds(1));
            return null;
        }).when(client).publish(any());
        var saved = service.save(request());
        verify(client).publish(argThat(event -> event.restockingId() == saved.id()));
        assertThat(deliveries.findFirstByRestockingId(saved.id()).orElseThrow().isDelivered()).isTrue();
    }

    @Test
    void rollbackNeverPublishesOrLeavesAnOutboxEvent() {
        long before = deliveries.count();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.save(request());
            verifyNoInteractions(client);
            status.setRollbackOnly();
        });
        verifyNoInteractions(client);
        assertThat(deliveries.count()).isEqualTo(before);
        assertThat(restockings.count()).isZero();
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(100);
        assertThat(productShelves.count()).isZero();
    }

    @Test
    void movesOnlyUsableUnitsToShelfAndAccumulatesFurtherRestockings() {
        service.save(new RestockingRequest(inventory.getId(), stocker.getId(), shelf.getId(), product.getId(), 10, 1));
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(90);
        assertThat(productShelves.findAll().getFirst().getProductQuantity()).isEqualTo(9);
        service.save(request());
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(80);
        assertThat(productShelves.findAll().getFirst().getProductQuantity()).isEqualTo(19);
    }

    @Test
    void rejectsMissingOrInsufficientStockWithoutRecordingHistory() {
        assertThatThrownBy(() -> service.save(new RestockingRequest(inventory.getId(), stocker.getId(),
                shelf.getId(), product.getId(), 101, 0)))
                .isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(100);
        productInventories.deleteAll();
        assertThatThrownBy(() -> service.save(request()))
                .isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        assertThat(productShelves.count()).isZero();
        assertThat(restockings.count()).isZero();
        assertThat(deliveries.count()).isZero();
        verifyNoInteractions(client);
    }

    @Test
    void allDamagedUnitsExhaustInventoryWithoutCreatingAnEmptyShelfAssignment() {
        service.save(new RestockingRequest(inventory.getId(), stocker.getId(), shelf.getId(), product.getId(), 100, 100));
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isZero();
        assertThat(productShelves.count()).isZero();
        assertThat(restockings.count()).isEqualTo(1);
    }

    @Test
    void concurrentRestockingsCannotSpendTheSameInventoryTwice() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> attempt = () -> {
            as(stocker);
            try {
                assertThat(start.await(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                service.save(new RestockingRequest(inventory.getId(), stocker.getId(), shelf.getId(), product.getId(), 60, 0));
                return true;
            } catch (com.kairos.kairosapipostgres.exception.InvalidRestockingException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(30, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(30, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(40);
        assertThat(productShelves.findAll().getFirst().getProductQuantity()).isEqualTo(60);
        assertThat(restockings.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
    }

    @Test
    void shelfQuantityOverflowLeavesTheInventoryAndHistoryUntouched() {
        productShelves.save(new ProductShelf(null, product, shelf, Integer.MAX_VALUE));
        assertThatThrownBy(() -> service.save(request()))
                .isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        assertThat(productInventories.findAll().getFirst().getProductQuantity()).isEqualTo(100);
        assertThat(productShelves.findAll().getFirst().getProductQuantity()).isEqualTo(Integer.MAX_VALUE);
        assertThat(restockings.count()).isZero();
        assertThat(deliveries.count()).isZero();
        verifyNoInteractions(client);
    }

    @Test
    void upstreamFailureKeepsThePermanentRecordAndRetriesFromTheDurableOutbox() {
        doThrow(new ResourceAccessException("unavailable")).when(client).publish(any());
        var saved = service.save(request());
        var queued = deliveries.findFirstByRestockingId(saved.id()).orElseThrow();
        assertThat(queued.isDelivered()).isFalse();
        assertThat(restockings.existsById(saved.id())).isTrue();
        assertThat(history.week(null, 100).total()).isEqualTo(1);
        verify(client, never()).week(anyLong(), anyLong());
        queued.setNextAttemptAt(clock.instant().minusSeconds(1));
        deliveries.save(queued);
        doNothing().when(client).publish(any());
        worker.retry();
        assertThat(deliveries.findById(queued.getId()).orElseThrow().isDelivered()).isTrue();
        worker.retry();
        verify(client, times(2)).publish(any());
    }

    @Test
    void cacheHitUsesTheInternalTenantAndReturnsTheSamePublicContract() throws Exception {
        var saved = service.save(request());
        var event = event(saved.id());
        when(client.week(company.getId(), stocker.getId())).thenReturn(cached(List.of(event)));
        mvc.perform(get("/api/v1/restockings/week").with(user(stocker.getUser().getEmail()).roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(company.getId()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].restocking_id").value(saved.id()));
        verify(client).week(company.getId(), stocker.getId());
    }

    @Test
    void emptyOrUnavailableCacheFallsBackAndSchedulesReconstruction() {
        var saved = service.save(request());
        when(client.week(anyLong(), anyLong())).thenThrow(new ResourceAccessException("offline"));
        assertThat(history.week(null, 100).items()).extracting(RestockingCacheEvent::restockingId).containsExactly(saved.id());
        assertThat(deliveries.findFirstByRestockingId(saved.id()).orElseThrow().isDelivered()).isFalse();
        worker.retry();
        doReturn(cached(List.of())).when(client).week(anyLong(), anyLong());
        assertThat(history.week(null, 100).items()).hasSize(1);
        worker.retry();
        when(client.week(anyLong(), anyLong())).thenReturn(cached(List.of(event(saved.id()))));
        assertThat(history.week(null, 100).items()).hasSize(1);
    }

    @Test
    void invalidCacheScopeOrWindowNeverLeaksForeignData() {
        var saved = service.save(request());
        var event = event(saved.id());
        var week = RestockingWeek.current(clock);
        List<RestockingRedisClient.CachedWeek> invalid = List.of(
                new RestockingRedisClient.CachedWeek(outsider.getCompany().getId(), stocker.getId(), week.startOffset(), week.endOffset(), 1, List.of(event)),
                new RestockingRedisClient.CachedWeek(company.getId(), outsider.getId(), week.startOffset(), week.endOffset(), 1, List.of(event)),
                new RestockingRedisClient.CachedWeek(company.getId(), stocker.getId(), week.startOffset().minusDays(7), week.endOffset(), 1, List.of(event)),
                cached(List.of(new RestockingCacheEvent(event.restockingId(), outsider.getCompany().getId(), event.employeeId(),
                        event.productId(), event.shelfId(), event.inventoryId(), 10, 0, event.restockingDate(), "Foreign"))));
        for (var response : invalid) {
            when(client.week(anyLong(), anyLong())).thenReturn(response);
            assertThat(history.week(null, 100).items().getFirst().productName()).isEqualTo(product.getName());
            worker.retry();
        }
    }

    @Test
    void updatesAndDeletesBypassTheAppendOnlyCacheUntilTheNextWeek() {
        var saved = service.save(request());
        service.update(saved.id(), new RestockingUpdateRequest(null, null, null, null, 20, 2));
        assertThat(history.week(null, 100).items().getFirst().restockedQuantity()).isEqualTo(20);
        verify(client, never()).week(anyLong(), anyLong());
        service.deleteById(saved.id());
        assertThat(history.week(null, 100).items()).isEmpty();
        assertThat(history.week(null, 100).total()).isZero();
        service.save(request());
        verify(client, times(2)).publish(any());
    }

    @Test
    void managerCanReadOwnCompanyStockerButOtherStockersAndCashiersCannot() throws Exception {
        service.save(request());
        as(manager);
        assertThat(history.week(stocker.getId(), 100).total()).isEqualTo(1);
        mvc.perform(get("/api/v1/restockings/week").param("employeeId", stocker.getId().toString())
                        .with(user(outsider.getUser().getEmail()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        for (Employee denied : List.of(cashier, outsider)) {
            mvc.perform(get("/api/v1/restockings/week").param("employeeId", stocker.getId().toString())
                            .with(user(denied.getUser().getEmail()).roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/restockings/week").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/restockings/week").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesTenantReferencesAndQuantitiesBeforeSaving() throws Exception {
        var foreignInventory = inventories.save(new Inventory(null, null, outsider.getCompany()));
        var invalid = new RestockingRequest(foreignInventory.getId(), stocker.getId(), shelf.getId(), product.getId(), 10, 0);
        assertThatThrownBy(() -> service.save(invalid)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(post("/api/v1/restockings/registration").with(user(stocker.getUser().getEmail()).roles("EMPLOYEE"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                                {"inventoryId":%d,"employeeId":%d,"shelfId":%d,"productId":%d,
                                 "restockedQuantity":1,"damagedQuantity":2}
                                """.formatted(inventory.getId(), stocker.getId(), shelf.getId(), product.getId())))
                .andExpect(status().isBadRequest());
        assertThat(restockings.count()).isZero();
        verifyNoInteractions(client);
    }

    @Test
    void rejectsInvalidLimitsAndNeverIncludesPreviousWeeks() {
        assertThatThrownBy(() -> history.week(null, 501)).isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        assertThatThrownBy(() -> history.week(null, 0)).isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        assertThatThrownBy(() -> history.week(-1L, 1)).isInstanceOf(com.kairos.kairosapipostgres.exception.InvalidRestockingException.class);
        var saved = service.save(request());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var value = restockings.findById(saved.id()).orElseThrow();
            value.setRestockingDate(RestockingWeek.current(clock).from().minusSeconds(1));
        });
        assertThat(history.week(null, 100).items()).isEmpty();
    }

    @Test
    void cleanupRemovesOldWeekDeliveryAndBypassMetadata() {
        var saved = service.save(request());
        service.update(saved.id(), new RestockingUpdateRequest(null, null, null, null, 11, null));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            bypasses.findAll().forEach(marker -> marker.setWeekStart(RestockingWeek.current(clock).start().minusDays(7)));
        });
        worker.cleanup();
        assertThat(bypasses.count()).isZero();
    }

    private RestockingRequest request() {
        return new RestockingRequest(inventory.getId(), stocker.getId(), shelf.getId(), product.getId(), 10, 0);
    }

    private void as(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                employee.getUser().getEmail(), "", employee.getPosition() == Position.MANAGER ? "ROLE_MANAGER" : "ROLE_EMPLOYEE"));
    }

    private Employee employee(Company tenant, Position position) {
        String unique = UUID.randomUUID().toString();
        var user = users.save(new User(null, "Test", "Employee", LocalDate.of(1990, 1, 1),
                unique.substring(0, 14), unique + "@example.com", "hash"));
        return employees.save(new Employee(null, position, user, tenant));
    }

    private RestockingCacheEvent event(long id) {
        return new TransactionTemplate(transactions).execute(status -> RestockingCacheEvent.from(restockings.findById(id).orElseThrow()));
    }

    private RestockingRedisClient.CachedWeek cached(List<RestockingCacheEvent> events) {
        var week = RestockingWeek.current(clock);
        return new RestockingRedisClient.CachedWeek(company.getId(), stocker.getId(), week.startOffset(), week.endOffset(), events.size(), events);
    }
}
