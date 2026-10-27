package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RestockingAccess {
    private final EmployeeRepository employees;

    public RestockingAccess(EmployeeRepository employees) {
        this.employees = employees;
    }

    public Employee actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw denied();
        }
        Employee actor = employees.findByUserEmail(authentication.getName()).orElseThrow(RestockingAccess::denied);
        if (actor.getCompany() == null || !(actor.getPosition() == Position.STOCKER || actor.getPosition() == Position.MANAGER)) {
            throw denied();
        }
        return actor;
    }

    public Employee target(Long employeeId) {
        Employee actor = actor();
        if (employeeId == null || employeeId.equals(actor.getId())) {
            return actor;
        }
        Employee target = employees.findById(employeeId).orElseThrow(RestockingAccess::denied);
        if (actor.getPosition() != Position.MANAGER || !sameCompany(target.getCompany(), actor)
                || !(target.getPosition() == Position.STOCKER || target.getPosition() == Position.MANAGER)) {
            throw denied();
        }
        return target;
    }

    public boolean visible(Restocking value) {
        Employee actor = actor();
        return sameCompany(value.getEmployee().getCompany(), actor)
                && sameCompany(value.getInventory().getCompany(), actor)
                && sameCompany(value.getProduct().getCompany(), actor)
                && sameCompany(value.getShelf().getAisle().getSector().getCompany(), actor)
                && (actor.getPosition() == Position.MANAGER || value.getEmployee().getId().equals(actor.getId()));
    }

    public void require(Restocking value) {
        target(value.getEmployee().getId());
        if (!visible(value)) {
            throw denied();
        }
    }

    public void requireCompany(Company company) {
        if (!sameCompany(company, actor())) {
            throw denied();
        }
    }

    private boolean sameCompany(Company company, Employee actor) {
        return company != null && company.getId().equals(actor.getCompany().getId());
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Restocking access requires an authorized employee in the same company");
    }
}
