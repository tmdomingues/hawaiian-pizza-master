package com.tiago.pizzeria.domain.repositories;

import com.tiago.pizzeria.domain.models.Purchase;
import com.tiago.pizzeria.domain.models.PurchaseState;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface PurchaseRepository extends CrudRepository<Purchase, Long> {

	Purchase findFirstByStateEquals(PurchaseState state);

	List<Purchase> findAllByStateEqualsAndCustomer_Id(PurchaseState state, Long customerId);
}
