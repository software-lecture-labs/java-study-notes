package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import com.lectures.business.design.sales.application.DomainEventPublisher;
import com.lectures.business.design.sales.application.InProcessEventPublisher;
import com.lectures.business.design.sales.application.OrderActivityLog;
import com.lectures.business.design.sales.application.ShipmentNotificationHandler;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import com.lectures.business.design.sales.domain.OrderShipped;
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import javax.sql.DataSource;

@DataSourceDefinition(
        name = "java:app/jdbc/sales",
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "${MPCONFIG=sales.datasource.url}",
        user = "${MPCONFIG=sales.datasource.user}",
        password = "${MPCONFIG=sales.datasource.password}")
@ApplicationScoped
public class SalesWiring {

    @Produces
    @Resource(lookup = "java:app/jdbc/sales")
    private DataSource dataSource;

    @Produces
    Clock clock() {
        return Clock.systemUTC();
    }

    @Produces
    OrderFactory orderFactory(OrderIdGenerator ids, Clock clock) {
        return new OrderFactory(ids, clock);
    }

    // Replaced by CDI events in the next step
    @Produces
    @ApplicationScoped
    DomainEventPublisher eventPublisher(CustomerNotifier notifier) {
        return new InProcessEventPublisher()
                .subscribe(OrderEvent.class, new OrderActivityLog())
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(notifier));
    }
}
