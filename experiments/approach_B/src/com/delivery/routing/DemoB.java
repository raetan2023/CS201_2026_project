package com.delivery.routing;

import java.util.ArrayList;
import java.util.List;

/**
 * Sanity checks on the hand-worked examples from the design discussion.
 * Customers sit on a line; travel time = |position difference| in minutes.
 * Run: java -cp out com.delivery.routing.DemoB
 */
public class DemoB {

    public static void main(String[] args) {
        int failures = 0;

        // Example 1: A = ice cream at +15 (deadline 30), B = rice at -5, C = rice at -10.
        // Expected: SolverB -> B, A, C (all 3, waits 5+25+50 => mean 26.67).
        //           NN      -> B, C, then A late (only 2 on time).
        DeliveryInstance ex1 = lineInstance(
                new double[]{15, -5, -10},
                new double[]{30, 90, 90},
                new String[]{"ice cream", "rice", "rice"});
        RouteResult a1 = new SolverB().solve(ex1);
        RouteResult nn1 = nearestNeighbour(ex1);
        System.out.println("Example 1 (A/B/C)");
        System.out.println("  SolverB : " + a1);
        System.out.println("  NN      : " + nn1);
        failures += check("ex1 SolverB route", a1.route(), List.of(1, 0, 2));
        failures += check("ex1 SolverB delivered", a1.delivered(), 3);
        failures += check("ex1 NN delivered", nn1.delivered(), 2);

        // Example 2 (known limitation): P at +5 (deadline 10), Q at -5 (deadline 16), R rice at -1.
        // Optimal is P, Q, R (all 3). SolverB's independent lookahead sees R as "0 lost",
        // goes to R first, and then P and Q cannot both be reached -> 2 delivered.
        DeliveryInstance ex2 = lineInstance(
                new double[]{5, -5, -1},
                new double[]{10, 16, 90},
                new String[]{"P", "Q", "R rice"});
        RouteResult a2 = new SolverB().solve(ex2);
        System.out.println("Example 2 (P/Q/R, documented limitation)");
        System.out.println("  SolverB : " + a2);
        failures += check("ex2 SolverB first stop is R", a2.route().get(0), 2);
        failures += check("ex2 SolverB delivered", a2.delivered(), 2);

        // Example 3: loose deadlines -> SolverB must equal NN exactly.
        DeliveryInstance ex3 = lineInstance(
                new double[]{3, -2, 7, -6, 1},
                new double[]{999, 999, 999, 999, 999},
                new String[]{"r", "r", "r", "r", "r"});
        RouteResult a3 = new SolverB().solve(ex3);
        RouteResult nn3 = nearestNeighbour(ex3);
        System.out.println("Example 3 (loose deadlines)");
        System.out.println("  SolverB : " + a3);
        System.out.println("  NN      : " + nn3);
        failures += check("ex3 SolverB == NN", a3.route(), nn3.route());

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        if (failures > 0) System.exit(1);
    }

    /** Baseline: classic nearest neighbour, visits everyone; late arrivals count as dropped. */
    static RouteResult nearestNeighbour(DeliveryInstance inst) {
        int n = inst.size();
        boolean[] visited = new boolean[n];
        List<Integer> route = new ArrayList<>();
        List<Double> arrivals = new ArrayList<>();
        List<Integer> dropped = new ArrayList<>();
        int cur = DeliveryInstance.DEPOT;
        double t = 0, totalWait = 0;
        for (int step = 0; step < n; step++) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int v = 0; v < n; v++) {
                if (visited[v]) continue;
                double d = inst.travel(cur, DeliveryInstance.node(v));
                if (d < bestD) { bestD = d; best = v; }
            }
            visited[best] = true;
            t += bestD;
            cur = DeliveryInstance.node(best);
            if (t <= inst.customers().get(best).deadline() + 1e-9) {
                route.add(best);
                arrivals.add(t);
                totalWait += t - inst.customers().get(best).orderTime();
            } else {
                dropped.add(best);
            }
        }
        return new RouteResult(route, arrivals, dropped, route.isEmpty() ? 0 : totalWait / route.size());
    }

    /** Builds an instance on a line; restaurant at position 0, all orders at t = 0. */
    static DeliveryInstance lineInstance(double[] pos, double[] shelfLife, String[] food) {
        int n = pos.length;
        double[] all = new double[n + 1];
        all[0] = 0;
        System.arraycopy(pos, 0, all, 1, n);
        double[][] m = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++)
                m[i][j] = Math.abs(all[i] - all[j]);
        List<Customer> cs = new ArrayList<>();
        for (int i = 0; i < n; i++) cs.add(new Customer(i, 0.0, shelfLife[i], food[i]));
        return new DeliveryInstance(m, cs);
    }

    private static int check(String name, Object actual, Object expected) {
        boolean ok = actual.equals(expected);
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + name
                + (ok ? "" : " expected=" + expected + " actual=" + actual));
        return ok ? 0 : 1;
    }
}
