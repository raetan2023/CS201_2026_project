package com.delivery.routing;

import java.util.List;

/**
 * A static problem instance.
 *
 * INDEXING CONVENTION (fixes the depot/customer-0 collision in the proposal):
 *   travelMatrix is (n + 1) x (n + 1), in minutes.
 *   Index 0      = restaurant (depot).
 *   Index i + 1  = customer with id i.
 */
public record DeliveryInstance(double[][] travelMatrix, List<Customer> customers) {

    public static final int DEPOT = 0;

    public DeliveryInstance {
        int expected = customers.size() + 1;
        if (travelMatrix.length != expected) {
            throw new IllegalArgumentException(
                "travelMatrix must be (n+1)x(n+1) with depot at index 0; expected "
                + expected + " rows, got " + travelMatrix.length);
        }
    }

    public int size() {
        return customers.size();
    }

    /** Matrix index of a customer id. */
    public static int node(int customerId) {
        return customerId + 1;
    }

    /** Travel time between two matrix indices. */
    public double travel(int fromNode, int toNode) {
        return travelMatrix[fromNode][toNode];
    }
}
