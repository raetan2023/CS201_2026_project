package com.delivery.tools;

import com.delivery.routing.SolverA;
import com.delivery.routing.SolverB;
import java.util.ArrayList;
import java.util.List;

/** Reproduces the five-customer example in slides 2-15, including all 120 permutations. */
public final class CheckPresentationExample {
    private static int permutations, allDelivered;
    private static double bestWait = Double.POSITIVE_INFINITY;

    private static void check(String label, boolean passed) {
        if (!passed) throw new AssertionError(label);
        System.out.println("  [PASS] " + label);
    }

    private static void score(Instance in, String label, List<Integer> route,
                              int delivered, double wait) {
        Evaluate.Score s = Evaluate.evaluate(in, route);
        check(label, s.late() == 0 && s.delivered() == delivered
                && Math.abs(s.avgWait() - wait) < Common.EPS);
        System.out.println("    route=" + route + " " + s);
    }

    private static void enumerate(Instance in, List<Integer> plan, boolean[] used) {
        if (plan.size() == in.n) {
            permutations++;
            Evaluate.Score s = Evaluate.evaluate(in, plan);
            if (s.late() == 0) {
                allDelivered++;
                bestWait = Math.min(bestWait, s.avgWait());
            }
            return;
        }
        for (int id = 1; id <= in.n; id++) {
            if (used[id]) continue;
            used[id] = true;
            plan.add(id);
            enumerate(in, plan, used);
            plan.remove(plan.size() - 1);
            used[id] = false;
        }
    }

    public static void main(String[] args) {
        // Synthetic line example: positions are minutes of travel from the restaurant.
        // IDs 1..5 are Amy, Ben, Cat, Dan, Eve. All orders are placed at t=0.
        double[] positions = {0, 9, -13, 8, -7, 14};
        String[] names = {"Amy", "Ben", "Cat", "Dan", "Eve"};
        String[] foods = {"ice_cream", "noodles", "noodles", "rice", "ice_cream"};
        double[] shelfLives = {30, 60, 60, 90, 30};
        Instance in = new Instance();
        in.n = 5;
        in.tight = 1;
        for (int k = 0; k < in.n; k++) {
            in.customers.add(new Instance.Cust(k + 1, "synthetic-" + (k + 1), names[k],
                    0, 0, 0, foods[k], shelfLives[k]));
        }
        in.travel = new double[in.n + 1][in.n + 1];
        for (int i = 0; i <= in.n; i++) {
            for (int j = 0; j <= in.n; j++) {
                in.travel[i][j] = Math.abs(positions[i] - positions[j]);
            }
        }
        score(in, "nearest neighbour: 3 delivered, wait 18.0", NearestNeighbor.solve(in), 3, 18.0);
        score(in, "Approach A: 5 delivered, wait 21.4",
                RunExperiments.fromSolver(new SolverA().solve(in.toSolverInstance())), 5, 21.4);
        score(in, "Approach B: 5 delivered, wait 27.0",
                RunExperiments.fromSolver(new SolverB().solve(in.toSolverInstance())), 5, 27.0);
        score(in, "deadline-order delivered route", List.of(1, 5, 2, 4), 4, 27.75);
        score(in, "first improvement delivered route", List.of(1, 5, 3, 4, 2), 5, 23.8);
        score(in, "second improvement delivered route", List.of(3, 1, 5, 4, 2), 5, 21.4);
        permutations = 0;
        allDelivered = 0;
        bestWait = Double.POSITIVE_INFINITY;
        enumerate(in, new ArrayList<>(), new boolean[in.n + 1]);
        check("120 permutations; 15 deliver everyone; best wait 21.4",
                permutations == 120 && allDelivered == 15 && Math.abs(bestWait - 21.4) < Common.EPS);
        System.out.println("ALL PRESENTATION EXAMPLE CHECKS PASSED.");
    }
}
