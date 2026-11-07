# Approach A — Greedy with lookahead guards

Full explanation with worked traces: [HOW_IT_WORKS.md](HOW_IT_WORKS.md).

Requires Java 17+. All orders are known at t = 0 (static model).

Source: `src/com/delivery/routing/` (SolverA.java, Demo.java, plus the
Customer / DeliveryInstance / RouteResult model).

Compile and run the sanity checks from this folder (`experiments/approach_A/`):

    javac -encoding UTF-8 -d out src/com/delivery/routing/*.java
    java -cp out com.delivery.routing.Demo

Indexing: travelMatrix is (n+1)x(n+1); index 0 = restaurant, index i+1 = customer i.

Selection rules, in strict order: (1) skip unreachable orders, (2) fewest orders lost,
(3) shortest travel, (4) least slack, (5) lowest id. Time O(n^3), space O(n).
