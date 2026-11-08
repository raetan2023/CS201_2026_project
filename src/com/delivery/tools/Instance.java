package com.delivery.tools;

import com.delivery.routing.Customer;
import com.delivery.routing.DeliveryInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One ready-to-run test case, stored as JSON in data/processed:
 *
 * {"n": 8, "seed": 1, "tight": 1.0,
 *  "restaurant": {"geonameid": "7284944", "name": "Bukit Bintang Plaza", "lat": .., "lon": ..},
 *  "customers": [{"id": 1, "geonameid": "11054400", "name": "Golden Triangle", "lat": .., "lon": ..,
 *                 "order_time": -3.5, "food": "ice_cream", "shelf_life": 30.0}, ...],
 *  "travel_min": [[0, 2.6250, ...], ...]}       index 0 = restaurant, index k = customer k
 *
 * order_time, food and shelf_life are the attributes we add; everything else comes from MY.txt.
 * The deadline is not stored: it is order_time + shelf_life, worked out when scoring.
 */
public final class Instance {
    public record Cust(int id, String geonameid, String name, double lat, double lon,
                       double orderTime, String food, double shelfLife) {}

    public int n, seed;
    public double tight;
    public Places.Place restaurant;
    public List<Cust> customers = new ArrayList<>();
    public double[][] travel;          // (n+1) x (n+1), minutes

    public double deadline(int id) {
        Cust c = customers.get(id - 1);
        return Common.deadline(c.orderTime(), c.shelfLife());
    }

    /** Adapter to the solvers' model: solver customer i = instance customer i + 1. */
    public DeliveryInstance toSolverInstance() {
        List<Customer> cs = new ArrayList<>();
        for (Cust c : customers) {
            cs.add(new Customer(c.id() - 1, c.orderTime(), c.shelfLife(), c.food()));
        }
        double[][] m = new double[n + 1][];
        for (int i = 0; i <= n; i++) m[i] = travel[i].clone();
        return new DeliveryInstance(m, cs);
    }

    public static String fileName(int n, int seed, double tight) {
        return String.format(Locale.ROOT, "n%d_s%d_t%.1f.json", n, seed, tight);
    }

    // ---------- JSON out ----------
    public String toJson() {
        StringBuilder s = new StringBuilder();
        s.append("{\"n\": ").append(n).append(", \"seed\": ").append(seed)
         .append(", \"tight\": ").append(fmt(tight, 1)).append(",\n");
        s.append(" \"restaurant\": {\"geonameid\": \"").append(restaurant.geonameid()).append("\", \"name\": \"")
         .append(esc(restaurant.name())).append("\", \"lat\": ").append(fmt(restaurant.lat(), 5))
         .append(", \"lon\": ").append(fmt(restaurant.lon(), 5)).append("},\n \"customers\": [\n");
        for (int i = 0; i < customers.size(); i++) {
            Cust c = customers.get(i);
            s.append("  {\"id\": ").append(c.id()).append(", \"geonameid\": \"").append(c.geonameid())
             .append("\", \"name\": \"").append(esc(c.name())).append("\", \"lat\": ").append(fmt(c.lat(), 5))
             .append(", \"lon\": ").append(fmt(c.lon(), 5)).append(", \"order_time\": ").append(fmt(c.orderTime(), 2))
             .append(", \"food\": \"").append(c.food()).append("\", \"shelf_life\": ").append(fmt(c.shelfLife(), 1))
             .append("}").append(i + 1 < customers.size() ? ",\n" : "\n");
        }
        s.append(" ],\n \"travel_min\": [\n");
        for (int i = 0; i <= n; i++) {
            s.append("  [");
            for (int j = 0; j <= n; j++) {
                if (j > 0) s.append(", ");
                s.append(fmt(travel[i][j], 4));
            }
            s.append(i < n ? "],\n" : "]\n");
        }
        s.append(" ]\n}\n");
        return s.toString();
    }

    public void write(Path dir) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName(n, seed, tight)), toJson(), StandardCharsets.UTF_8);
    }

    static String fmt(double d, int dp) {
        String s = String.format(Locale.ROOT, "%." + dp + "f", d);
        return s.equals("-0." + "0".repeat(dp)) ? s.substring(1) : s;
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---------- JSON in ----------
    @SuppressWarnings("unchecked")
    public static Instance read(Path file) throws IOException {
        Map<String, Object> o = (Map<String, Object>) new Json(Files.readString(file, StandardCharsets.UTF_8)).value();
        Instance in = new Instance();
        in.n = ((Number) o.get("n")).intValue();
        in.seed = o.containsKey("seed") ? ((Number) o.get("seed")).intValue() : 0;
        in.tight = o.containsKey("tight") ? ((Number) o.get("tight")).doubleValue() : 1.0;
        Map<String, Object> r = (Map<String, Object>) o.get("restaurant");
        in.restaurant = new Places.Place((String) r.get("geonameid"), (String) r.get("name"), num(r, "lat"), num(r, "lon"));
        for (Object co : (List<Object>) o.get("customers")) {
            Map<String, Object> c = (Map<String, Object>) co;
            in.customers.add(new Cust(((Number) c.get("id")).intValue(), (String) c.get("geonameid"), (String) c.get("name"),
                    num(c, "lat"), num(c, "lon"), num(c, "order_time"), (String) c.get("food"), num(c, "shelf_life")));
        }
        List<Object> rows = (List<Object>) o.get("travel_min");
        in.travel = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = (List<Object>) rows.get(i);
            in.travel[i] = new double[row.size()];
            for (int j = 0; j < row.size(); j++) in.travel[i][j] = ((Number) row.get(j)).doubleValue();
        }
        if (in.customers.size() != in.n || in.travel.length != in.n + 1) {
            throw new IllegalStateException(file + ": n does not match customers / travel_min");
        }
        return in;
    }

    private static double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? Double.NaN : ((Number) v).doubleValue();
    }

    /** Minimal JSON parser: objects, arrays, strings, numbers, true/false/null. */
    static final class Json {
        private final String s;
        private int i;

        Json(String s) { this.s = s; }

        Object value() {
            ws();
            char c = s.charAt(i);
            if (c == '{') return obj();
            if (c == '[') return arr();
            if (c == '"') return str();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numv();
        }

        private Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String k = str();
                ws();
                i++;            // ':'
                m.put(k, value());
                ws();
                if (s.charAt(i++) == '}') return m;
            }
        }

        private List<Object> arr() {
            List<Object> a = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return a; }
            while (true) {
                a.add(value());
                ws();
                if (s.charAt(i++) == ']') return a;
            }
        }

        private String str() {
            StringBuilder b = new StringBuilder();
            i++;
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    if (e == 'u') { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    else b.append(e == 'n' ? '\n' : e == 't' ? '\t' : e);
                } else b.append(c);
            }
            i++;
            return b.toString();
        }

        private Double numv() {
            int j = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(j, i));
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
