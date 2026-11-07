package com.lectures.business.design.app;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.shared.kernel.Address;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import java.util.Objects;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

@Dependent
public class SalesStartup {

    private static final System.Logger LOG = System.getLogger(SalesStartup.class.getName());

    private final DataSource dataSource;
    private final CustomerRepository customers;
    private final boolean demoData;

    @Inject
    public SalesStartup(DataSource dataSource, CustomerRepository customers,
            @ConfigProperty(name = "sales.demo-data", defaultValue = "false") boolean demoData) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.demoData = demoData;
    }

    void onStartup(@Observes Startup startup) {
        var result = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .load()
                .migrate();
        LOG.log(System.Logger.Level.INFO, "sales schema at version {0}", result.targetSchemaVersion);

        if (demoData) {
            customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                    new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
            customers.save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                    new Address("Luisenstr. 48", "Munster", "44087", "Germany")));
            LOG.log(System.Logger.Level.INFO, "demo customers VINET and TOMSP are in place");
        }
    }
}
