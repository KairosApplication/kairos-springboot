package com.kairos.kairosapipostgres;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class FlywaySchemaIntegrationTest {

    @Test
    void dailyActivityMigrationPreservesPreinstalledKairosDataObjectsAndRows() throws Exception {
        String url = "jdbc:tc:postgresql:17-alpine:///kairos_dau_compat_test";
        try (var connection = DriverManager.getConnection(url, "test", "test")) {
            Flyway.configure().dataSource(url, "test", "test").target("3").load().migrate();
            String legacy = new org.springframework.core.io.ClassPathResource("db/legacy-kairos-data-dau.sql")
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            try (var statement = connection.createStatement()) {
                statement.execute(legacy);
                statement.execute("""
                        INSERT INTO companies(id,name,cnpj,email,plan) VALUES(1,'DEMO','00000000000001','demo@invalid','BASIC');
                        INSERT INTO users(id,name,last_name,birth_date,cpf,email,password)
                        VALUES(1,'DEMO','FICTICIO','2000-01-01','00000000001','user@invalid','not-a-login');
                        INSERT INTO customers(users_id,company_id) VALUES(1,1);
                        SELECT register_user_access(1,1);
                        """);
            }
            java.time.OffsetDateTime first;
            try (var statement = connection.createStatement();
                 var row = statement.executeQuery("SELECT first_seen_at FROM user_accesses")) {
                row.next();
                first = row.getObject(1, java.time.OffsetDateTime.class);
            }
            assertThat(Flyway.configure().dataSource(url, "test", "test").load().migrate().migrationsExecuted).isEqualTo(1);
            try (var statement = connection.createStatement();
                 var row = statement.executeQuery("SELECT first_seen_at FROM user_accesses")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getObject(1, java.time.OffsetDateTime.class)).isEqualTo(first);
                assertThat(row.next()).isFalse();
            }
        }
    }

    @Test
    void migrationsCreateTheCurrentPostgresSchemaFromAnEmptyDatabase() throws Exception {
        String url = "jdbc:tc:postgresql:17-alpine:///kairos_flyway_test";
        try (var connection = DriverManager.getConnection(url, "test", "test")) {
            var flyway = Flyway.configure()
                    .dataSource(url, "test", "test")
                    .locations("classpath:db/migration")
                    .load();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);

            try (var statement = connection.createStatement();
                 var tables = statement.executeQuery("""
                         SELECT count(*) FROM information_schema.tables
                         WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                           AND table_name <> 'flyway_schema_history'
                         """)) {
                tables.next();
                assertThat(tables.getInt(1)).isEqualTo(23);
            }

            try (var statement = connection.createStatement();
                 var columns = statement.executeQuery("""
                         SELECT table_name, column_name, data_type
                         FROM information_schema.columns
                         WHERE table_schema = 'public'
                           AND ((table_name = 'users' AND column_name IN ('id', 'plan', 'zip_code'))
                             OR (table_name = 'products' AND column_name IN ('id', 'price'))
                             OR (table_name = 'purchase_products' AND column_name = 'amount'))
                         ORDER BY table_name, column_name
                         """)) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("table_name")).isEqualTo("products");
                assertThat(columns.getString("column_name")).isEqualTo("id");
                assertThat(columns.getString("data_type")).isEqualTo("integer");
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("column_name")).isEqualTo("price");
                assertThat(columns.getString("data_type")).isEqualTo("money");
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("table_name")).isEqualTo("purchase_products");
                assertThat(columns.getString("column_name")).isEqualTo("amount");
                assertThat(columns.getString("data_type")).isEqualTo("money");
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("table_name")).isEqualTo("users");
                assertThat(columns.getString("column_name")).isEqualTo("id");
                assertThat(columns.getString("data_type")).isEqualTo("integer");
                assertThat(columns.next()).isFalse();
            }
        }
    }
}
