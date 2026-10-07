package com.kairos.kairosapipostgres;

import com.kairos.kairosapipostgres.model.Company;
import com.kairos.kairosapipostgres.repository.CompanyRepository;

import java.util.UUID;

public final class TestCompanyFactory {
    private TestCompanyFactory() {
    }

    public static Company create(CompanyRepository repository) {
        String unique = UUID.randomUUID().toString();
        return repository.saveAndFlush(new Company(null, "Test company", unique.substring(0, 18),
                unique + "@example.com", "BASIC"));
    }
}
