package com.delivery.tools;

/**
 * Fixed parameters shared by every experiment (from the group template).
 * Change them only if the whole group agrees.
 */
public final class Common {
    private Common() {}

    public static final String RESTAURANT_ID = "7284944";   // Bukit Bintang Plaza, class S / MALL
    public static final double RADIUS_KM = 6.0;              // customers: class P within 6 km
    public static final double SPEED_KMH = 20.0;             // straight-line distance / 20 km/h
    public static final String[] FOODS = {"ice_cream", "noodles", "rice"};
    public static final double[] SHELF_MIN = {30.0, 60.0, 90.0};
    public static final double ORDER_WINDOW_MIN = 10.0;      // orders placed in the 10 min before leaving
    public static final double EPS = 1e-9;                   // arriving exactly on the deadline is on time

    public static final int[] N_VALUES = {5, 8, 10, 20, 50, 100};
    public static final double[] TIGHT_VALUES = {0.5, 1.0, 1.5};
    public static final int SEEDS = 30;
    public static final int BRUTE_MAX_N = 10;

    /** Great-circle (haversine) distance in km, mean Earth radius. Reproduces the template's worked
     *  example: same best routes, average and longest waits (see CheckWorkedExample). */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dp / 2) * Math.sin(dp / 2)
                 + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    public static double travelMin(double km) {
        return km / SPEED_KMH * 60.0;
    }

    /** Deadline = order time + shelf life (the food must not spoil). */
    public static double deadline(double orderTime, double shelfLife) {
        return orderTime + shelfLife;
    }
}
