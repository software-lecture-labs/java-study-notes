package com.lectures.business.design.sales.rest;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public final class JsonbConfiguration implements ContextResolver<Jsonb> {

    private static final String FAIL_ON_UNKNOWN_PROPERTIES = "jsonb.fail-on-unknown-properties";

    private final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
            .setProperty(FAIL_ON_UNKNOWN_PROPERTIES, true));

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}
