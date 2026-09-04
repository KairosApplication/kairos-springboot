package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CustomerRequest;
import com.kairos.kairosapipostgres.dto.response.CustomerResponse;
import com.kairos.kairosapipostgres.exception.CustomerAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.CustomerNotFoundException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, userRepository);
    }

    @Test
    void shouldCreateCustomerForExistingUser() {
        User user = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(customerRepository.existsByUserId(10L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CustomerResponse response = service.save(new CustomerRequest(10L));

        assertThat(response).isEqualTo(new CustomerResponse(1L, 10L, "Davi"));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectCustomerWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(new CustomerRequest(99L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(customerRepository, never()).existsByUserId(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldRejectSecondCustomerForSameUser() {
        User user = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(customerRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(new CustomerRequest(10L)))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessage("Customer already exists for this user");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldListCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(
                customer(1L, 10L),
                customer(2L, 20L)
        ));

        List<CustomerResponse> customers = service.findAll();

        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(CustomerResponse::id)
                .containsExactly(1L, 2L);
        assertThat(customers).extracting(CustomerResponse::userId)
                .containsExactly(10L, 20L);
    }

    @Test
    void shouldFindCustomerById() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer(1L, 10L)));

        CustomerResponse response = service.findById(1L);

        assertThat(response).isEqualTo(new CustomerResponse(1L, 10L, "Davi"));
    }

    @Test
    void shouldDeleteExistingCustomer() {
        Customer customer = customer(1L, 10L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        service.deleteById(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void shouldRejectMissingCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void shouldRejectDeletingMissingCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");
        verify(customerRepository, never()).delete(any());
    }

    private Customer customer(Long id, Long userId) {
        return new Customer(id, user(userId));
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
