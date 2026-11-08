package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrderActivityLogTest {

    @Test
    @DisplayName("the log describes every kind of order event")
    void describesEachEvent() {
        assertThat(OrderActivityLog.describe(
                new OrderConfirmed(OrderId.of(10248), CustomerId.of("VINET"), Money.tl("140.00"))))
                .isEqualTo("10248 confirmed, total 140.00 TRY");
        assertThat(OrderActivityLog.describe(
                new OrderCancelled(OrderId.of(10248), CustomerId.of("VINET"), OrderStatus.CONFIRMED)))
                .isEqualTo("10248 cancelled while CONFIRMED");
    }
}
