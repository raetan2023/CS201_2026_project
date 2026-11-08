package com.delivery.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scores a route (customer ids 1..n in delivery order; ids left out are cancelled).
 * The rider drives to every stop in the route; a stop reached after its deadline counts as late.
 * A valid solution always has late = 0.
 *
 *   java ... Evaluate data/processed/n8_s1_t1.0.json 3 1 4 2
 */
public final class Evaluate {

    public record Score(int delivered, int cancelled, int late, double avgWait, double longestWait,
                        List<Double> arrivals) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "delivered=%d cancelled=%d late=%d avg_wait=%.2f longest_wait=%.2f",
                    delivered, cancelled, late, avgWait, longestWait);
        }
    }

    public static Score evaluate(Instance in, List<Integer> route) {
        Set<Integer> seen = new HashSet<>();
        int cur = 0, late = 0, onTime = 0;
        double t = 0, totalWait = 0, longest = 0;
        List<Double> arr = new ArrayList<>();
        for (int id : route) {
            if (id < 1 || id > in.n || !seen.add(id)) throw new IllegalArgumentException("bad or repeated id " + id);
            t += in.travel[cur][id];
            cur = id;
            arr.add(t);
            if (t > in.deadline(id) + Common.EPS) {
                late++;
            } else {
                onTime++;
                double w = t - in.customers.get(id - 1).orderTime();
                totalWait += w;
                longest = Math.max(longest, w);
            }
        }
        return new Score(onTime, in.n - route.size(), late, onTime == 0 ? 0 : totalWait / onTime, longest, arr);
    }

    public static void main(String[] args) throws Exception {
        Instance in = Instance.read(Path.of(args[0]));
        List<Integer> route = new ArrayList<>();
        for (int i = 1; i < args.length; i++) route.add(Integer.parseInt(args[i]));
        Score s = evaluate(in, route);
        System.out.println(s);
        for (int k = 0; k < route.size(); k++) {
            int id = route.get(k);
            Instance.Cust c = in.customers.get(id - 1);
            System.out.printf(Locale.ROOT, "  %2d %-24s arrive %6.2f  deadline %6.2f  wait %6.2f%s%n", id, c.name(),
                    s.arrivals().get(k), in.deadline(id), s.arrivals().get(k) - c.orderTime(),
                    s.arrivals().get(k) > in.deadline(id) + Common.EPS ? "  LATE" : "");
        }
    }
}
