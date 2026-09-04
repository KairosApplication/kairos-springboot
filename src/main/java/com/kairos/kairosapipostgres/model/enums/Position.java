package com.kairos.kairosapipostgres.model.enums;

public enum Position {
    MANAGER("manager"),
    STOCKER("stocker"),
    CASHIER("cashier");

    private final String value;

    Position(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
