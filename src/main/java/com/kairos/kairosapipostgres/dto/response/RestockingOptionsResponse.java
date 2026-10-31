package com.kairos.kairosapipostgres.dto.response;

import java.util.List;

public record RestockingOptionsResponse(List<EmployeeResponse> people, List<InventoryResponse> inventories) {
}
