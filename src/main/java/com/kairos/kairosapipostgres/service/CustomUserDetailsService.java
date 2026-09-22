package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import com.kairos.kairosapipostgres.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        List<String> roles = new ArrayList<>();

        if (customerRepository.existsByUserId(user.getId())) {
            roles.add("CUSTOMER");
        }

        employeeRepository.findByUserId(user.getId())
                .ifPresent(employee -> {
                    if (employee.getPosition() == Position.MANAGER) {
                        roles.add("MANAGER");
                    } else {
                        roles.add("EMPLOYEE");
                    }
                });

        if (roles.isEmpty()) {
            throw new UsernameNotFoundException(
                    "User has no access profile"
            );
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(roles.toArray(new String[0]))
                .build();
    }
}