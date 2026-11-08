package com.lectures.business.design.sales.rest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lectures.business.design.sales.api.OrderingFacade;
import com.lectures.business.design.sales.application.OrderMapper;
import com.lectures.business.design.sales.application.OrderService;
import com.lectures.business.design.sales.application.SteppingClock;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.shared.kernel.Address;

import jakarta.ws.rs.core.Application;

class OrderResourceTest {

    private static final String VALID_ORDER = """
            {"customerId": "VINET",
             "lines": [{"productId": 11, "unitPrice": 14.00, "quantity": 10},
                       {"productId": 42, "unitPrice": 9.80, "quantity": 5, "discount": 0.10}]}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private JerseyTest jersey;

    @BeforeEach
    void startJersey() throws Exception {
        SteppingClock clock = new SteppingClock("1996-07-04T09:00:00Z");
        CustomerRepository customers = new InMemoryCustomerRepository();
        customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
        OrderService service = new OrderService(new InMemoryOrderRepository(), customers,
                new OrderFactory(new SequentialOrderIdGenerator(10248), clock),
                new OrderMapper(), clock, event -> {
                });

        jersey = new JerseyTest() {
            @Override
            protected Application configure() {
                SalesRestApplication application = new SalesRestApplication();
                return new ResourceConfig(application.getClasses())
                        .addProperties(application.getProperties())
                        .register(new AbstractBinder() {
                            @Override
                            protected void configure() {
                                // What CDI does on the server: hand OrderResource an OrderingFacade
                                bind(service).to(OrderingFacade.class);
                            }
                        });
            }
        };
        jersey.setUp();
    }

    @AfterEach
    void stopJersey() throws Exception {
        jersey.tearDown();
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return jersey.target().getUri().resolve(path);
    }

    @Test
    @DisplayName("POST /orders answers 201, a Location header and the summary")
    void placeOrderReturnsCreated() throws Exception {
        HttpResponse<String> response = post("/orders", VALID_ORDER);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).endsWith("/orders/10248"));
        assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(response.body())
                .contains("\"orderId\":10248", "\"status\":\"DRAFT\"",
                        "\"orderDate\":\"1996-07-04\"", "\"total\":184.10");
    }

    @Test
    @DisplayName("the whole lifecycle over HTTP: place, confirm, ship, read")
    void lifecycleOverHttp() throws Exception {
        post("/orders", VALID_ORDER);

        assertThat(post("/orders/10248/confirm", "").body()).contains("\"status\":\"CONFIRMED\"");
        assertThat(post("/orders/10248/ship", "").body()).contains("\"status\":\"SHIPPED\"");
        assertThat(get("/orders/10248").body()).contains("\"status\":\"SHIPPED\"");
    }

    @Test
    @DisplayName("every invalid field is reported at once, as a 400 problem")
    void validationFailuresAreCollected() throws Exception {
        HttpResponse<String> response = post("/orders", """
                {"customerId": " ",
                 "lines": [{"productId": 0, "unitPrice": 14.001, "quantity": 0, "discount": 1.5}]}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).contains(ProblemDetail.MEDIA_TYPE);
        assertThat(response.body())
                .contains("\"code\":\"validation-failed\"", "\"detail\":\"5 field(s) are invalid\"")
                .contains("\"field\":\"customerId\"", "\"field\":\"lines[0].productId\"",
                        "\"field\":\"lines[0].quantity\"", "\"field\":\"lines[0].unitPrice\"",
                        "\"field\":\"lines[0].discount\"", "must be less than or equal to 1")
                .doesNotContain("${", "place.", "arg0");
    }

    @Test
    @DisplayName("a missing body is a validation failure of the body itself")
    void nullBody() throws Exception {
        HttpResponse<String> response = post("/orders", "null");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"field\":\"body\"");
    }

    @Test
    @DisplayName("shape is checked at the edge, invariants in the domain: a bad id format is still a 400")
    void domainValueObjectRejectionIsA400() throws Exception {
        HttpResponse<String> response = post("/orders", """
                {"customerId": "VIN3T", "lines": [{"productId": 11, "unitPrice": 14.00, "quantity": 1}]}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"invalid-argument\"", "five letters");
    }

    @Test
    @DisplayName("broken JSON and unknown fields are a 400, not a 500")
    void malformedBodies() throws Exception {
        assertThat(post("/orders", "{\"customerId\": ").statusCode()).isEqualTo(400);

        HttpResponse<String> typo = post("/orders", """
                {"customerID": "VINET", "lines": []}
                """);
        assertThat(typo.statusCode()).isEqualTo(400);
        assertThat(typo.body()).contains("\"code\":\"malformed-request\"");
    }

    @Test
    @DisplayName("domain refusals map by kind: NOT_FOUND is 404, CONFLICT is 409")
    void domainRefusalsMapByKind() throws Exception {
        HttpResponse<String> unknownCustomer = post("/orders", """
                {"customerId": "ALFKI", "lines": [{"productId": 11, "unitPrice": 14.00, "quantity": 1}]}
                """);
        assertThat(unknownCustomer.statusCode()).isEqualTo(404);
        assertThat(unknownCustomer.body()).contains("\"code\":\"customer-not-found\"",
                "\"type\":\"https://northwind.example/problems/customer-not-found\"");

        assertThat(get("/orders/99999").statusCode()).isEqualTo(404);

        post("/orders", VALID_ORDER);
        HttpResponse<String> shipDraft = post("/orders/10248/ship", "");
        assertThat(shipDraft.statusCode()).isEqualTo(409);
        assertThat(shipDraft.body()).contains("\"code\":\"invalid-state-transition\"",
                "\"instance\":\"/orders/10248/ship\"");
    }

    @Test
    @DisplayName("a known route with the wrong verb is a 405 with an Allow header")
    void wrongVerb() throws Exception {
        HttpResponse<String> response = get("/orders/10248/ship");

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow"))
                .hasValueSatisfying(allow -> assertThat(allow).contains("POST"));
        assertThat(response.body()).contains("\"code\":\"method-not-allowed\"");
    }

    @Test
    @DisplayName("a body that is not JSON is a 415")
    void wrongMediaType() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/orders"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.body()).contains("\"code\":\"unsupported-media-type\"");
    }

    @Test
    @DisplayName("an unknown route is a 404 problem, too")
    void unknownRoute() throws Exception {
        HttpResponse<String> response = get("/orders/10248/refund");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"no-such-route\"");
    }
}
