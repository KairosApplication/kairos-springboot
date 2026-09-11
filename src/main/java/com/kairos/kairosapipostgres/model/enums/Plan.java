package com.kairos.kairosapipostgres.model.enums;

public enum Plan {
    STANDART("Standart"),
    CORPORATIVO("Corporativo");

    private String value;

    Plan (String valor) {
        this.value = valor;
    }

    public String getValue() {
        return value;
    }
}