package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    @Test
    void shouldMapRequestAndUserToEntity() {
        User user = user(10L);
        CustomerRequest request = new CustomerRequest(10L);

        Customer customer = CustomerMapper.toEntity(request, user);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getUser()).isSameAs(user);
    }

    @Test
    void shouldMapEntityToResponse() {
        Customer customer = new Customer(7L, user(10L));

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
                "encoded-password",
                "01310-100",
                Plan.STANDART
        );
    }
}
