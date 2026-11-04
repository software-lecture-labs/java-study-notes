package com.lectures.business.design.sales.rest;

import com.fasterxml.jackson.core.JacksonException;
import com.lectures.business.design.shared.kernel.DomainException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;

final class ProblemMapper {

    private static final System.Logger LOG = System.getLogger(ProblemMapper.class.getName());

    ProblemDetail toProblem(Throwable failure, String instance) {
        return switch (failure) {
            case ConstraintViolationException invalid ->
                validation(invalid, instance);
            case MethodNotAllowed wrongVerb ->
                ProblemDetail.of(405, "method-not-allowed",
                "Method not allowed", wrongVerb.getMessage(), instance);
            case JacksonException malformed ->
                ProblemDetail.of(400, "malformed-request",
                "Malformed request body", "the body is not valid JSON for this resource", instance);
            case DomainException refused ->
                domain(refused, instance);
            case IllegalArgumentException invalid ->
                ProblemDetail.of(400, "invalid-argument",
                "Invalid argument", invalid.getMessage(), instance);
            default ->
                unexpected(failure, instance);
        };
    }

    private static ProblemDetail validation(ConstraintViolationException invalid, String instance) {
        List<ProblemDetail.FieldError> errors = invalid.getConstraintViolations().stream()
                .sorted(Comparator.comparing((ConstraintViolation<?> v) -> v.getPropertyPath().toString()))
                .map(v -> new ProblemDetail.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ProblemDetail.of(400, "validation-failed", "Request validation failed",
                errors.size() + " field(s) are invalid", instance).withErrors(errors);
    }

    private static ProblemDetail domain(DomainException refused, String instance) {
        return switch (refused.kind()) {
            case NOT_FOUND ->
                problem(404, "Resource not found", refused, instance);
            case CONFLICT ->
                problem(409, "Conflict with the current state", refused, instance);
            case RULE_VIOLATION ->
                problem(422, "Business rule violated", refused, instance);
        };
    }

    private static ProblemDetail problem(int status, String title, DomainException refused, String instance) {
        return ProblemDetail.of(status, refused.code(), title, refused.getMessage(), instance);
    }

    private static ProblemDetail unexpected(Throwable failure, String instance) {
        LOG.log(System.Logger.Level.ERROR, "unhandled failure on " + instance, failure);
        return ProblemDetail.of(500, "internal-error", "Internal server error",
                "an unexpected error occurred", instance);
    }
}
