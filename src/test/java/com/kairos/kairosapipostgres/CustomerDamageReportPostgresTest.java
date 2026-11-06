package com.kairos.kairosapipostgres;

import com.kairos.kairosapipostgres.dto.request.CustomerDamageReportRequest;
import com.kairos.kairosapipostgres.service.CustomerDamageReportService;
import com.kairos.kairosapipostgres.service.StockerDamageReportService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.*;

@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class CustomerDamageReportPostgresTest {
    private static Connection keepAlive;
    private static JdbcTemplate db;
    private static TransactionTemplate tx;
    private static CustomerDamageReportService service;
    private final Authentication customer = auth("customer1@invalid", "CUSTOMER");

    private static Authentication auth(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @BeforeAll
    static void database() throws Exception {
        String url = "jdbc:tc:postgresql:17-alpine:///kairos_customer_reports";
        keepAlive = DriverManager.getConnection(url, "test", "test");
        Flyway.configure().dataSource(url, "test", "test").load().migrate();
        var source = new DriverManagerDataSource(url, "test", "test");
        db = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        service = new CustomerDamageReportService(db);
    }

    @AfterAll
    static void close() throws Exception {
        if (keepAlive != null) {
            keepAlive.close();
        }
    }

    @BeforeEach
    void fixtures() {
        db.execute("TRUNCATE companies,users CASCADE");
        db.execute("""
                INSERT INTO companies VALUES
                (1,'Test A','00000000000001','a@invalid','BASIC'),(2,'Test B','00000000000002','b@invalid','BASIC');
                INSERT INTO users(id,name,last_name,birth_date,cpf,email,password) VALUES
                (1,'Client','A','2000-01-01','00000000001','customer1@invalid','not-a-login'),
                (2,'Stocker','A','2000-01-01','00000000002','stocker1@invalid','not-a-login'),
                (3,'Client','B','2000-01-01','00000000003','customer2@invalid','not-a-login'),
                (4,'Stocker','B','2000-01-01','00000000004','stocker2@invalid','not-a-login'),
                (5,'Other','A','2000-01-01','00000000005','other@invalid','not-a-login');
                INSERT INTO customers(id,users_id,company_id) VALUES (1,1,1),(2,3,2),(3,5,1);
                INSERT INTO employees(id,users_id,company_id,position) VALUES (1,2,1,'STOCKER'),(2,4,2,'STOCKER');
                INSERT INTO sectors(id,company_id,name,type) VALUES (1,1,'A','NORMAL'),(2,2,'B','NORMAL');
                INSERT INTO aisles(id,sector_id,position) VALUES (1,1,1),(2,2,1);
                INSERT INTO shelves(id,aisle_id,maximum_capacity) VALUES (1,1,100),(2,2,100);
                INSERT INTO products(id,company_id,name) VALUES (1,1,'Test product A'),(2,2,'Test product B');
                INSERT INTO product_shelves(product_id,shelf_id,product_quantity) VALUES (1,1,7),(2,2,9);
                INSERT INTO shelf_employees(shelf_id,employee_id) VALUES (1,1),(2,2),(1,2);
                """);
    }

    private CustomerDamageReportRequest request(UUID command, Long shelf, String description, String image) {
        return new CustomerDamageReportRequest(command, 1L, shelf, "DAMAGED", description, image);
    }

    private Map<String, Object> send(CustomerDamageReportRequest request) {
        return tx.execute(status -> service.report(customer, request));
    }

    private int count(String table) {
        return db.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    @Test
    void createsExactlyOneAlertAndLeavesStockUntouchedOnRetry() {
        UUID command = UUID.randomUUID();
        var first = send(request(command, 1L, "Broken package", null));
        assertThat(send(request(command, 1L, "Broken package", null))).isEqualTo(first);
        assertThat(count("alerts")).isEqualTo(1);
        assertThat(count("customer_damage_reports")).isEqualTo(1);
        assertThat(count("restockings")).isZero();
        assertThat(db.queryForObject("SELECT product_quantity FROM product_shelves WHERE product_id=1", Integer.class)).isEqualTo(7);
        assertThat(db.queryForObject("SELECT employee_id FROM alerts", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> send(request(command, 1L, "Different content", null)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(count("alerts")).isEqualTo(1);
    }

    @Test
    void concurrentRetriesAcrossTransactionsHaveTheSameProtocol() throws Exception {
        var request = request(UUID.randomUUID(), 1L, "Concurrent retry", null);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var start = new CountDownLatch(1);
            List<Future<Map<String, Object>>> results = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return send(request);
                }));
            }
            start.countDown();
            Set<Object> protocols = new HashSet<>();
            for (var result : results) {
                protocols.add(result.get(15, TimeUnit.SECONDS).get("protocol"));
            }
            assertThat(protocols).hasSize(1);
            assertThat(count("alerts")).isEqualTo(1);
        }
    }

    @Test
    void deniesForeignProductForeignShelfAndNonCustomerRole() {
        assertThatThrownBy(() -> tx.execute(status -> service.report(customer,
                new CustomerDamageReportRequest(UUID.randomUUID(), 2L, null, "DAMAGED", "", null))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> send(request(UUID.randomUUID(), 2L, "", null)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> tx.execute(status -> service.report(auth("stocker1@invalid", "EMPLOYEE"),
                request(UUID.randomUUID(), 1L, "", null)))).isInstanceOf(ResponseStatusException.class);
        assertThat(count("alerts")).isZero();
        assertThat(count("customer_damage_reports")).isZero();
    }

    @Test
    void missingLocationUsesValidProductShelfAndDoesNotClaimAnObservedLocation() {
        send(request(UUID.randomUUID(), null, "Problem", null));
        assertThat(db.queryForObject("SELECT shelf_id FROM alerts", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("SELECT description FROM alerts", String.class)).contains("Local nao informado");
        assertThat(db.queryForObject("SELECT shelf_id FROM customer_damage_reports", Integer.class)).isNull();
    }

    @Test
    void noResponsibleStockerRollsBackBothReportAndAlert() {
        db.update("DELETE FROM shelf_employees WHERE employee_id=1");
        assertThatThrownBy(() -> send(request(UUID.randomUUID(), null, "Problem", null)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(count("alerts")).isZero();
        assertThat(count("customer_damage_reports")).isZero();
    }

    @Test
    void customerSeesOnlyOwnReportsAndTracksActualAlertResolution() {
        var confirmation = send(request(UUID.randomUUID(), 1L, "Problem", null));
        db.update("UPDATE alerts SET status='RESOLVED',resolution_date=CURRENT_TIMESTAMP WHERE id=?", confirmation.get("alertId"));
        assertThat(service.list(customer)).hasSize(1);
        assertThat(service.list(customer).getFirst().get("status")).isEqualTo("RESOLVED");
        assertThat(service.list(auth("other@invalid", "CUSTOMER"))).isEmpty();
        assertThat(service.list(auth("customer2@invalid", "CUSTOMER"))).isEmpty();
        assertThat(service.options(customer).get("products").toString()).doesNotContain("Test product B");
    }

    @Test
    void acceptsRealJpegAndRejectsInvalidPhotoWithoutWritingAnything() throws Exception {
        assertThatThrownBy(() -> send(request(UUID.randomUUID(), 1L, "Problem", "bm90LWEtcGhvdG8=")))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(count("alerts")).isZero();
        var image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        var request = request(UUID.randomUUID(), 1L, "Photo", Base64.getEncoder().encodeToString(output.toByteArray()));
        var first = send(request);
        assertThat(send(request)).isEqualTo(first);
        assertThat(db.queryForObject("SELECT octet_length(photo) FROM customer_damage_reports", Integer.class)).isPositive();
    }
    private StockerDamageReportService stocker() {
        return new StockerDamageReportService(db);
    }

    private Authentication responsible() {
        return auth("stocker1@invalid", "EMPLOYEE");
    }

    private void assertStatus(int expected, Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(expected));
    }

    @Test
    void responsibleReceivesMetadataAndResolvesForCustomerWithStableResolutionDate() {
        var confirmation = send(request(UUID.randomUUID(), 1L, "Broken package", null));
        long id = ((Number) confirmation.get("alertId")).longValue();
        var rows = stocker().list(responsible());
        assertThat(rows).hasSize(1);
        var metadata = rows.getFirst();
        assertThat(metadata.keySet()).containsExactlyInAnyOrder("id", "report_id", "problem_type",
                "report_description", "product_name", "reported_at");
        assertThat(metadata.get("id")).isEqualTo(id);
        assertThat(metadata.get("report_id")).isEqualTo(confirmation.get("protocol"));
        assertThat(metadata.get("problem_type")).isEqualTo("DAMAGED");
        assertThat(metadata.get("report_description")).isEqualTo("Broken package");
        assertThat(metadata.get("product_name")).isEqualTo("Test product A");
        assertThat(java.time.Instant.parse(metadata.get("reported_at").toString())).isNotNull();
        Map<String, Object> resolved = tx.execute(status -> stocker().resolve(responsible(), id));
        assertThat(resolved).isEqualTo(Map.of("alertId", id, "status", "RESOLVED"));
        Object firstDate = db.queryForObject("SELECT resolution_date FROM alerts WHERE id=?", Object.class, id);
        assertThat(firstDate).isNotNull();
        tx.execute(status -> stocker().resolve(responsible(), id));
        assertThat(db.queryForObject("SELECT resolution_date FROM alerts WHERE id=?", Object.class, id)).isEqualTo(firstDate);
        assertThat(service.list(customer).getFirst().get("status")).isEqualTo("RESOLVED");
        assertThat(stocker().list(responsible())).isEmpty();
        assertThat(db.queryForObject("SELECT product_quantity FROM product_shelves WHERE product_id=1", Integer.class)).isEqualTo(7);
        assertThat(count("restockings")).isZero();
        assertThat(count("purchases")).isZero();
    }

    @Test
    void anotherEmployeeOrCompanyCannotReadOrResolveReportAndCustomerIsForbidden() {
        var confirmation = send(request(UUID.randomUUID(), 1L, "Problem", null));
        long id = ((Number) confirmation.get("alertId")).longValue();
        var foreign = auth("stocker2@invalid", "EMPLOYEE");
        assertThat(stocker().list(foreign)).isEmpty();
        assertStatus(404, () -> tx.execute(status -> stocker().resolve(foreign, id)));
        db.update("INSERT INTO users(id,name,last_name,birth_date,cpf,email,password) VALUES "
                + "(6,'Other','Stocker','2000-01-01','00000000006','same-company@invalid','not-a-login')");
        db.update("INSERT INTO employees(id,users_id,company_id,position) VALUES (3,6,1,'STOCKER')");
        var other = auth("same-company@invalid", "EMPLOYEE");
        assertThat(stocker().list(other)).isEmpty();
        assertStatus(404, () -> tx.execute(status -> stocker().resolve(other, id)));
        assertStatus(403, () -> stocker().list(customer));
        assertStatus(403, () -> tx.execute(status -> stocker().resolve(customer, id)));
        // Even an assigned employee cannot read a report outside their current market.
        db.update("UPDATE employees SET company_id=2 WHERE id=1");
        assertThat(stocker().list(responsible())).isEmpty();
        assertStatus(404, () -> tx.execute(status -> stocker().resolve(responsible(), id)));
        assertThat(service.list(customer).getFirst().get("status")).isEqualTo("OPEN");
    }

    @Test
    void cancelledReportsAndOrdinaryAlertsCannotBeResolvedByReportEndpoint() {
        var confirmation = send(request(UUID.randomUUID(), 1L, "Problem", null));
        long id = ((Number) confirmation.get("alertId")).longValue();
        db.update("UPDATE alerts SET status='CANCELLED' WHERE id=?", id);
        assertStatus(409, () -> tx.execute(status -> stocker().resolve(responsible(), id)));
        assertThat(stocker().list(responsible())).isEmpty();
        assertThat(db.queryForObject("SELECT resolution_date FROM alerts WHERE id=?", Object.class, id)).isNull();
        long ordinary = db.queryForObject("INSERT INTO alerts(stockout_date,employee_id,shelf_id,product_id,description,status) "
                + "VALUES(CURRENT_DATE,1,1,1,'Ordinary alert','OPEN') RETURNING id", Long.class);
        assertStatus(404, () -> tx.execute(status -> stocker().resolve(responsible(), ordinary)));
    }

    @Test
    void aisleAssignmentWorksWithoutDirectShelfAssignment() {
        db.update("DELETE FROM shelf_employees WHERE employee_id=1");
        db.update("INSERT INTO employee_aisles(employee_id,aisle_id) VALUES(1,1)");
        var confirmation = send(request(UUID.randomUUID(), 1L, "Aisle assigned", null));
        assertThat(stocker().list(responsible())).hasSize(1);
        tx.execute(status -> stocker().resolve(responsible(), ((Number) confirmation.get("alertId")).longValue()));
        assertThat(service.list(customer).getFirst().get("status")).isEqualTo("RESOLVED");
        assertThat(db.queryForObject("SELECT product_quantity FROM product_shelves WHERE product_id=1", Integer.class)).isEqualTo(7);
        assertThat(count("restockings")).isZero();
        assertThat(count("purchases")).isZero();
    }

}
