package com.delivery.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads MY.txt (GeoNames, tab separated, 19 columns, no header) and returns the restaurant and the
 * customer locations. MY.txt is never edited.
 *
 * Columns used: 0 GeoNameID, 1 name, 4 latitude, 5 longitude, 6 feature class.
 * Customers: feature class P within 6 km of the restaurant; rows with exactly the same coordinates
 * are dropped, keeping the lowest GeoNameID.
 */
public final class Places {
    public record Place(String geonameid, String name, double lat, double lon) {}

    public final Place restaurant;
    public final List<Place> customers;
    public final int rows, withinRadius;

    private Places(Place r, List<Place> c, int rows, int within) {
        this.restaurant = r;
        this.customers = c;
        this.rows = rows;
        this.withinRadius = within;
    }

    public static Places load(Path myTxt) throws IOException {
        List<String> lines = Files.readAllLines(myTxt, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (String l : lines) {
            if (!l.isEmpty()) rows.add(l.split("\t", -1));
        }
        Place r = null;
        for (String[] f : rows) {
            if (f[0].equals(Common.RESTAURANT_ID)) {
                r = new Place(f[0], f[1], Double.parseDouble(f[4]), Double.parseDouble(f[5]));
            }
        }
        if (r == null) throw new IllegalStateException("restaurant " + Common.RESTAURANT_ID + " not in " + myTxt);
        List<Place> near = new ArrayList<>();
        for (String[] f : rows) {
            if (!"P".equals(f[6])) continue;
            double lat = Double.parseDouble(f[4]), lon = Double.parseDouble(f[5]);
            if (Common.haversineKm(r.lat(), r.lon(), lat, lon) <= Common.RADIUS_KM) {
                near.add(new Place(f[0], f[1], lat, lon));
            }
        }
        near.sort(Comparator.comparingLong(p -> Long.parseLong(p.geonameid())));
        Set<String> seen = new HashSet<>();
        List<Place> keep = new ArrayList<>();
        for (Place p : near) {
            if (seen.add(f4(p.lat()) + "," + f4(p.lon()))) keep.add(p);
        }
        return new Places(r, keep, rows.size(), near.size());
    }

    private static String f4(double d) {
        return String.format(java.util.Locale.ROOT, "%.6f", d);
    }
}
