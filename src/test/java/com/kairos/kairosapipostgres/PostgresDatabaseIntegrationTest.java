package com.kairos.kairosapipostgres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class PostgresDatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldUseRealPostgresInsteadOfH2() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getCatalog()).isEqualTo("kairos_test");
        }
    }
}
