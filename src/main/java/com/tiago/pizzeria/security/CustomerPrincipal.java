package com.tiago.pizzeria.security;

import com.tiago.pizzeria.domain.models.Customer;
import com.tiago.pizzeria.domain.models.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomerPrincipal implements UserDetails {

    private Customer user;

    public CustomerPrincipal(Customer user) {
        this.user=user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            for (UserRole role: this.user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.name()));
            }
            return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Customer getUser() {
        return user;
    }
}
