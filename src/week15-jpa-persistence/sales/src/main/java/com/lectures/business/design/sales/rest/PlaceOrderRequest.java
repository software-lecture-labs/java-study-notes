package com.lectures.business.design.sales.rest;

import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
        @NotBlank
        String customerId,
        @NotEmpty
        @Size(max = 50)
        List<@NotNull @Valid Line> lines) {

    public record Line(
            @Positive
            int productId,
            @NotNull
            @Positive
            @Digits(integer = 8, fraction = 2)
            BigDecimal unitPrice,
            @Positive
            int quantity,
            @DecimalMin("0")
            @DecimalMax("1")
            @Digits(integer = 1, fraction = 2)
            BigDecimal discount) {

    }

    PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(customerId, lines.stream()
                .map(line -> new OrderLineRequest(line.productId(), line.unitPrice(), line.quantity(),
                        line.discount() == null ? BigDecimal.ZERO : line.discount()))
                .toList());
    }
}
