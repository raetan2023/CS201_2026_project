# Approach B — Deadline order, then improve by moving stops

Full explanation with worked traces: [HOW_IT_WORKS.md](HOW_IT_WORKS.md).

Requires Java 17+. All orders are known at t = 0 (static model).

Source: `src/com/delivery/routing/` (SolverB.java, Demo.java, plus the
Customer / DeliveryInstance / RouteResult model, identical to Approach A's).

Compile and run the sanity checks from this folder (`experiments/approach_B/`):

    javac -encoding UTF-8 -d out src/com/delivery/routing/*.java
    java -cp out com.delivery.routing.Demo

Indexing: travelMatrix is (n+1)x(n+1); index 0 = restaurant, index i+1 = customer i.

Two steps: (1) sort all orders by deadline, most urgent first; (2) repeatedly try moving
one order to another position in the plan, keep the best move if it delivers more orders
(or the same number with a lower average wait), stop when no move helps.
A plan is scored by driving it: deliver if on time, otherwise cancel.
Time O(n^3) per improvement pass, space O(n).
