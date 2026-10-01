package com.kairos.kairosapipostgres;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class FlywaySchemaIntegrationTest {

    @Test
    void migrationsCreateTheCurrentPostgresSchemaFromAnEmptyDatabase() throws Exception {
        String url = "jdbc:tc:postgresql:17-alpine:///kairos_flyway_test";
        try (var connection = DriverManager.getConnection(url, "test", "test")) {
            var flyway = Flyway.configure()
                    .dataSource(url, "test", "test")
                    .locations("classpath:db/migration")
                    .load();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);

            try (var statement = connection.createStatement();
                 var tables = statement.executeQuery("""
                         SELECT count(*) FROM information_schema.tables
                         WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                         """)) {
                tables.next();
                assertThat(tables.getInt(1)).isEqualTo(20);
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
