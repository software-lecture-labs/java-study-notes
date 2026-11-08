package com.lectures.business.design.sales.application;

import com.lectures.business.design.shared.kernel.DomainEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.util.Objects;

@Dependent
public final class CdiDomainEventPublisher implements DomainEventPublisher {

    private final Event<DomainEvent> events;

    @Inject
    public CdiDomainEventPublisher(Event<DomainEvent> events) {
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    @Override
    public void publish(DomainEvent event) {
        events.fire(Objects.requireNonNull(event, "event must not be null"));
    }
}
