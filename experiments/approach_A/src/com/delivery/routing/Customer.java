package com.delivery.routing;

/**
 * One food order.
 *
 * @param id         customer id, 0..n-1 (matrix index is id + 1; index 0 is the restaurant)
 * @param orderTime  minutes after the rider departs (static model: all orders known at t = 0)
 * @param shelfLife  minutes the food stays acceptable (ice cream 30, noodles 60, rice 90)
 * @param foodType   label only, for reporting
 */
public record Customer(int id, double orderTime, double shelfLife, String foodType) {

    /** Latest acceptable arrival time. */
    public double deadline() {
        return orderTime + shelfLife;
    }
}
