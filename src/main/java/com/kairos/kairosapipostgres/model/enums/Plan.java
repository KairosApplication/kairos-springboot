package com.kairos.kairosapipostgres.model.enums;

public enum Plan {
    STANDART("Standart"),
    CORPORATIVO("Corporativo");

    private String valor;

    Plan (String valor) {
        this.valor = valor;
    }
}