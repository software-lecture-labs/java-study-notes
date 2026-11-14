package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.shared.kernel.Specification;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public final class JpaOrderRepository implements OrderRepository {

    private static final String ALL_ORDERS_WITH_LINES = """
            SELECT DISTINCT o
              FROM OrderEntity o
              LEFT JOIN FETCH o.lines
             ORDER BY o.orderId
            """;

    private final EntityManager entityManager;

    @Inject
    public JpaOrderRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        entityManager.merge(OrderEntity.from(order));
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return Optional.ofNullable(entityManager.find(OrderEntity.class, orderId.value()))
                .map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return entityManager.createQuery(ALL_ORDERS_WITH_LINES, OrderEntity.class)
                .setHint("eclipselink.read-only", true)
                .getResultStream()
                .map(OrderEntity::toDomain)
                .toList();
    }

    @Override
    public List<Order> findAll(Specification<Order> specification) {
        Objects.requireNonNull(specification, "specification must not be null");
        return findAll().stream()
                .filter(specification::isSatisfiedBy)
                .toList();
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(o) FROM OrderEntity o", Long.class).getSingleResult();
    }
}
