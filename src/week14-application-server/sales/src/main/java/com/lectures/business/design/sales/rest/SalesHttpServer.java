package com.lectures.business.design.sales.rest;

import com.lectures.business.design.sales.api.OrderingFacade;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SalesHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor;

    private SalesHttpServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    public static SalesHttpServer start(int port, OrderingFacade orders, RequestValidator validator) {
        Objects.requireNonNull(orders, "orders must not be null");
        Objects.requireNonNull(validator, "validator must not be null");
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            server.createContext("/orders", new OrderResource(orders, validator, new JsonCodec(), new ProblemMapper()));
            server.start();
            return new SalesHttpServer(server, executor);
        } catch (IOException e) {
            throw new UncheckedIOException("could not start the HTTP server on port " + port, e);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }
}
