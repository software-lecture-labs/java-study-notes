package com.lectures.business.design.sales.rest;

final class MethodNotAllowed extends RuntimeException {

    MethodNotAllowed(String actual, String allowed) {
        super(actual + " is not allowed here; use " + allowed);
    }
}
