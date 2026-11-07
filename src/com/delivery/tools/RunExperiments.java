package com.delivery.tools;

import com.delivery.routing.DeliveryInstance;
import com.delivery.routing.RouteResult;
import com.delivery.routing.SolverA;
import com.delivery.routing.SolverB;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Runs every method on every instance file and writes
 *   results/raw_results.csv   one row per (instance, method)
 *   results/summary.csv       mean and spread per (n, tight, method)
 *
 * Methods (names as in the presentation):
 *   NN          nearest neighbour baseline (NearestNeighbor.java)
 *   A_plan_fix  Approach A: deadline order, then improve by moving stops   (code: SolverB, experiments/approach_B)
 *   B_greedy    Approach B: greedy with one-step lookahead                 (code: SolverA, experiments/approach_A)
 *   BRUTE       exact best, n <= 10 only (BruteForce.java)
 *
 * Every route is re-scored with Evaluate.java (not trusted from the solver) and must have late = 0.
 *
 *   java ... RunExperiments [--data data/processed] [--out results]
 */
public final class RunExperiments {

    record Method(String name, Function<Instance, List<Integer>> run) {}

    static List<Integer> fromSolver(RouteResult r) {
        List<Integer> out = new ArrayList<>();
        for (int id : r.route()) out.add(id + 1);
        return out;
    }

    static final List<Method> METHODS = List.of(
            new Method("NN", NearestNeighbor::solve),
            new Method("A_plan_fix", in -> fromSolver(new SolverB().solve(in.toSolverInstance()))),
            new Method("B_greedy", in -> fromSolver(new SolverA().solve(in.toSolverInstance()))),
            new Method("BRUTE", BruteForce::solve));

    record Row(int n, double tight, int seed, String method, Evaluate.Score s, double ms,
               int bestDel, double bestWait) {}

    static int reps(int n, String method) {
        if (method.equals("BRUTE")) return 1;
        return n <= 10 ? 20 : n <= 20 ? 10 : n <= 50 ? 3 : 1;
    }

    /** Median wall time over reps runs, in ms. */
    static double time(Runnable r, int reps) {
        double[] t = new double[reps];
        for (int i = 0; i < reps; i++) {
            long t0 = System.nanoTime();
            r.run();
            t[i] = (System.nanoTime() - t0) / 1e6;
        }
        Arrays.sort(t);
        return t[reps / 2];
    }

    public static void main(String[] args) throws Exception {
        Path data = Path.of("data", "processed"), out = Path.of("results");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--data")) data = Path.of(args[++i]);
            else if (args[i].equals("--out")) out = Path.of(args[++i]);
        }
        Files.createDirectories(out);

        List<Instance> instances = new ArrayList<>();
        for (int n : Common.N_VALUES)
            for (double t : Common.TIGHT_VALUES)
                for (int s = 1; s <= Common.SEEDS; s++) {
                    Path f = data.resolve(Instance.fileName(n, s, t));
                    if (!Files.exists(f)) throw new IllegalStateException("missing " + f + " (run GenerateInstance --all first)");
                    instances.add(Instance.read(f));
                }
        System.out.println("instances: " + instances.size());

        // warm-up so the JIT compiler does not distort the first timings
        for (int k = 0; k < 3; k++)
            for (Instance in : instances.subList(0, 40))
                for (Method m : METHODS)
                    if (!m.name().equals("BRUTE")) m.run().apply(in);

