package com.kairos.kairosapipostgres.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Real PostgreSQL coverage of simultaneous replica startup and lock release. */
@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class PostgresAuditConcurrencyTest {
    @Test
    void shouldSerializeReplicaStartupAndReleaseTransactionLock() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:tc:postgresql:17-alpine:///kairos_audit_concurrency", "test", "test");
        try (var executor = Executors.newFixedThreadPool(2);
             Connection blocker = source.getConnection()) {
                blocker.createStatement().execute("CREATE TABLE products (id bigint PRIMARY KEY)");
                blocker.createStatement().execute("CREATE TABLE purchases (id bigint PRIMARY KEY)");
                blocker.setAutoCommit(false);
                blocker.createStatement().execute("SELECT pg_advisory_xact_lock(1262572114, 1)");
                var initializer = new PostgresAuditInitializer(source);
                var first = executor.submit(() -> { initializer.run(null); return null; });
                var second = executor.submit(() -> { initializer.run(null); return null; });
                // Wait until both real backend sessions are waiting on our lock.
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                int waiting = 0;
                while (waiting < 2 && System.nanoTime() < deadline) {
                    try (var statement = blocker.createStatement();
                         var result = statement.executeQuery("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted")) {
                        result.next();
                        waiting = result.getInt(1);
                    }
                    if (waiting < 2) Thread.sleep(50);
                }
                assertThat(waiting).isEqualTo(2);
                blocker.commit();
                first.get(30, TimeUnit.SECONDS);
                second.get(30, TimeUnit.SECONDS);
                try (var statement = blocker.createStatement();
                     var result = statement.executeQuery("SELECT count(*) FROM pg_trigger WHERE tgname IN ('trg_products_audit', 'trg_purchases_audit')")) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(2);
                }
                try (var statement = blocker.createStatement();
                     var result = statement.executeQuery("SELECT pg_try_advisory_xact_lock(1262572114, 1)")) {
                    result.next();
                    assertThat(result.getBoolean(1)).isTrue();
                }
                blocker.rollback();
        }
    }
}
