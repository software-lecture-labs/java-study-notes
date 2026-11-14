package com.lectures.business.design.app;

import java.util.Objects;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@Dependent
public class SalesStartup {

    private static final System.Logger LOG = System.getLogger(SalesStartup.class.getName());

    private final DataSource dataSource;
    private final DemoCustomers demoCustomers;
    private final boolean demoData;

    @Inject
    public SalesStartup(DataSource dataSource, DemoCustomers demoCustomers,
            @ConfigProperty(name = "sales.demo-data", defaultValue = "false") boolean demoData) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.demoCustomers = Objects.requireNonNull(demoCustomers, "demoCustomers must not be null");
        this.demoData = demoData;
    }

    void onStartup(@Observes Startup startup) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .load();
        int applied = flyway.migrate().migrationsExecuted;
        LOG.log(System.Logger.Level.INFO, "sales schema at version {0}, {1} migration(s) applied now",
                flyway.info().current().getVersion(), applied);

        if (demoData) {
            demoCustomers.putInPlace();
            LOG.log(System.Logger.Level.INFO, "demo customers VINET and TOMSP are in place");
        }
    }
}
