package com.lectures.business.design.sales.rest;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public final class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private final ProblemResponses responses = new ProblemResponses();

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(ConstraintViolationException failure) {
        return responses.toResponse(failure, request);
    }
}
