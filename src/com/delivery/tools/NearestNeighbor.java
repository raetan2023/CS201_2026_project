package com.delivery.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Baseline from the template: classic nearest neighbour over all customers (deadlines ignored),
 * then drive that order and drop every order that would arrive late (the rider skips it, so it
 * costs no time). The result is always valid (late = 0).
 *
 *   java ... NearestNeighbor data/processed/n8_s1_t1.0.json
 */
public final class NearestNeighbor {

    public static List<Integer> tour(Instance in) {
        boolean[] vis = new boolean[in.n + 1];
        List<Integer> order = new ArrayList<>();
        int cur = 0;
        for (int step = 0; step < in.n; step++) {
            int best = -1;
            for (int k = 1; k <= in.n; k++) {
                if (!vis[k] && (best == -1 || in.travel[cur][k] < in.travel[cur][best])) best = k;
            }
            vis[best] = true;
            order.add(best);
            cur = best;
        }
        return order;
    }

    public static List<Integer> solve(Instance in) {
        List<Integer> route = new ArrayList<>();
        int cur = 0;
        double t = 0;
        for (int k : tour(in)) {
            double a = t + in.travel[cur][k];
            if (a <= in.deadline(k) + Common.EPS) {
                route.add(k);
                t = a;
                cur = k;
            }
        }
        return route;
    }

    public static void main(String[] args) throws Exception {
        Instance in = Instance.read(Path.of(args[0]));
        List<Integer> r = solve(in);
        System.out.println("route " + r);
        System.out.println(Evaluate.evaluate(in, r));
    }
}
