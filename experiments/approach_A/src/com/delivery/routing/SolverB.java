package com.delivery.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Approach B: deadline order first, then improve it by moving one stop at a time.
 *
 * Unlike Approach A, B does not pick stops one by one. It always holds a
 * complete plan: a list of ALL customers in the order the rider will try
 * them. A plan is scored by simply driving it ("walk"):
 *
 *   go through the plan in order; if the rider can reach the next customer
 *   by its deadline, drive there and deliver; otherwise cancel that order
 *   and stay put.
 *
 * Step 1 (start plan) : sort customers by deadline, most urgent first
 *                       (Earliest Deadline First). Ties: lower id first.
 * Step 2 (improve)    : try every "move" (take one customer out of the plan
 *                       and put it back at another position). Keep the
 *                       single best move if it makes the plan better.
 *                       Repeat until no move helps.
 *
 * "Better" compares the two goals in strict order (no weights):
 *   (1) more orders delivered, then (2) lower mean wait.
 *
 * Main data structure: the plan, a plain List of customer ids (a permutation).
 * Cancelled customers stay in the plan, so a later move can bring them back.
 *
 * Known limitation (by design): Step 2 only ever changes ONE stop at a time
 * (a local search). If fixing the plan needs two stops moved together, and
 * each move alone makes nothing better, B stops early (see Demo, Example 5).
 *
 * Complexity: one pass tries n(n-1) moves, and each move costs O(n) to copy
 * and walk the plan, so one pass is O(n^3). The number of passes depends on
 * the instance (each pass must strictly improve the plan, so it always
 * terminates). Space: O(n) for the plan on top of the travel matrix.
 */
public class SolverB {

    private static final double EPS = 1e-9;

    public RouteResult solve(DeliveryInstance inst) {
        // Step 1: Earliest Deadline First.
        List<Integer> plan = deadlineOrder(inst);
        RouteResult best = walk(inst, plan);

        // Step 2: best single move, repeated until nothing improves.
        while (true) {
            List<Integer> bestPlan = null;
            int n = plan.size();
            for (int from = 0; from < n; from++) {
                for (int to = 0; to < n; to++) {
                    if (from == to) continue;
                    List<Integer> candidate = move(plan, from, to);
                    RouteResult result = walk(inst, candidate);
                    if (isBetter(result, best)) {
                        best = result;
                        bestPlan = candidate;
                    }
                }
            }
            if (bestPlan == null) break;      // local optimum: no move helps
            plan = bestPlan;
        }
        return best;
    }

    /** All customer ids, most urgent deadline first; ties broken by lower id. */
    static List<Integer> deadlineOrder(DeliveryInstance inst) {
        List<Integer> plan = new ArrayList<>();
        for (Customer c : inst.customers()) plan.add(c.id());
        plan.sort(Comparator.comparingDouble((Integer id) -> inst.customers().get(id).deadline())
                            .thenComparingInt(id -> id));
        return plan;
    }

    /** A copy of the plan with the customer at position {@code from} moved to position {@code to}. */
    static List<Integer> move(List<Integer> plan, int from, int to) {
        List<Integer> copy = new ArrayList<>(plan);
        int id = copy.remove(from);
        copy.add(to, id);
        return copy;
    }

    /**
     * Drives the plan: deliver each customer if the rider arrives by its
     * deadline, otherwise cancel it (the rider does not move).
     */
    static RouteResult walk(DeliveryInstance inst, List<Integer> plan) {
        List<Integer> route = new ArrayList<>();
        List<Double> arrivals = new ArrayList<>();
        List<Integer> dropped = new ArrayList<>();
        int currentNode = DeliveryInstance.DEPOT;
        double t = 0.0;
        double totalWait = 0.0;

        for (int id : plan) {
            Customer c = inst.customers().get(id);
            int node = DeliveryInstance.node(id);
            double arrival = t + inst.travel(currentNode, node);
            if (arrival <= c.deadline() + EPS) {
                t = arrival;
                currentNode = node;
                route.add(id);
                arrivals.add(t);
                totalWait += t - c.orderTime();
            } else {
                dropped.add(id);
            }
        }
        dropped.sort(null);
        double meanWait = route.isEmpty() ? 0.0 : totalWait / route.size();
        return new RouteResult(route, arrivals, dropped, meanWait);
    }

    /** Goal order: more delivered first; mean wait only breaks ties. */
    static boolean isBetter(RouteResult a, RouteResult b) {
        if (a.delivered() != b.delivered()) return a.delivered() > b.delivered();
        return a.meanWait() < b.meanWait() - EPS;
    }
}
