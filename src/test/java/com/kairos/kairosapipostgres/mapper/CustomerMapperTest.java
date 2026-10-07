package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.Company;
import com.kairos.kairosapipostgres.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    @Test
    void shouldMapRequestAndUserToEntity() {
        User user = user(10L);
        CustomerRequest request = new CustomerRequest(10L, 1L);
        Company company = new Company(1L, "Test", "12345678000199", "company@example.com", "BASIC");

        Customer customer = CustomerMapper.toEntity(request, user, company);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getUser()).isSameAs(user);
        assertThat(customer.getCompany()).isSameAs(company);
    }

    @Test
    void shouldMapEntityToResponse() {
        Customer customer = new Customer(7L, user(10L), null);

        CustomerResponse response = CustomerMapper.toResponse(customer);

        assertThat(response).isEqualTo(new CustomerResponse(
                7L,
                10L,
                "Davi"
        ));
    }

    private User user(Long id) {
        return new User(
                id,
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "52998224725",
                "dias@example.com",
                "encoded-password"
        );
    }
}
