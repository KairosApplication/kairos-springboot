package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.Company;

public final class CustomerMapper {

    private CustomerMapper () {
    }

    public static Customer toEntity(CustomerRequest request, User user, Company company) {
        Customer customer = new Customer ();
        customer.setUser(user);
        customer.setCompany(company);
        return customer;
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUser().getId(),
                customer.getUser().getName()
        );
    }
}
