package com.lectures.business.design.sales.rest;

import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.OrderingFacade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OrderResource implements HttpHandler {

    private static final Pattern COLLECTION = Pattern.compile("^/orders/?$");
    private static final Pattern ITEM = Pattern.compile("^/orders/(\\d+)$");
    private static final Pattern ACTION = Pattern.compile("^/orders/(\\d+)/(confirm|ship|cancel)$");

    private final OrderingFacade orders;
    private final RequestValidator validator;
    private final JsonCodec json;
    private final ProblemMapper problems;

    OrderResource(OrderingFacade orders, RequestValidator validator, JsonCodec json, ProblemMapper problems) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.json = Objects.requireNonNull(json, "json must not be null");
        this.problems = Objects.requireNonNull(problems, "problems must not be null");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try (exchange) {
            try {
                route(exchange, exchange.getRequestMethod(), path);
            } catch (Exception failure) {
                // The JAX-RS ExceptionMapper's job, done by hand
                send(exchange, problems.toProblem(failure, path));
            }
        }
    }

    private void route(HttpExchange exchange, String method, String path) throws IOException {
        Matcher m;
        if (COLLECTION.matcher(path).matches()) {
            requireMethod(exchange, method, "POST");
            PlaceOrderRequest request = validator.validate(
                    json.read(exchange.getRequestBody(), PlaceOrderRequest.class));
            OrderSummary placed = orders.placeOrder(request.toCommand());
            exchange.getResponseHeaders().add("Location", "/orders/" + placed.orderId());
            send(exchange, 201, placed);
        } else if ((m = ITEM.matcher(path)).matches()) {
            requireMethod(exchange, method, "GET");
            send(exchange, 200, orders.getOrder(Integer.parseInt(m.group(1))));
        } else if ((m = ACTION.matcher(path)).matches()) {
            requireMethod(exchange, method, "POST");
            send(exchange, 200, action(m.group(2)).apply(Integer.parseInt(m.group(1))));
        } else {
            send(exchange, ProblemDetail.of(404, "no-such-route", "Resource not found",
                    "no resource at " + path, path));
        }
    }

    private IntFunction<OrderSummary> action(String name) {
        return switch (name) {
            case "confirm" ->
                orders::confirm;
            case "ship" ->
                orders::ship;
            case "cancel" ->
                orders::cancel;
            default ->
                throw new IllegalStateException("route pattern and actions disagree: " + name);
        };
    }

    private static void requireMethod(HttpExchange exchange, String actual, String allowed) {
        if (!allowed.equals(actual)) {
            exchange.getResponseHeaders().set("Allow", allowed);
            throw new MethodNotAllowed(actual, allowed);
        }
    }

    private void send(HttpExchange exchange, int status, Object body) throws IOException {
        write(exchange, status, "application/json", json.write(body));
    }

    private void send(HttpExchange exchange, ProblemDetail problem) throws IOException {
        write(exchange, problem.status(), ProblemDetail.MEDIA_TYPE, json.write(problem));
    }

    private static void write(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
