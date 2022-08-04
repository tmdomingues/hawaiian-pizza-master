package com.tiago.pizzeria.domain.services;

import com.tiago.pizzeria.domain.models.Pizza;
import com.tiago.pizzeria.domain.models.Customer;
import com.tiago.pizzeria.domain.models.Purchase;
import com.tiago.pizzeria.domain.models.PurchaseState;
import com.tiago.pizzeria.domain.repositories.CustomerRepository;
import com.tiago.pizzeria.domain.repositories.PurchaseRepository;
import com.tiago.pizzeria.security.CustomerPrincipal;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Precision;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {

    //cache for the ongoing order
    private final Map<Customer, Purchase> ongoingPurchases = new HashMap<>();

    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;

    public PurchaseService(PurchaseRepository purchaseRepository, CustomerRepository customerRepository) {
        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
    }

    @PreAuthorize("hasAuthority('ADD_PIZZA')")
    @Transactional
    public Purchase addPizzaToPurchase(Pizza pizza) {
        Customer currentUser = getCurrentUser();

        List<Purchase> purchases = purchaseRepository.findAllByStateEqualsAndCustomer_Id(PurchaseState.DRAFT, currentUser.getId());
        if (purchases.size() > 1) {
            throw new PizzeriaException();
        }
        Purchase purchase;
        if (purchases.isEmpty()) {
            purchase = new Purchase();
            purchase.setCustomer(currentUser);
            purchase.setState(PurchaseState.DRAFT);
        } else {
            purchase = purchases.get(0);
        }
        if (purchase.getPizzas() == null) {
            purchase.setPizzas(new LinkedList<>());
        }
        purchase.setCreationDate(new Date());
        purchase.getPizzas().add(pizza);
        purchaseRepository.save(purchase);
        return purchase;
    }

    @PreAuthorize("hasAuthority('CONFIRM_PURCHASE')")
    public void confirmPurchase() {
        Customer currentUser = getCurrentUser();
        List<Purchase> purchases = purchaseRepository.findAllByStateEqualsAndCustomer_Id(PurchaseState.DRAFT, currentUser.getId());
        if (purchases.size() != 1) {
            throw new PizzeriaException();
        }
        Purchase purchase = purchases.get(0);
        purchase.setState(PurchaseState.PLACED);
        purchaseRepository.save(purchase);
    }

    @PreAuthorize("hasAuthority('PICK_PURCHASE')")
    public Purchase pickPurchase() {
        Customer currentUser = getCurrentUser();
        Purchase purchase = purchaseRepository.findFirstByStateEquals(PurchaseState.PLACED);
        purchase.setWorker(currentUser);
        purchase.setState(PurchaseState.ONGOING);
        //can work only on a single order!
        if (ongoingPurchases.containsKey(currentUser)) {
            throw new PizzeriaException();
        }
        ongoingPurchases.put(currentUser, purchase);
        return purchaseRepository.save(purchase);
    }

    @PreAuthorize("hasRole('PIZZA_MAKER')")
    public void completePurchase(long id) {
        Customer currentUser = getCurrentUser();

        Purchase purchase = purchaseRepository.findById(id).orElseThrow(PizzeriaException::new);

        if (!purchase.getState().equals(PurchaseState.ONGOING)) {
            throw new PizzeriaException();
        }
        if (ongoingPurchases.get(currentUser).getId() != purchase.getId()) {
            throw new PizzeriaException();
        }
        purchase.setCheckoutDate(new Date());
        purchase.setState(PurchaseState.SERVED);
        purchase.setAmount(computeAmount(purchase.getPizzas()));
        purchaseRepository.save(purchase);
        ongoingPurchases.remove(currentUser);

        try {
            new EmailService().sendConfirmationEmail(currentUser, purchase);
        } catch (Exception e) {
        }
    }

    private Double computeAmount(List<Pizza> pizzas) {
        double totalPrice = 0;
        if (pizzas == null) {
            return 0.0;
        }
        // buy a pineapple pizza, get 10% off the others
        boolean applyPineappleDiscount = false;
        for (Pizza pizza : pizzas) {
            if (pizza.getToppings().contains("pineapple")) {
                applyPineappleDiscount = true;
            }
        }

        double cheapestPricedPizza = Double.MAX_VALUE;

        for (Pizza pizza : pizzas) {
            if (pizza.getToppings().contains("pineapple")) {
                totalPrice += pizza.getPrice();
                cheapestPricedPizza = computeCheapestOfThreeRule(pizza.getPrice(), cheapestPricedPizza, pizzas.size());
            } else {
                if (applyPineappleDiscount) {
                    totalPrice += pizza.getPrice() * 0.9;
                    cheapestPricedPizza = computeCheapestOfThreeRule(pizza.getPrice() * 0.9,
                            cheapestPricedPizza, pizzas.size());
                } else {
                    totalPrice += pizza.getPrice();
                    cheapestPricedPizza = computeCheapestOfThreeRule(pizza.getPrice(), cheapestPricedPizza, pizzas.size());
                }
            }
        }

        if (pizzas.size() == 3) {
            totalPrice -= cheapestPricedPizza;
        }

        return Precision.round(totalPrice, 2);
    }

    // buy 3 pizzas, get cheapest one for free ("cheapest" assumes other promotions applied, e.g. 10% rule)
    private double computeCheapestOfThreeRule(double priceToEvaluate, double cheapestPizzaPrice, int sizeOfOrder) {
        if (sizeOfOrder == 3 && priceToEvaluate < cheapestPizzaPrice) {
            cheapestPizzaPrice = priceToEvaluate;
        }
        return cheapestPizzaPrice;
    }

    @PreAuthorize("hasRole('PIZZA_MAKER')")
    public Purchase getCurrentPurchase() {
        return ongoingPurchases.get(getCurrentUser());
    }

    private Customer getCurrentUser() {
        return ((CustomerPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }
}
