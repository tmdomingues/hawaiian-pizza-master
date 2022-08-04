package com.tiago.pizzeria.domain.repositories;

import com.tiago.pizzeria.domain.models.Pizza;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaRepository extends CrudRepository<Pizza, Long>, PizzaRepositoryCustom { }
