package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LoggingCustomerNotifier implements CustomerNotifier {

    private static final System.Logger LOG = System.getLogger(LoggingCustomerNotifier.class.getName());

    @Override
    public void notify(String customerId, String message) {
        LOG.log(System.Logger.Level.INFO, "[notify {0}] {1}", customerId, message);
    }
}
