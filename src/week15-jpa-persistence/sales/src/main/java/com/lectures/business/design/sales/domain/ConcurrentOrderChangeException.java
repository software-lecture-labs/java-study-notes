package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.DomainException;

public class ConcurrentOrderChangeException extends DomainException {

    private final OrderId orderId;

    public ConcurrentOrderChangeException(OrderId orderId) {
        super("order " + orderId + " was changed by someone else; read it again and retry", Kind.CONFLICT);
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
