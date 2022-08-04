package com.tiago.pizzeria.domain.models;

import com.tiago.pizzeria.domain.converters.RoleConverter;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    private String name;

    @NotNull
    private String username;

    @NotNull
    private String password;

    @NotNull
    private String email;

    @NotNull
    @Convert(converter = RoleConverter.class)
    @Column(length = 10485760)
    private List<UserRole> roles;

}

