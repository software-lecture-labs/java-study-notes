/**
 * The HTTP edge of the sales module, on Jakarta REST (JAX-RS).
 *
 * <p>
 * Sees {@code api} and {@code shared.kernel} only - never domain, application
 * or persistence (ADR-0009). The only package allowed to use JAX-RS, JSON-B
 * and Bean Validation; nobody depends on it.
 */
package com.lectures.business.design.sales.rest;