        List<Row> rows = new ArrayList<>();
        int done = 0;
        for (Instance in : instances) {
            {
                int bestDel = -1;
                double bestWait = Double.NaN;
                for (Method m : METHODS) {
                    if (m.name().equals("BRUTE") && in.n > Common.BRUTE_MAX_N) continue;
                    List<List<Integer>> holder = new ArrayList<>(List.of(List.of()));
                    double ms = time(() -> holder.set(0, m.run().apply(in)), reps(in.n, m.name()));
                    Evaluate.Score sc = Evaluate.evaluate(in, holder.get(0));
                    if (sc.late() != 0) throw new IllegalStateException(m.name() + " returned a late order on " + in.n + "/" + in.seed);
                    rows.add(new Row(in.n, in.tight, in.seed, m.name(), sc, ms, -1, Double.NaN));
                    if (m.name().equals("BRUTE")) {
                        bestDel = sc.delivered();
                        bestWait = sc.avgWait();
                    }
                }
                if (bestDel >= 0) {   // fill in the gap to the exact best for this instance
                    for (int k = rows.size() - 4; k < rows.size(); k++) {
                        Row r = rows.get(k);
                        rows.set(k, new Row(r.n(), r.tight(), r.seed(), r.method(), r.s(), r.ms(), bestDel, bestWait));
                        if (r.s().delivered() > bestDel) throw new IllegalStateException("beat brute force?");
                    }
                }
            }
            if (++done % 60 == 0) System.out.println("  " + done + "/" + instances.size());
        }

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out.resolve("raw_results.csv")))) {
            w.println("n,tight,seed,method,delivered,cancelled,late,avg_wait,longest_wait,runtime_ms,best_delivered,extra_cancelled_vs_best,avg_wait_gap_vs_best");
            for (Row r : rows) {
                String gapC = r.bestDel() < 0 ? "" : String.valueOf(r.bestDel() - r.s().delivered());
                String gapW = r.bestDel() < 0 || r.bestDel() != r.s().delivered() ? "" : f(r.s().avgWait() - r.bestWait(), 4);
                w.printf(Locale.ROOT, "%d,%.1f,%d,%s,%d,%d,%d,%.4f,%.4f,%.4f,%s,%s,%s%n", r.n(), r.tight(),
                        r.seed(), r.method(), r.s().delivered(), r.s().cancelled(), r.s().late(), r.s().avgWait(),
                        r.s().longestWait(), r.ms(), r.bestDel() < 0 ? "" : String.valueOf(r.bestDel()), gapC, gapW);
            }
        }

        Map<String, List<Row>> groups = new LinkedHashMap<>();
        for (Row r : rows) {
            groups.computeIfAbsent(r.n() + "|" + r.tight() + "|" + r.method(), k -> new ArrayList<>()).add(r);
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out.resolve("summary.csv")))) {
            w.println("n,tight,method,runs,cancelled_mean,cancelled_sd,avg_wait_mean,avg_wait_sd,longest_wait_mean,runtime_ms_mean,runtime_ms_max,extra_cancelled_vs_best_mean,pct_runs_as_many_delivered_as_best,pct_runs_exactly_best");
            for (List<Row> g : groups.values()) {
                Row r0 = g.get(0);
                double[] canc = g.stream().mapToDouble(r -> r.s().cancelled()).toArray();
                double[] wait = g.stream().mapToDouble(r -> r.s().avgWait()).toArray();
                double[] lw = g.stream().mapToDouble(r -> r.s().longestWait()).toArray();
                double[] ms = g.stream().mapToDouble(Row::ms).toArray();
                String gap = "", pctDel = "", pctExact = "";
                if (r0.bestDel() >= 0) {
                    gap = f(g.stream().mapToDouble(r -> r.bestDel() - r.s().delivered()).average().orElse(0), 3);
                    pctDel = f(100.0 * g.stream().filter(r -> r.s().delivered() == r.bestDel()).count() / g.size(), 1);
                    pctExact = f(100.0 * g.stream().filter(r -> r.s().delivered() == r.bestDel()
                            && Math.abs(r.s().avgWait() - r.bestWait()) < 1e-6).count() / g.size(), 1);
                }
                w.printf(Locale.ROOT, "%d,%.1f,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", r0.n(), r0.tight(), r0.method(),
                        g.size(), f(mean(canc), 3), f(sd(canc), 3), f(mean(wait), 2), f(sd(wait), 2), f(mean(lw), 2),
                        f(mean(ms), 4), f(Arrays.stream(ms).max().orElse(0), 4), gap, pctDel, pctExact);
            }
        }
        System.out.println("wrote " + out.resolve("raw_results.csv") + " (" + rows.size() + " rows) and " + out.resolve("summary.csv"));
    }

    static double mean(double[] a) {
        return Arrays.stream(a).average().orElse(Double.NaN);
    }

    static double sd(double[] a) {
        double m = mean(a), s = 0;
        for (double x : a) s += (x - m) * (x - m);
        return a.length > 1 ? Math.sqrt(s / (a.length - 1)) : 0;
    }

    static String f(double d, int dp) {
        return String.format(Locale.ROOT, "%." + dp + "f", d);
    }
}
