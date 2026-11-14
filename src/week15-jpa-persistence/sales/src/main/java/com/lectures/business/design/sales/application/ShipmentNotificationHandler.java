package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderShipped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.function.Consumer;

@Dependent
public final class ShipmentNotificationHandler implements Consumer<OrderShipped> {

    private final CustomerNotifier notifier;

    @Inject
    public ShipmentNotificationHandler(CustomerNotifier notifier) {
        this.notifier = Objects.requireNonNull(notifier, "notifier must not be null");
    }

    @Override
    public void accept(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderShipped event) {
        notifier.notify(event.customerId().value(),
                "Order " + event.orderId() + " shipped on " + event.shippedDate()
                + " to " + event.destination().city());
    }
}
