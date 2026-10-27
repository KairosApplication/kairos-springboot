package com.kairos.kairosapipostgres.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kairos.kairosapipostgres.model.Restocking;
import java.time.OffsetDateTime;

public record RestockingCacheEvent(
        @JsonProperty("restocking_id") long restockingId,
        @JsonProperty("company_id") long companyId,
        @JsonProperty("employee_id") long employeeId,
        @JsonProperty("product_id") long productId,
        @JsonProperty("shelf_id") long shelfId,
        @JsonProperty("inventory_id") long inventoryId,
        @JsonProperty("restocked_quantity") int restockedQuantity,
        @JsonProperty("qnt_avarias") int damagedQuantity,
        @JsonProperty("restocking_date") OffsetDateTime restockingDate,
        @JsonProperty("product_name") String productName) {
    public static RestockingCacheEvent from(Restocking value) {
        return new RestockingCacheEvent(value.getId(), value.getEmployee().getCompany().getId(),
                value.getEmployee().getId(), value.getProduct().getId(), value.getShelf().getId(),
                value.getInventory().getId(), value.getRestockedQuantity(), value.getDamagedQuantity(),
                value.getRestockingDate().atZone(com.kairos.kairosapipostgres.service.RestockingWeek.ZONE).toOffsetDateTime(),
                value.getProduct().getName());
    }
}
