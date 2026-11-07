package com.delivery.routing;

import java.util.List;

/**
 * Output of a solver.
 *
 * @param route    customer ids in delivery order (delivered orders only)
 * @param arrivals arrival time at each stop in {@code route}, same order
 * @param dropped  customer ids that could not be delivered before their deadline
 * @param meanWait mean (arrival - orderTime) over delivered customers; 0 if none delivered
 */
public record RouteResult(List<Integer> route, List<Double> arrivals,
                          List<Integer> dropped, double meanWait) {

    public int delivered() {
        return route.size();
    }

    @Override
    public String toString() {
        return String.format("route=%s arrivals=%s dropped=%s delivered=%d meanWait=%.2f",
                route, arrivals, dropped, delivered(), meanWait);
    }
}
