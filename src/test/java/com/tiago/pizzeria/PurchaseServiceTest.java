package com.tiago.pizzeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiago.pizzeria.domain.models.Pizza;
import com.tiago.pizzeria.domain.models.Customer;
import com.tiago.pizzeria.domain.models.Purchase;
import com.tiago.pizzeria.domain.models.PurchaseState;
import com.tiago.pizzeria.domain.repositories.CustomerRepository;
import com.tiago.pizzeria.domain.repositories.PurchaseRepository;
import com.tiago.pizzeria.security.CustomerPrincipal;
import com.tiago.pizzeria.domain.services.PizzeriaException;
import com.tiago.pizzeria.domain.services.PurchaseService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class PurchaseServiceTest {

    @Mock
    private CustomerRepository userRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    private Customer currentUser;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(purchaseRepository, userRepository);
        currentUser = new Customer();
        currentUser.setName("Papa");
        currentUser.setId(666L);
        currentUser.setRoles(Collections.emptyList());
        currentUser.setEmail("abc@def.com");

        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        CustomerPrincipal userPrincipal = new CustomerPrincipal(currentUser);
        Mockito.when(authentication.getPrincipal()).thenReturn(userPrincipal);
    }

    @Test
    void should_create_draft_purchase_if_non_existing() {

        purchaseService.addPizzaToPurchase(new Pizza());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.DRAFT);
    }

    @Test
    void should_throw_exception_if_more_purchases() {
        when(purchaseRepository.findAllByStateEqualsAndCustomer_Id(any(), any()))
                .thenReturn(Arrays.asList(new Purchase(), new Purchase()));

        assertThrows(PizzeriaException.class, () -> purchaseService.addPizzaToPurchase(new Pizza()));
    }

    @Test
    void confirm_purchase_changes_state() {
        when(purchaseRepository.findAllByStateEqualsAndCustomer_Id(any(), any()))
                .thenReturn(Collections.singletonList(new Purchase()));

        purchaseService.confirmPurchase();

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.PLACED);
    }

    @Test
    void should_add_items_to_purchase() {
        Pizza one = new Pizza();
        one.setId(666L);
        Pizza two = new Pizza();
        two.setId(43L);
        purchaseService.addPizzaToPurchase(one);
        purchaseService.addPizzaToPurchase(two);
        Purchase toReturn = new Purchase();
        toReturn.setPizzas(Arrays.asList(one, two));
        when(purchaseRepository.findAllByStateEqualsAndCustomer_Id(any(), any()))
                .thenReturn(Collections.singletonList(new Purchase()));
        when(purchaseRepository.findFirstByStateEquals(any())).thenReturn(new Purchase());
        when(purchaseRepository.save(any())).thenReturn(toReturn);

        purchaseService.confirmPurchase();

        Purchase latest = purchaseService.pickPurchase();
        assertThat(latest.getPizzas()).containsExactlyInAnyOrder(one, two);
    }

    @Test
    void confirm_pick_changes_state() {
        when(purchaseRepository.findFirstByStateEquals(any()))
                .thenReturn(new Purchase());

        purchaseService.pickPurchase();

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.ONGOING);
    }

    @Test
    void confirm_close_changes_state() {
        final Purchase p = new Purchase();
        p.setState(PurchaseState.ONGOING);
        when(purchaseRepository.findFirstByStateEquals(any()))
                .thenReturn(p);
        when(purchaseRepository.findById(any()))
                .thenReturn(Optional.of(p));
        purchaseService.pickPurchase();

        purchaseService.completePurchase(p.getId());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.SERVED);
    }

    @Test
    void apply_10_percent_rule_when_pineapple_pizza_exists() {
        final double EXPECTED_PROMOTION_PRICE = 24.6; // 12 + 14 -> apply 10% on 14 = 12 + 12.6
        final Purchase p = new Purchase();
        p.setState(PurchaseState.ONGOING);
        p.setPizzas(Arrays.asList(getHawaiianPizza(), getPepperoniPizza()));
        when(purchaseRepository.findFirstByStateEquals(any()))
                .thenReturn(p);
        when(purchaseRepository.findById(any()))
                .thenReturn(Optional.of(p));
        purchaseService.pickPurchase();

        purchaseService.completePurchase(p.getId());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.SERVED);
        assertThat(p.getAmount()).isEqualTo(EXPECTED_PROMOTION_PRICE);
    }

    @Test
    void apply_cheapest_pizza_free_rule_on_order_with_3_pizzas() {
        final double EXPECTED_PROMOTION_PRICE = 28.0; // 11 + 14 + 14 -> cheapest = 11
        final Purchase p = new Purchase();
        p.setState(PurchaseState.ONGOING);

        p.setPizzas(Arrays.asList(getDiavolaPizza(), getPepperoniPizza(), getPepperoniPizza()));
        when(purchaseRepository.findFirstByStateEquals(any()))
                .thenReturn(p);
        when(purchaseRepository.findById(any()))
                .thenReturn(Optional.of(p));
        purchaseService.pickPurchase();

        purchaseService.completePurchase(p.getId());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.SERVED);
        assertThat(p.getAmount()).isEqualTo(EXPECTED_PROMOTION_PRICE);
    }

    @Test
    void apply_cheapest_pizza_free_rule_on_order_with_3_pizzas_and_pineapple_rule() {
        final double EXPECTED_PROMOTION_PRICE = 24.6;
        final Purchase p = new Purchase();
        p.setState(PurchaseState.ONGOING);

        p.setPizzas(Arrays.asList(getHawaiianPizza(), getPepperoniPizza(), getMargheritaPizza()));
        when(purchaseRepository.findFirstByStateEquals(any()))
                .thenReturn(p);
        when(purchaseRepository.findById(any()))
                .thenReturn(Optional.of(p));
        purchaseService.pickPurchase();

        purchaseService.completePurchase(p.getId());

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(purchaseCaptor.capture());
        Purchase saved = purchaseCaptor.getValue();
        assertThat(saved.getState()).isEqualByComparingTo(PurchaseState.SERVED);
        assertThat(p.getAmount()).isEqualTo(EXPECTED_PROMOTION_PRICE);
    }


    // Pizza fixtures to help testing...should be on a dedicated PizzaFixtures class, simplified for this challenge.
    private Pizza getPepperoniPizza() {
        Pizza pizza = new Pizza();
        pizza.setName("Pepperoni");
        pizza.setPrice(14.0);
        pizza.setToppings(Arrays.asList("tomato", "mozzarela", "pepperoni"));
        return pizza;
    }

    private Pizza getHawaiianPizza() {
        Pizza pizza = new Pizza();
        pizza.setName("Hawaii");
        pizza.setPrice(12.0);
        pizza.setToppings(Arrays.asList("pineapple", "cheese", "ham"));
        return pizza;
    }

    private Pizza getMargheritaPizza() {
        Pizza pizza = new Pizza();
        pizza.setName("Margherita");
        pizza.setPrice(13.0);
        pizza.setToppings(Arrays.asList("mozzarela", "tomato"));
        return pizza;
    }

    private Pizza getDiavolaPizza() {
        Pizza pizza = new Pizza();
        pizza.setName("Diavola");
        pizza.setPrice(11.0);
        pizza.setToppings(Arrays.asList("mozzarela", "tomato", "spicy salami"));
        return pizza;
    }
}
