package com.lectures.business.design.sales.rest;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@ApplicationPath("api")
@OpenAPIDefinition(info = @Info(
        title = "Northwind Sales API",
        version = "1.0",
        description = "Places orders and moves them through confirm, ship and cancel."))
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
