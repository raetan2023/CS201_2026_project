package com.delivery.routing;

import java.util.ArrayList;
import java.util.List;

/**
 * Sanity checks on hand-worked examples (see HOW_IT_WORKS.md).
 * Examples 1-3 are the same instances as Approach A's Demo, so the two
 * approaches can be compared directly. Examples 4-5 are specific to B.
 * Customers sit on a line; travel time = |position difference| in minutes.
 * Run: java -cp out com.delivery.routing.Demo
 */
public class Demo {

    public static void main(String[] args) {
        int failures = 0;

        // Example 1 (same as A): A = ice cream at +15 (deadline 30), B = rice at -5, C = rice at -10.
        // Deadline order is A, B, C (mean wait 30.0). One move (B to the front) gives
        // B, A, C: all 3 delivered, mean wait 26.67. Same final route as SolverA.
        DeliveryInstance ex1 = lineInstance(
                new double[]{15, -5, -10},
                new double[]{30, 90, 90},
                new String[]{"ice cream", "rice", "rice"});
        RouteResult b1 = new SolverB().solve(ex1);
        RouteResult nn1 = nearestNeighbour(ex1);
        System.out.println("Example 1 (A/B/C)");
        System.out.println("  SolverB : " + b1);
        System.out.println("  NN      : " + nn1);
        failures += check("ex1 SolverB route", b1.route(), List.of(1, 0, 2));
        failures += check("ex1 SolverB delivered", b1.delivered(), 3);
        failures += check("ex1 NN delivered", nn1.delivered(), 2);

        // Example 2 (A's known limitation): P at +5 (deadline 10), Q at -5 (deadline 16), R rice at -1.
        // SolverA goes to R first and loses P. Deadline order P, Q, R already delivers all 3
        // (mean wait 13.0). One move (R before Q) picks R up on the way back: P, R, Q, mean 10.33.
        DeliveryInstance ex2 = lineInstance(
                new double[]{5, -5, -1},
                new double[]{10, 16, 90},
                new String[]{"P", "Q", "R rice"});
        RouteResult b2 = new SolverB().solve(ex2);
        System.out.println("Example 2 (P/Q/R, A's limitation)");
        System.out.println("  SolverB : " + b2);
        failures += check("ex2 SolverB route", b2.route(), List.of(0, 2, 1));
        failures += check("ex2 SolverB delivered", b2.delivered(), 3);

        // Example 3 (same as A): loose deadlines. All deadlines tie, so Step 1 gives id order,
        // which zig-zags. Step 2 then reorders purely to cut waiting time.
        DeliveryInstance ex3 = lineInstance(
                new double[]{3, -2, 7, -6, 1},
                new double[]{999, 999, 999, 999, 999},
                new String[]{"r", "r", "r", "r", "r"});
        RouteResult start3 = SolverB.walk(ex3, SolverB.deadlineOrder(ex3));
        RouteResult b3 = new SolverB().solve(ex3);
        RouteResult nn3 = nearestNeighbour(ex3);
        System.out.println("Example 3 (loose deadlines)");
        System.out.println("  Step 1  : " + start3);
        System.out.println("  SolverB : " + b3);
        System.out.println("  NN      : " + nn3);
        failures += check("ex3 SolverB delivered", b3.delivered(), 5);
        failures += check("ex3 SolverB wait <= NN wait", b3.meanWait() <= nn3.meanWait() + 1e-9, true);

        // Example 4 (why Step 2 is needed): X at +10 (deadline 10), Y at +5 (deadline 11).
        // Deadline order X, Y: X at 10, Y at 15 > 11, so Y is cancelled.
        // Moving Y to the front delivers both (Y at 5, X at 10): Y is "on the way".
        DeliveryInstance ex4 = lineInstance(
                new double[]{10, 5},
                new double[]{10, 11},
                new String[]{"X", "Y"});
        RouteResult start4 = SolverB.walk(ex4, SolverB.deadlineOrder(ex4));
        RouteResult b4 = new SolverB().solve(ex4);
        System.out.println("Example 4 (on the way)");
        System.out.println("  Step 1  : " + start4);
        System.out.println("  SolverB : " + b4);
        failures += check("ex4 Step 1 alone delivered", start4.delivered(), 1);
        failures += check("ex4 SolverB route", b4.route(), List.of(1, 0));

        // Example 5 (B's known limitation): ids 0..3 at -5, +5, -12, +4 with deadlines 16, 31, 24, 20.
        // Best route 3, 1, 0, 2 delivers all 4 (SolverA finds it). It needs both east-side
        // stops moved ahead of the west side; no single move achieves that, so B stops at 3.
        DeliveryInstance ex5 = lineInstance(
                new double[]{-5, 5, -12, 4},
                new double[]{16, 31, 24, 20},
                new String[]{"S", "T", "U", "V"});
        RouteResult start5 = SolverB.walk(ex5, SolverB.deadlineOrder(ex5));
        RouteResult b5 = new SolverB().solve(ex5);
        RouteResult best5 = SolverB.walk(ex5, List.of(3, 1, 0, 2));
        System.out.println("Example 5 (B's limitation: stuck after single moves)");
        System.out.println("  Step 1  : " + start5);
        System.out.println("  SolverB : " + b5);
        System.out.println("  Best    : " + best5);
        failures += check("ex5 best route delivers", best5.delivered(), 4);
        failures += check("ex5 SolverB delivered", b5.delivered(), 3);

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
