package com.kairos.kairosapipostgres.exception;

public class SectorNotFoundException extends RuntimeException {
    public SectorNotFoundException (String s) {
        super (s);
    }
}
