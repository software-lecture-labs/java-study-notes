package com.lectures.business.design.shared.kernel;

import java.util.Locale;
import java.util.Objects;

public abstract class DomainException extends RuntimeException {

    public enum Kind {
        NOT_FOUND,
        CONFLICT,
        RULE_VIOLATION
    }

    private final Kind kind;

    protected DomainException(String message, Kind kind) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        String name = getClass().getSimpleName().replaceFirst("Exception$", "");
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }
}
