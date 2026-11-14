package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Objects;

@Dependent
public final class SequenceOrderIdGenerator implements OrderIdGenerator {

    private final EntityManager entityManager;

    @Inject
    public SequenceOrderIdGenerator(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    @Override
    public OrderId next() {
        Number next = (Number) entityManager.createNativeQuery("SELECT nextval('orders_seq')").getSingleResult();
        return OrderId.of(next.intValue());
    }
}
