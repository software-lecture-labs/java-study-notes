package com.lectures.business.design.sales.rest;

import com.lectures.business.design.shared.kernel.DomainException;
import jakarta.json.bind.JsonbException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class ProblemMapper {

    private static final System.Logger LOG = System.getLogger(ProblemMapper.class.getName());

    private static final Set<ElementKind> NOT_PART_OF_THE_BODY = EnumSet.of(
            ElementKind.METHOD, ElementKind.CONSTRUCTOR, ElementKind.PARAMETER,
            ElementKind.CROSS_PARAMETER, ElementKind.RETURN_VALUE);

    ProblemDetail toProblem(Throwable failure, String instance) {
        return switch (failure) {
            case ConstraintViolationException invalid ->
                validation(invalid, instance);
            case WebApplicationException rejectedByJaxRs ->
                http(rejectedByJaxRs.getResponse().getStatus(), rejectedByJaxRs, instance);
            case JsonbException malformed ->
                malformed(instance);
            case ProcessingException wrapped when wrapped.getCause() instanceof JsonbException ->
                malformed(instance);
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
                .map(v -> new ProblemDetail.FieldError(field(v.getPropertyPath()), v.getMessage()))
                .sorted(Comparator.comparing(ProblemDetail.FieldError::field))
                .toList();
        return ProblemDetail.of(400, "validation-failed", "Request validation failed",
                errors.size() + " field(s) are invalid", instance).withErrors(errors);
    }

    private static String field(Path path) {
        StringBuilder field = new StringBuilder();
        for (Path.Node node : path) {
            if (NOT_PART_OF_THE_BODY.contains(node.getKind())) {
                continue;
            }
            if (node.isInIterable()) {
                field.append('[').append(node.getIndex() != null ? node.getIndex() : node.getKey()).append(']');
            }
            if (node.getKind() == ElementKind.PROPERTY) {
                field.append(field.isEmpty() ? "" : ".").append(node.getName());
            }
        }
        return field.isEmpty() ? "body" : field.toString();
    }

    private static ProblemDetail http(int status, WebApplicationException failure, String instance) {
        return switch (status) {
            case 400 ->
                malformed(instance);
            case 404 ->
                ProblemDetail.of(404, "no-such-route", "Resource not found",
                "no resource at " + instance, instance);
            case 405 ->
                ProblemDetail.of(405, "method-not-allowed", "Method not allowed",
                "this resource does not support the request method", instance);
            case 415 ->
                ProblemDetail.of(415, "unsupported-media-type", "Unsupported media type",
                "send the body as application/json", instance);
            default ->
                ProblemDetail.of(status, "http-" + status, "HTTP " + status, failure.getMessage(), instance);
        };
    }

    private static ProblemDetail malformed(String instance) {
        return ProblemDetail.of(400, "malformed-request",
                "Malformed request body", "the body is not valid JSON for this resource", instance);
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
