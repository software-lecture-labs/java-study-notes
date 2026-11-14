package com.lectures.business.design.sales.persistence;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class LocalTransactions {

    private final DataSource target;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public LocalTransactions(DataSource target) {
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    public DataSource dataSource() {
        return proxy(DataSource.class, (method, args) -> {
            Connection bound = current.get();
            if (bound != null && method.getName().equals("getConnection")) {
                return shared(bound);
            }
            return method.invoke(target, args);
        });
    }

    public void inTransaction(Runnable work) {
        Objects.requireNonNull(work, "work must not be null");
        try (Connection connection = target.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                work.run();
                connection.commit();
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("the test transaction failed", e);
        }
    }

    private static Connection shared(Connection connection) {
        return proxy(Connection.class, (method, args) -> method.getName().equals("close")
                ? null
                : method.invoke(connection, args));
    }

    @FunctionalInterface
    private interface Handler {

        Object handle(Method method, Object[] args) throws Exception;
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    try {
                        return handler.handle(method, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }));
    }
}
