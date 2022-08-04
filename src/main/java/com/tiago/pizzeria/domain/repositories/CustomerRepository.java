package com.tiago.pizzeria.domain.repositories;

import com.tiago.pizzeria.domain.models.Customer;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

    Customer findByUsername(String username);
}
