package com.lectures.business.design.sales.rest;

import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.OrderingFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Objects;

@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OrderResource {

    private final OrderingFacade orders;

    protected OrderResource() {
        this.orders = null;
    }

    @Inject
    public OrderResource(OrderingFacade orders) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response place(@NotNull @Valid PlaceOrderRequest request, @Context UriInfo uri) {
        OrderSummary placed = orders.placeOrder(request.toCommand());
        URI location = uri.getAbsolutePathBuilder().path(String.valueOf(placed.orderId())).build();
        return Response.created(location).entity(placed).build();
    }

    @GET
    @Path("{id: \\d+}")
    public OrderSummary get(@PathParam("id") int id) {
        return orders.getOrder(id);
    }

    @POST
    @Path("{id: \\d+}/confirm")
    public OrderSummary confirm(@PathParam("id") int id) {
        return orders.confirm(id);
    }

    @POST
    @Path("{id: \\d+}/ship")
    public OrderSummary ship(@PathParam("id") int id) {
        return orders.ship(id);
    }

    @POST
    @Path("{id: \\d+}/cancel")
    public OrderSummary cancel(@PathParam("id") int id) {
        return orders.cancel(id);
    }
}
