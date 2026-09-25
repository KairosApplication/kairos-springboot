package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.AlertRequest;
import com.kairos.kairosapipostgres.dto.response.AlertResponse;
import com.kairos.kairosapipostgres.model.*;

public final class AlertMapper {
    private AlertMapper() {
    }

    public static Alert toEntity(AlertRequest request, Employee employee, Shelf shelf, Product product) {
        return new Alert(null, request.stockoutDate(), employee, shelf, product,
                request.description().trim(), request.status(), request.resolutionDate());
    }

    public static AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getStockoutDate(),
                alert.getEmployee() == null ? null : alert.getEmployee().getId(),
                alert.getShelf().getId(), alert.getProduct() == null ? null : alert.getProduct().getId(),
                alert.getDescription(), alert.getStatus(), alert.getResolutionDate());
    }
}
