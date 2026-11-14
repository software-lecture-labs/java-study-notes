package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.OrderLine;
import com.lectures.business.design.shared.kernel.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

@Embeddable
public class OrderLineColumns {

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "discount", nullable = false, precision = 3, scale = 2)
    private BigDecimal discount;

    protected OrderLineColumns() {
    }

    static OrderLineColumns from(int lineNo, OrderLine line) {
        OrderLineColumns columns = new OrderLineColumns();
        columns.lineNo = lineNo;
        columns.productId = line.productId();
        columns.unitPrice = line.unitPrice().amount();
        columns.currency = line.unitPrice().currency().getCurrencyCode();
        columns.quantity = line.quantity();
        columns.discount = line.discount();
        return columns;
    }

    OrderLine toOrderLine() {
        return new OrderLine(productId, new Money(unitPrice, Currency.getInstance(currency)), quantity, discount);
    }
}
