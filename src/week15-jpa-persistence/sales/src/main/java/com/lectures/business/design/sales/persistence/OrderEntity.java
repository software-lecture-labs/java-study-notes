package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderLine;
import com.lectures.business.design.sales.domain.OrderStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "customer_id", nullable = false, length = 5)
    private String customerId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status;

    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "ship_street"))
    @AttributeOverride(name = "city", column = @Column(name = "ship_city"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code"))
    @AttributeOverride(name = "country", column = @Column(name = "ship_country"))
    private AddressColumns shippingAddress;

    @Column(name = "shipped_date")
    private LocalDate shippedDate;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ElementCollection
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderBy("lineNo")
    private List<OrderLineColumns> lines = new ArrayList<>();

    protected OrderEntity() {
    }

    static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.orderId = order.orderId().value();
        entity.customerId = order.customerId().value();
        entity.orderDate = order.orderDate();
        entity.status = order.status();
        entity.shippingAddress = order.shippingAddress().map(AddressColumns::from).orElse(null);
        entity.shippedDate = order.shippedDate().orElse(null);
        entity.version = order.version();
        int lineNo = 1;
        for (OrderLine line : order.lines()) {
            entity.lines.add(OrderLineColumns.from(lineNo++, line));
        }
        return entity;
    }

    Order toDomain() {
        return Order.reconstitute(
                OrderId.of(orderId),
                CustomerId.of(customerId),
                orderDate,
                status,
                shippingAddress == null ? null : shippingAddress.toAddress(),
                shippedDate,
                lines.stream().map(OrderLineColumns::toOrderLine).toList(),
                version);
    }
}
