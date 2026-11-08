package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderShipped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@Dependent
public final class OrderActivityLog {

    private static final System.Logger LOG = System.getLogger(OrderActivityLog.class.getName());

    void record(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderEvent event) {
        LOG.log(System.Logger.Level.INFO, describe(event));
    }

    static String describe(OrderEvent event) {
        return switch (event) {
            case OrderConfirmed confirmed ->
                confirmed.orderId() + " confirmed, total " + confirmed.total();
            case OrderShipped shipped ->
                shipped.orderId() + " shipped on " + shipped.shippedDate();
            case OrderCancelled cancelled ->
                cancelled.orderId() + " cancelled while " + cancelled.previousStatus();
        };
    }
}
