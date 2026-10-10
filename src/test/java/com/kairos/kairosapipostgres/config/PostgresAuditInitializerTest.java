package com.kairos.kairosapipostgres.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresAuditInitializerTest {
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData metadata;
    @Mock private Statement statement;

    private PostgresAuditInitializer initializer;

    @BeforeEach
    void setUp() throws SQLException {
        initializer = new PostgresAuditInitializer(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
    }

    @Test
    void shouldInstallAuditFunctionAndBothTriggersOnPostgres() throws Exception {
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);

        initializer.run(null);

        ArgumentCaptor<String> scripts = ArgumentCaptor.forClass(String.class);
        verify(statement, times(5)).execute(scripts.capture());
        assertThat(scripts.getAllValues().get(0)).isEqualTo("SELECT pg_advisory_xact_lock(1262572114, 1)");
        assertThat(scripts.getAllValues().get(1)).contains("CREATE TABLE IF NOT EXISTS audits");
        assertThat(scripts.getAllValues().get(2)).contains("CREATE OR REPLACE FUNCTION register_audit()");
        assertThat(scripts.getAllValues().get(3)).contains("trg_products_audit");
        assertThat(scripts.getAllValues().get(4)).contains("trg_purchases_audit");
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void shouldSkipDatabaseWithoutPostgresSupport() throws Exception {
        when(metadata.getDatabaseProductName()).thenReturn("H2");

        initializer.run(null);

        verify(connection, never()).createStatement();
    }

    @Test
    void shouldRollBackWhenInstallationFails() throws Exception {
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("Missing permission"));

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(SQLException.class)
                .hasMessage("Missing permission");
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
    }
}
