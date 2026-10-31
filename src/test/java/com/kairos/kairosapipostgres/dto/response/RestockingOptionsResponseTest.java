package com.kairos.kairosapipostgres.dto.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestockingOptionsResponseTest {
    @Test
    void optionsCannotBeChangedThroughInputListsOrResponseAccessors() {
        EmployeeResponse employee = new EmployeeResponse(1L, "stocker", 2L, "Pessoa de teste");
        InventoryResponse inventory = new InventoryResponse(3L, 4L, "Setor de teste");
        var people = new ArrayList<>(List.of(employee));
        var inventories = new ArrayList<>(List.of(inventory));
        var response = new RestockingOptionsResponse(people, inventories);

        people.clear();
        inventories.clear();

        assertThat(response.people()).containsExactly(employee);
        assertThat(response.inventories()).containsExactly(inventory);
        assertThatThrownBy(() -> response.people().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.inventories().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesEmptyOptions() {
        var response = new RestockingOptionsResponse(List.of(), List.of());
        assertThat(response.people()).isEmpty();
        assertThat(response.inventories()).isEmpty();
    }
}
