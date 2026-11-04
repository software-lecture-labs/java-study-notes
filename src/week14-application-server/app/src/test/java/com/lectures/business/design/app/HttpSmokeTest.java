package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.SteppingClock;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.rest.SalesHttpServer;
import com.lectures.business.design.shared.kernel.Address;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HttpSmokeTest {

    @Test
    @DisplayName("HTTP to database and back: the edge, the use case, JDBC and an observer, wired once")
    void fullStack() throws Exception {
        List<String> sent = new ArrayList<>();
        SalesModule module = SalesModule.jdbc(Database.migrated(Database.h2InMemory()),
                new SteppingClock("1996-07-04T09:00:00Z"), (customerId, message) -> sent.add(customerId));
        module.customers().save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));

        HttpClient client = HttpClient.newHttpClient();
        try (SalesHttpServer server = module.startHttpApi(0)) {
            String base = "http://localhost:" + server.port();
            HttpResponse<String> placed = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {"customerId": "VINET", "lines": [{"productId": 11, "unitPrice": 14.00, "quantity": 10}]}
                            """)).build(), HttpResponse.BodyHandlers.ofString());
            String location = placed.headers().firstValue("Location").orElseThrow();

            for (String action : List.of("/confirm", "/ship")) {
                client.send(HttpRequest.newBuilder(URI.create(base + location + action))
                        .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            }

            assertThat(placed.statusCode()).isEqualTo(201);
            assertThat(module.salesReport().countByStatus()).containsEntry("SHIPPED", 1L);
            assertThat(sent).containsExactly("VINET");
        }
    }
}
