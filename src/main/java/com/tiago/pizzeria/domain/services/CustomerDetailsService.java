package com.tiago.pizzeria.domain.services;

import com.tiago.pizzeria.domain.models.Customer;
import com.tiago.pizzeria.domain.repositories.CustomerRepository;
import com.tiago.pizzeria.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService implements UserDetailsService {

    @Autowired private CustomerRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Customer user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return new CustomerPrincipal(user);
    }
}