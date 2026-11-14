package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.DomainException;
import com.lectures.business.design.shared.kernel.DomainException.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionKindTest {

    @Test
    @DisplayName("every refusal declares its kind and a stable code")
    void kindsAndCodes() {
        OrderId id = OrderId.of(10248);
        assertKind(new OrderNotFoundException(id), Kind.NOT_FOUND, "order-not-found");
        assertKind(new CustomerNotFoundException(CustomerId.of("ALFKI")), Kind.NOT_FOUND, "customer-not-found");
        assertKind(new InvalidStateTransitionException(id, OrderStatus.DRAFT, OrderStatus.SHIPPED),
                Kind.CONFLICT, "invalid-state-transition");
        assertKind(new OrderNotModifiableException(id, OrderStatus.SHIPPED, "add a line"),
                Kind.CONFLICT, "order-not-modifiable");
        assertKind(new IncompleteOrderException(id, IncompleteOrderException.MissingPart.LINES),
                Kind.RULE_VIOLATION, "incomplete-order");
    }

    private static void assertKind(DomainException exception, Kind kind, String code) {
        assertThat(exception.kind()).as(exception.getClass().getSimpleName()).isEqualTo(kind);
        assertThat(exception.code()).isEqualTo(code);
    }
}
