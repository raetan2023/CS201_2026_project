package com.delivery.tools;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Rebuilds the template's worked example (n = 6, seed 10) from MY.txt and checks the shared tools
 * against the numbers printed in the template. Java's random numbers differ from Python's, so this
 * instance is built from the template's table, not regenerated; it is also saved as
 * data/processed/example_n6_s10.json.
 *
 *   java ... CheckWorkedExample data/raw/MY.txt
 */
public final class CheckWorkedExample {
    static final String[] NAMES = {"Golden Triangle", "Kampong Baharu", "Sentul", "Pudu", "Kuala Lumpur Village", "Taman Maluri"};
    static final double[] ORDER = {-3.5, -3.2, -5.3, 0.0, -7.8, -3.2};
    static final String[] FOOD = {"ice_cream", "ice_cream", "rice", "noodles", "ice_cream", "noodles"};

    public static Instance build(Places places) {
        Instance in = new Instance();
        in.n = NAMES.length;
        in.seed = 10;
        in.tight = 1.0;
        in.restaurant = places.restaurant;
        double[] lat = new double[in.n + 1], lon = new double[in.n + 1];
        lat[0] = places.restaurant.lat();
        lon[0] = places.restaurant.lon();
        for (int k = 0; k < in.n; k++) {
            final String nm = NAMES[k];
            Places.Place p = places.customers.stream().filter(x -> x.name().equals(nm)).findFirst().orElseThrow();
            int f = List.of(Common.FOODS).indexOf(FOOD[k]);
            in.customers.add(new Instance.Cust(k + 1, p.geonameid(), p.name(), p.lat(), p.lon(), ORDER[k], FOOD[k], Common.SHELF_MIN[f]));
            lat[k + 1] = p.lat();
            lon[k + 1] = p.lon();
        }
        in.travel = new double[in.n + 1][in.n + 1];
        for (int i = 0; i <= in.n; i++)
            for (int j = 0; j <= in.n; j++)
                in.travel[i][j] = i == j ? 0 : Common.travelMin(Common.haversineKm(lat[i], lon[i], lat[j], lon[j]));
        return in;
    }

    static int fails = 0;

    static void expect(String what, Object got, Object want) {
        boolean ok = got.equals(want);
        if (!ok) fails++;
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what + ": " + got + (ok ? "" : "  (template: " + want + ")"));
    }

    static String f1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    public static void main(String[] args) throws Exception {
        Places places = Places.load(Path.of(args.length > 0 ? args[0] : "data/raw/MY.txt"));
        Instance in = build(places);
        Path out = Path.of(args.length > 1 ? args[1] : "data/processed");
        java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.writeString(out.resolve("example_n6_s10.json"), in.toJson());
        System.out.println("Travel from restaurant (template: 2.6, 6.9, 14.8, 5.3, 8.5, 8.0; Kampong Baharu is 6.85, rounded differently):");
        StringBuilder sb = new StringBuilder("  ");
        for (int k = 1; k <= in.n; k++) sb.append(f1(in.travel[0][k])).append(" ");
        System.out.println(sb);

        List<Integer> b1 = BruteForce.solve(in);
        Evaluate.Score s1 = Evaluate.evaluate(in, b1);
        System.out.println("Best route: " + b1 + "  " + s1);
        expect("best route", b1, List.of(1, 2, 5, 4, 6, 3));
        expect("best avg wait", f1(s1.avgWait()), "29.0");
        expect("best longest wait", f1(s1.longestWait()), "62.7");

        List<Integer> nn = NearestNeighbor.solve(in);
        Evaluate.Score sn = Evaluate.evaluate(in, nn);
        System.out.println("Nearest neighbour: " + nn + "  " + sn);
        expect("NN route", nn, List.of(1, 2, 4, 6, 3));
        expect("NN avg wait", f1(sn.avgWait()), "21.9");
        expect("NN longest wait", f1(sn.longestWait()), "49.8");

        expect("deadline of Sentul", f1(in.deadline(3)), "84.7");
        System.out.println(fails == 0 ? "ALL CHECKS PASSED" : fails + " CHECK(S) FAILED");
        if (fails > 0) System.exit(1);
    }
}
