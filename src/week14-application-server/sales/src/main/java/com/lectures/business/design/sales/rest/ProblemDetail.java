package com.lectures.business.design.sales.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        List<FieldError> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";
    static final String TYPE_BASE = "https://northwind.example/problems/";

    public record FieldError(String field, String message) {

    }

    static ProblemDetail of(int status, String slug, String title, String detail, String instance) {
        return new ProblemDetail(TYPE_BASE + slug, title, status, detail, instance, slug, List.of());
    }

    ProblemDetail withErrors(List<FieldError> fieldErrors) {
        return new ProblemDetail(type, title, status, detail, instance, code, List.copyOf(fieldErrors));
    }
}
