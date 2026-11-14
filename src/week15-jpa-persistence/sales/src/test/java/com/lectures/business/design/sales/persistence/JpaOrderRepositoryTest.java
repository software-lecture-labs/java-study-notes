package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.OrderBuilder;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import jakarta.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaOrderRepositoryTest extends OrderRepositoryContract {

    private DataSource dataSource;
    private JpaUnit unit;

    @Override
    protected OrderRepository createRepository() {
        dataSource = TestDatabases.withDemoCustomers(TestDatabases.h2());
        unit = JpaUnit.on(dataSource);
        return unit.transactional(OrderRepository.class, new JpaOrderRepository(unit.entityManager()));
    }

    @Test
    @DisplayName("the foreign key refuses an order for a customer that does not exist, at commit")
    void unknownCustomerIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> repository.save(OrderBuilder.anOrder()
                .withId(10300).forCustomer("ALFKI").withLine(11, "14.00", 1).build()))
                .isInstanceOf(PersistenceException.class)
                .hasRootCauseInstanceOf(SQLException.class);
        assertThat(repository.findById(OrderId.of(10300))).isEmpty();
    }

    @Test
    @DisplayName("a commit that fails leaves no half-written aggregate behind")
    void failedCommitWritesNothing() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE order_lines ADD CONSTRAINT ck_sabotage CHECK (quantity <> 7)");
        }

        assertThatThrownBy(() -> repository.save(OrderBuilder.anOrder()
                .withId(10301).forCustomer("VINET").withLine(11, "14.00", 7).build()))
                .isInstanceOf(PersistenceException.class);
        assertThat(repository.findById(OrderId.of(10301))).isEmpty();
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("findAll reads each order once, with all of its lines")
    void findAllJoinsTheLines() {
        repository.save(OrderBuilder.anOrder()
                .withId(10302).forCustomer("VINET").withLine(11, "14.00", 2).withLine(42, "9.80", 3).build());

        assertThat(repository.findAll())
                .filteredOn(order -> order.orderId().value() == 10302)
                .singleElement()
                .satisfies(order -> assertThat(order.productIds()).containsExactly(11, 42));
    }

    @Test
    @DisplayName("ids come from the database sequence and survive a restart")
    void sequenceHandsOutIds() {
        OrderIdGenerator first = unit.transactional(OrderIdGenerator.class,
                new SequenceOrderIdGenerator(unit.entityManager()));
        OrderIdGenerator afterRestart = unit.transactional(OrderIdGenerator.class,
                new SequenceOrderIdGenerator(JpaUnit.on(dataSource).entityManager()));
        assertThat(first.next().value()).isEqualTo(10248);
        assertThat(afterRestart.next().value()).isEqualTo(10249);
    }
}
