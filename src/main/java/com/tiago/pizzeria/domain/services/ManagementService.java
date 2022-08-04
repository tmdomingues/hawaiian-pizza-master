package com.tiago.pizzeria.domain.services;

import com.tiago.pizzeria.domain.models.Customer;
import com.tiago.pizzeria.domain.models.UserRole;
import com.tiago.pizzeria.domain.repositories.CustomerRepository;
import com.tiago.pizzeria.domain.repositories.PurchaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ManagementService {
    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;

    public ManagementService(PurchaseRepository purchaseRepository, CustomerRepository customerRepository) {
        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
    }

    public long getPurchasesCount() {
        Customer currentUser = getCurrentUser();
        if (currentUser.getRoles().stream().noneMatch(userRole -> userRole.equals(UserRole.OWNER))) {
            throw new AccessDeniedException("Access not allowed");
        }
        return purchaseRepository.count();
    }

    private Customer getCurrentUser() {
        //todo change current user
        return customerRepository.findById(66L).orElseThrow(IllegalArgumentException::new);
    }
}
