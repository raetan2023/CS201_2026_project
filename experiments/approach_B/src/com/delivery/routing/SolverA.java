package com.delivery.routing;

import java.util.ArrayList;
import java.util.List;

/**
 * Approach A: greedy construction with lookahead guards.
 *
 * At each step the rider chooses the next customer by comparing candidates
 * with rules in strict order (no weights):
 *
 *   ① Feasibility filter : skip u if the rider cannot reach u by deadline(u).
 *   ② Fewest lost        : minimise the number of currently-alive orders that
 *                          become unreachable if the rider goes to u next.
 *                          (serves objective 1: maximise on-time deliveries)
 *   ③ Shortest travel    : among ties, go to the nearest.
 *                          (serves objective 2: minimise mean wait)
 *   ④ Least slack        : among remaining ties, the more urgent order.
 *   ⑤ Lowest id          : final deterministic tie-break.
 *
 * "Alive" = unvisited and still reachable on time directly from the rider's
 * current position.
 *
 * Known limitation (by design, motivates Approach B): the lookahead checks
 * each remaining order independently, so it can report "0 lost" when two
 * orders are individually reachable but not jointly reachable.
 *
 * Complexity: n steps x O(n) candidates x O(n) lookahead = O(n^3) time, O(n) space.
 * No priority queue: every candidate's key changes after each move, so a heap
 * would need rebuilding every step (O(n log n)) and is slower than a linear scan (O(n)).
 */
public class SolverA {

    private static final double EPS = 1e-9;

    public RouteResult solve(DeliveryInstance inst) {
        int n = inst.size();
        boolean[] visited = new boolean[n];
        List<Integer> route = new ArrayList<>();
        List<Double> arrivals = new ArrayList<>();

        int currentNode = DeliveryInstance.DEPOT;
        double t = 0.0;

        while (true) {
            // Orders still deliverable from where the rider stands now.
            boolean[] alive = aliveFrom(inst, visited, currentNode, t);

            int best = -1;
            int bestLost = Integer.MAX_VALUE;
            double bestTravel = Double.MAX_VALUE;
            double bestSlack = Double.MAX_VALUE;

            for (int u = 0; u < n; u++) {
                if (!alive[u]) continue;                               // ① filter

                Customer cu = inst.customers().get(u);
                double travel = inst.travel(currentNode, DeliveryInstance.node(u));
                double arrival = t + travel;
                double slack = cu.deadline() - arrival;

                int lost = countLost(inst, alive, u, arrival);          // ② lookahead

                if (isBetter(lost, travel, slack, u,                    // ③ ④ ⑤
                             bestLost, bestTravel, bestSlack, best)) {
                    best = u;
                    bestLost = lost;
                    bestTravel = travel;
                    bestSlack = slack;
                }
            }

            if (best == -1) break;                                     // nothing reachable

            t += bestTravel;
            visited[best] = true;
            route.add(best);
            arrivals.add(t);
            currentNode = DeliveryInstance.node(best);
        }

        return buildResult(inst, visited, route, arrivals);
    }

    /** Lexicographic comparison: lost, then travel, then slack, then id. */
    private static boolean isBetter(int lost, double travel, double slack, int id,
                                    int bLost, double bTravel, double bSlack, int bId) {
        if (bId == -1) return true;
        if (lost != bLost) return lost < bLost;
        if (Math.abs(travel - bTravel) > EPS) return travel < bTravel;
        if (Math.abs(slack - bSlack) > EPS) return slack < bSlack;
        return id < bId;
    }

    /** alive[v] = v unvisited and reachable on time directly from (node, t). */
    private static boolean[] aliveFrom(DeliveryInstance inst, boolean[] visited,
                                       int node, double t) {
        int n = inst.size();
        boolean[] alive = new boolean[n];
        for (int v = 0; v < n; v++) {
            if (visited[v]) continue;
            double arr = t + inst.travel(node, DeliveryInstance.node(v));
            alive[v] = arr <= inst.customers().get(v).deadline() + EPS;
        }
        return alive;
    }

    /**
     * Number of alive orders (other than u) that would no longer be reachable
     * on time if the rider goes to u first and arrives at {@code arrivalAtU}.
     * Each order is checked independently (1-step, optimistic lookahead).
     */
    private static int countLost(DeliveryInstance inst, boolean[] alive,
                                 int u, double arrivalAtU) {
        int lost = 0;
        int uNode = DeliveryInstance.node(u);
        for (int v = 0; v < inst.size(); v++) {
            if (!alive[v] || v == u) continue;
            double arr = arrivalAtU + inst.travel(uNode, DeliveryInstance.node(v));
            if (arr > inst.customers().get(v).deadline() + EPS) lost++;
        }
        return lost;
    }

    private static RouteResult buildResult(DeliveryInstance inst, boolean[] visited,
                                           List<Integer> route, List<Double> arrivals) {
        List<Integer> dropped = new ArrayList<>();
        for (int v = 0; v < inst.size(); v++) {
            if (!visited[v]) dropped.add(v);
        }
        double totalWait = 0.0;
        for (int k = 0; k < route.size(); k++) {
            totalWait += arrivals.get(k) - inst.customers().get(route.get(k)).orderTime();
        }
        double meanWait = route.isEmpty() ? 0.0 : totalWait / route.size();
        return new RouteResult(route, arrivals, dropped, meanWait);
    }
}
