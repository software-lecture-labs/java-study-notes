package com.lectures.business.design.sales.rest;

import com.lectures.business.design.sales.domain.IncompleteOrderException;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.persistence.RepositoryException;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProblemMapperTest {

    private final ProblemMapper mapper = new ProblemMapper();

    @Test
    @DisplayName("RULE_VIOLATION becomes 422 Unprocessable Content")
    void ruleViolationIs422() {
        ProblemDetail problem = mapper.toProblem(new IncompleteOrderException(
                OrderId.of(10248), IncompleteOrderException.MissingPart.SHIPPING_ADDRESS), "/orders/10248/confirm");

        assertThat(problem.status()).isEqualTo(422);
        assertThat(problem.code()).isEqualTo("incomplete-order");
        assertThat(problem.detail()).contains("SHIPPING_ADDRESS");
    }

    @Test
    @DisplayName("an infrastructure failure is a 500 that leaks nothing")
    void unexpectedFailuresAreOpaque() {
        RepositoryException failure = new RepositoryException("could not save order 10248",
                new SQLException("password authentication failed for user \"johndoe\""));

        ProblemDetail problem = mapper.toProblem(failure, "/orders");

        assertThat(problem.status()).isEqualTo(500);
        assertThat(problem.detail()).isEqualTo("an unexpected error occurred");
        assertThat(problem.toString()).doesNotContain("johndoe", "10248");
    }
}
