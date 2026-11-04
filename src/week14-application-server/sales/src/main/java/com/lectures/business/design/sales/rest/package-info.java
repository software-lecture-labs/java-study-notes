/**
 * The HTTP edge of the sales module.
 *
 * <p>
 * Sees {@code api} and {@code shared.kernel} only - never domain, application
 * or persistence (ADR-0009). The only package allowed to use Jackson, Bean
 * Validation and the JDK HTTP server; nobody depends on it.
 */
package com.lectures.business.design.sales.rest;
