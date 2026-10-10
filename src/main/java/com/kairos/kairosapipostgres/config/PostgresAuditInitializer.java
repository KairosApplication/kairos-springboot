package com.kairos.kairosapipostgres.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class PostgresAuditInitializer implements ApplicationRunner {
    private static final String[] AUDIT_SCRIPTS = {
            "db/postgres/audit-table.sql",
            "db/postgres/audit-function.sql",
            "db/postgres/audit-products-trigger.sql",
            "db/postgres/audit-purchases-trigger.sql"
    };

    private final DataSource dataSource;

    public PostgresAuditInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                // Serialize startup DDL across replicas; PostgreSQL releases this lock
                // on commit or rollback. Never use a session lock with pooled connections.
                statement.execute("SELECT pg_advisory_xact_lock(1262572114, 1)");
                for (String script : AUDIT_SCRIPTS) {
                    String sql = new ClassPathResource(script).getContentAsString(StandardCharsets.UTF_8);
                    statement.execute(sql);
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }
}
