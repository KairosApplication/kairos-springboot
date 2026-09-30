package com.kairos.kairosapipostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SerialIdMappingTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void generatedIdsAndForeignKeysUseIntegerColumns() throws Exception {
        var tables = List.of("aisles", "alerts", "categories", "customers", "employees",
                "employee_aisles", "inventories", "products", "product_inventories",
                "product_shelves", "purchases", "purchase_products", "restockings",
                "sectors", "shelves", "shelf_employees", "users");

        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            for (var table : tables) {
                try (var id = metadata.getColumns(null, null, table.toUpperCase(), "ID")) {
                    assertThat(id.next()).as("%s.id exists", table).isTrue();
                    assertThat(id.getInt("DATA_TYPE")).as("%s.id", table).isEqualTo(Types.INTEGER);
                }
                try (var foreignKeys = metadata.getImportedKeys(null, null, table.toUpperCase())) {
                    while (foreignKeys.next()) {
                        var column = foreignKeys.getString("FKCOLUMN_NAME");
                        try (var fk = metadata.getColumns(null, null, table.toUpperCase(), column)) {
                            assertThat(fk.next()).as("%s.%s exists", table, column).isTrue();
                            assertThat(fk.getInt("DATA_TYPE")).as("%s.%s", table, column)
                                    .isEqualTo(Types.INTEGER);
                        }
                    }
                }
            }
        }
    }
}
