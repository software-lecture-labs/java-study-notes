package com.lectures.business.design.sales.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

public final class JpaUnit {

    private final EntityManager entityManager;

    private JpaUnit(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static JpaUnit on(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("sales", Map.of(
                "jakarta.persistence.transactionType", "RESOURCE_LOCAL",
                "jakarta.persistence.jtaDataSource", "",
                "jakarta.persistence.nonJtaDataSource", dataSource,
                "eclipselink.weaving", "false",
                "eclipselink.logging.level", "WARNING"));
        return new JpaUnit(factory.createEntityManager());
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public <T> T transactional(Class<T> contract, T target) {
        Objects.requireNonNull(target, "target must not be null");
        return contract.cast(Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract},
                (proxy, method, args) -> {
                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    try {
                        Object result = method.invoke(target, args);
                        transaction.commit();
                        return result;
                    } catch (InvocationTargetException e) {
                        transaction.rollback();
                        throw e.getCause();
                    } finally {
                        entityManager.clear();
                    }
                }));
    }
}
