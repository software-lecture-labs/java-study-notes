package com.lectures.business.design.sales.api;

public interface OrderingFacade {

    OrderSummary placeOrder(PlaceOrderCommand command);

    OrderSummary confirm(int orderId);

    OrderSummary ship(int orderId);

    OrderSummary cancel(int orderId);

    OrderSummary getOrder(int orderId);
}
