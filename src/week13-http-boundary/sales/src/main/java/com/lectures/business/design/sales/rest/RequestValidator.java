package com.lectures.business.design.sales.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

public final class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public static RequestValidator withoutExpressionLanguage() {
        Validator validator = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
        return new RequestValidator(validator);
    }

    <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }
}
