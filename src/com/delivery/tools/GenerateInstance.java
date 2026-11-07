package com.delivery.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Makes instance files from MY.txt.
 *
 *   java ... GenerateInstance data/raw/MY.txt --n 8 --seed 1 --tight 1.0     one file
 *   java ... GenerateInstance data/raw/MY.txt --all                            the full grid (540 files)
 *   optional: --out data/processed
 *
 * Randomness: java.util.Random seeded from (n, seed). The tightness only scales the shelf lives, so
 * n8_s1_t0.5, n8_s1_t1.0 and n8_s1_t1.5 have the same customers, order times and food.
 * When n > 33, the 33 locations are reused (whole shuffled rounds, then a partial one).
 */
public final class GenerateInstance {

    public static Instance make(Places places, int n, int seed, double tight) {
        Random rnd = new Random(seed * 1_000_003L + n);
        List<Places.Place> pool = places.customers;
        List<Places.Place> picked = new ArrayList<>();
        while (picked.size() < n) {
            List<Places.Place> round = new ArrayList<>(pool);
            Collections.shuffle(round, rnd);
            for (Places.Place p : round) {
                if (picked.size() < n) picked.add(p);
            }
        }
        // order times: uniform in the 10 minutes before departure, shifted so the last order is at t = 0
        double[] raw = new double[n];
        double max = -Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            raw[k] = -Common.ORDER_WINDOW_MIN * rnd.nextDouble();
            max = Math.max(max, raw[k]);
        }
        Instance in = new Instance();
        in.n = n;
        in.seed = seed;
        in.tight = tight;
        in.restaurant = places.restaurant;
        for (int k = 0; k < n; k++) {
            int f = rnd.nextInt(Common.FOODS.length);
            double o = Math.round((raw[k] - max) * 100.0) / 100.0;
            Places.Place p = picked.get(k);
            in.customers.add(new Instance.Cust(k + 1, p.geonameid(), p.name(), p.lat(), p.lon(), o,
                    Common.FOODS[f], Common.SHELF_MIN[f] * tight));
        }
        double[] lat = new double[n + 1], lon = new double[n + 1];
        lat[0] = places.restaurant.lat();
        lon[0] = places.restaurant.lon();
        for (int k = 1; k <= n; k++) {
            lat[k] = picked.get(k - 1).lat();
            lon[k] = picked.get(k - 1).lon();
        }
        in.travel = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                in.travel[i][j] = i == j ? 0.0 : Common.travelMin(Common.haversineKm(lat[i], lon[i], lat[j], lon[j]));
            }
        }
        return in;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: GenerateInstance MY.txt (--n N --seed S [--tight T] | --all) [--out DIR]");
            System.exit(2);
        }
        Path my = Path.of(args[0]);
        int n = -1, seed = -1;
        double tight = 1.0;
        boolean all = false;
        Path out = Path.of("data", "processed");
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--n" -> n = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Integer.parseInt(args[++i]);
                case "--tight" -> tight = Double.parseDouble(args[++i]);
                case "--all" -> all = true;
                case "--out" -> out = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        Places places = Places.load(my);
        System.out.printf("MY.txt: %d rows; class P within %.0f km: %d; after dropping duplicate coordinates: %d%n",
                places.rows, Common.RADIUS_KM, places.withinRadius, places.customers.size());
        int count = 0;
        if (all) {
            for (int nn : Common.N_VALUES)
                for (double t : Common.TIGHT_VALUES)
                    for (int s = 1; s <= Common.SEEDS; s++) {
                        make(places, nn, s, t).write(out);
                        count++;
                    }
        } else {
            if (n < 1 || seed < 0) throw new IllegalArgumentException("need --n and --seed, or --all");
            make(places, n, seed, tight).write(out);
            count++;
        }
        System.out.println("wrote " + count + " instance file(s) to " + out);
    }
}
