package com.lectures.business.design.sales.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

final class ProblemResponses {

    private final ProblemMapper problems = new ProblemMapper();

    Response toResponse(Throwable failure, UriInfo request) {
        ProblemDetail problem = problems.toProblem(failure, request.getRequestUri().getPath());
        Response.ResponseBuilder builder = failure instanceof WebApplicationException rejected
                ? Response.fromResponse(rejected.getResponse())
                : Response.status(problem.status());
        return builder.status(problem.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }
}
