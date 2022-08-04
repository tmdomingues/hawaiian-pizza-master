package com.tiago.pizzeria.domain.controllers;

import com.tiago.pizzeria.domain.models.Pizza;
import com.tiago.pizzeria.domain.repositories.PizzaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PizzaController {

	@Autowired
    PizzaRepository repo;

	@PostMapping("/pizza")
	public void createPizza(@RequestBody Pizza pizza){
		repo.save(pizza);
	}

	@GetMapping("/pizza")
	public Iterable<Pizza> getPizzas(@RequestParam(value = "topping", required = false) String topping){
		if (topping==null)
			return repo.findAll();
		else
			return repo.getPizzaContainingTopping(topping);
	}
}
