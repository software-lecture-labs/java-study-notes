package com.lectures.business.design.sales.rest;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("api")
public class SalesRestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(OrderResource.class, FailureMapper.class, ConstraintViolationMapper.class,
                JsonbConfiguration.class);
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("jersey.config.server.wadl.disableWadl", true);
    }
}
