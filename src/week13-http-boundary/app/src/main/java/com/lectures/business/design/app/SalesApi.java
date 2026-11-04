package com.lectures.business.design.app;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.rest.SalesHttpServer;
import com.lectures.business.design.shared.kernel.Address;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;

public final class SalesApi {

    private SalesApi() {
    }

    public static void main(String[] args) throws InterruptedException {
        int port = Integer.parseInt(System.getenv().getOrDefault("SALES_HTTP_PORT", "8080"));

        DataSource dataSource = Database.migrated(Database.fromEnvironment(System.getenv()));
        SalesModule module = SalesModule.jdbc(dataSource, Clock.systemUTC(), new ConsoleCustomerNotifier());
        module.customers().save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
        module.customers().save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                new Address("Luisenstr. 48", "Munster", "44087", "Germany")));

        SalesHttpServer server = module.startHttpApi(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("Sales API listening on http://localhost:" + server.port() + "/orders (Ctrl+C to stop)");

        new CountDownLatch(1).await();
    }
}
