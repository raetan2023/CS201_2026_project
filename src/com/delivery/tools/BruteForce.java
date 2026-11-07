package com.delivery.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact best solution for small n (the template uses it for n <= 10).
 * Goal order: most delivered, then lowest total wait (= lowest sum of arrival times).
 *
 * Depth-first search over on-time routes. Only on-time stops are extended, so every leaf is valid.
 * Two prunings keep it fast and exact:
 *   - bound: delivered so far + orders still directly reachable can't beat the best -> stop
 *     (an order not reachable directly is not reachable later either: triangle inequality,
 *     which straight-line travel times satisfy);
 *   - dominance: for the same (set of visited customers, last customer), a state that is no earlier
 *     and has no smaller wait sum than one already seen can't lead anywhere better -> stop.
 *
 *   java ... BruteForce data/processed/n8_s1_t1.0.json
 */
public final class BruteForce {
    private final Instance in;
    private final double[] dl, ot;
    private int bestDel = -1;
    private double bestSum = Double.MAX_VALUE;
    private int[] bestRoute = new int[0];
    private final int[] cur;
    private final Map<Long, List<double[]>> seen = new HashMap<>();
    private final boolean dominance;

    private BruteForce(Instance in, boolean dominance) {
        this.in = in;
        this.dominance = dominance;
        dl = new double[in.n + 1];
        ot = new double[in.n + 1];
        for (int k = 1; k <= in.n; k++) {
            dl[k] = in.deadline(k);
            ot[k] = in.customers.get(k - 1).orderTime();
        }
        cur = new int[in.n];
    }

    public static List<Integer> solve(Instance in) {
        if (in.n > 20) throw new IllegalArgumentException("brute force is only for small n");
        BruteForce b = new BruteForce(in, true);
        b.dfs(0, 0L, 0, 0.0, 0.0);
        List<Integer> r = new ArrayList<>();
        for (int id : b.bestRoute) r.add(id);
        return r;
    }

    private void dfs(int last, long mask, int depth, double t, double sumWait) {
        if (depth > bestDel || (depth == bestDel && sumWait < bestSum - Common.EPS)) {
            bestDel = depth;
            bestSum = sumWait;
            bestRoute = java.util.Arrays.copyOf(cur, depth);
        }
        int reachable = 0;
        for (int k = 1; k <= in.n; k++) {
            if ((mask >> k & 1) == 0 && t + in.travel[last][k] <= dl[k] + Common.EPS) reachable++;
        }
        if (depth + reachable < bestDel) return;
        if (reachable == 0) return;
        if (dominance && depth > 0) {
            long key = mask * 64 + last;
            List<double[]> list = seen.computeIfAbsent(key, x -> new ArrayList<>());
            for (double[] p : list) {
                if (p[0] <= t + Common.EPS && p[1] <= sumWait + Common.EPS) return;
            }
            list.removeIf(p -> p[0] >= t - Common.EPS && p[1] >= sumWait - Common.EPS);
            list.add(new double[]{t, sumWait});
        }
        for (int k = 1; k <= in.n; k++) {
            if ((mask >> k & 1) != 0) continue;
            double a = t + in.travel[last][k];
            if (a > dl[k] + Common.EPS) continue;
            cur[depth] = k;
            dfs(k, mask | (1L << k), depth + 1, a, sumWait + a - ot[k]);
        }
    }

    /** Plain exhaustive search without the dominance rule, for checking the pruned version. */
    public static List<Integer> solvePlain(Instance in) {
        BruteForce b = new BruteForce(in, false);
        b.dfs(0, 0L, 0, 0.0, 0.0);
        List<Integer> r = new ArrayList<>();
        for (int id : b.bestRoute) r.add(id);
        return r;
    }

    public static void main(String[] args) throws Exception {
        Instance in = Instance.read(Path.of(args[0]));
        long t0 = System.nanoTime();
        List<Integer> r = solve(in);
        double ms = (System.nanoTime() - t0) / 1e6;
        System.out.println("route " + r);
        System.out.println(Evaluate.evaluate(in, r) + String.format(java.util.Locale.ROOT, " (%.1f ms)", ms));
    }
}
