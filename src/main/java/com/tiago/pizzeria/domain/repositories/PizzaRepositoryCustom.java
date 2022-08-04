package com.tiago.pizzeria.domain.repositories;

import com.tiago.pizzeria.domain.models.Pizza;

import java.util.List;

public interface PizzaRepositoryCustom {

	List<Pizza> getPizzaContainingTopping(String topping);

}
