# How Approach A works

Approach A is a **greedy route builder with a one-step lookahead**. One rider leaves the
restaurant with every order on board and picks the next customer one stop at a time. At each
stop it asks: *"If I go here next, how many other orders do I doom?"* It never reconsiders a
stop once it has been chosen.

All code lives in `src/com/delivery/routing/`. See `README.md` for compile/run commands.

---

## 1. The problem the code solves

| Classic TSP | Our variant (as coded) |
|---|---|
| Every city is equally important | Every customer has a **deadline** (food spoils) |
| Only the visit order matters | The **arrival time** at each stop matters |
| Single goal: shortest tour | 1st goal: **most on-time deliveries**; 2nd goal: **low mean wait** |
| Tour returns to the start | **Open path**: the route ends at the last customer |
| Every city must be visited | Orders that can no longer arrive on time are **dropped** |

**Assumptions (static model)**

- The rider departs the restaurant at `t = 0` with all orders. Every order is known up front.
- `deadline = orderTime + shelfLife`. Example shelf lives: ice cream 30, noodles 60, rice 90 minutes.
- `wait = arrival − orderTime`, measured only for delivered customers.
- Travel times are in minutes and come from a precomputed matrix.

---

## 2. Data structures

| File | What it holds |
|---|---|
| `Customer.java` | A record `(id, orderTime, shelfLife, foodType)` with `deadline()` |
| `DeliveryInstance.java` | The travel matrix plus the customer list (one problem instance) |
| `RouteResult.java` | The output: `route`, `arrivals`, `dropped`, `meanWait` |
| `SolverA.java` | The algorithm |
| `Demo.java` | Hand-worked sanity checks and a nearest-neighbour baseline |

**Indexing convention.** The travel matrix is `(n+1) × (n+1)`:

```
index 0      = restaurant (depot)
index i + 1  = customer with id i        →  DeliveryInstance.node(i)
```

Without this, "node 0" would mean both the restaurant and customer 0.
The `DeliveryInstance` constructor rejects a matrix with the wrong number of rows.

**Working state inside the solver** (all plain arrays and lists; space is O(n) on top of the matrix):

| Variable | Type | Meaning |
|---|---|---|
| `visited[v]` | `boolean[n]` | Customer `v` already delivered |
| `alive[v]` | `boolean[n]` | `v` is unvisited **and** reachable on time if the rider drives straight there now |
| `currentNode`, `t` | `int`, `double` | Where the rider is and the current time |
| `route`, `arrivals` | `List` | The delivered stops so far, in order |

**Why no priority queue?** Every candidate's score (lost count, travel time, slack) depends on
where the rider is now, so every key changes after every move. A heap would need an
O(n log n) rebuild at each step. That is more work than the O(n) linear scan.

---

## 3. The selection rules

At each step, every alive candidate `u` is scored. The rules below are compared **in strict
order, with no weights**. A later rule only matters if all earlier rules tie.

| # | Rule | Computed as | Serves |
|---|---|---|---|
| ① | **Feasibility filter** | skip `u` unless `t + travel(cur, u) ≤ deadline(u)` | can't deliver late food |
| ② | **Fewest lost** | count alive `v ≠ u` with `arrival(u) + travel(u, v) > deadline(v)` | objective 1: on-time count |
| ③ | **Shortest travel** | `travel(cur, u)` | objective 2: mean wait |
| ④ | **Least slack** | `deadline(u) − arrival(u)`: most urgent first | keeps tight orders safe |
| ⑤ | **Lowest id** | `u` | deterministic output |

The rider moves to the winning candidate. When no candidate passes rule ①, the loop stops and
every unvisited customer is reported as **dropped**.

Floating-point comparisons use a tolerance `EPS = 1e-9`, so arriving exactly on the deadline
counts as on time.

### Why rule ② makes sense

The check "go to `u`, then drive **straight** to `v`" is the fastest possible way to reach `v`
after `u`. So the check is **one-sided**:

- If it **fails**, `v` really is lost on every route that starts with `u`.
  This relies on the travel matrix obeying the triangle inequality, which real distances do.
- If it **passes**, `v` might still be lost later, because each `v` is checked on its own.
  This is the known limitation (see §6).

---

## 4. Pseudocode

```
SolverA(instance):
    visited ← all false
    cur ← RESTAURANT ; t ← 0
    route, arrivals ← []

    loop:
        alive ← { v unvisited : t + travel(cur, v) ≤ deadline(v) }       # O(n)
        best ← none
        for u in alive:                                                  # O(n) candidates
            arr   ← t + travel(cur, u)
            lost  ← |{ v in alive, v ≠ u : arr + travel(u, v) > deadline(v) }|   # O(n)
            slack ← deadline(u) − arr
            if (lost, travel, slack, id) of u < that of best (lexicographic):
                best ← u
        if best is none: break                                           # nobody reachable
        t ← t + travel(cur, best)
        visited[best] ← true ; append best to route, t to arrivals
        cur ← best

    dropped  ← all unvisited customers
    meanWait ← mean over route of (arrival − orderTime), or 0 if route is empty
    return (route, arrivals, dropped, meanWait)
```

---

## 5. Worked examples (these are the checks in `Demo.java`)

All three examples put customers on a line, with the restaurant at position 0.
Travel time = distance, all orders are placed at `t = 0`, and so `deadline = shelfLife`.

### Example 1: the lookahead beats nearest neighbour

| Customer | id | Position | Deadline |
|---|---|---|---|
| A (ice cream) | 0 | +15 | 30 |
| B (rice) | 1 | −5 | 90 |
| C (rice) | 2 | −10 | 90 |

**Step 1** (at the restaurant, t = 0). All three are alive.

| Candidate | Arrives | Lost if chosen | Travel |
|---|---|---|---|
| A | 15 | 0 (B by 35, C by 40) | 15 |
| **B** | 5 | **0** (A by 25 ≤ 30, C by 10) | **5** |
| C | 10 | 1 (A by 35 > 30) | 10 |

A and B tie on rule ② (0 lost). B wins on rule ③ (shorter travel). **Go to B, t = 5.**

**Step 2** (at B, t = 5). A and C are alive.

| Candidate | Arrives | Lost if chosen |
|---|---|---|
| **A** | 25 | **0** (C by 50) |
| C | 10 | 1 (A by 35 > 30) |

**Go to A, t = 25.**

**Step 3** (at A, t = 25). Only C is left, and it arrives at 50 ≤ 90. **Go to C.**

**Result:** B → A → C. All 3 on time, arrivals 5 / 25 / 50, mean wait **26.67**.
Nearest neighbour goes B → C → A and reaches A at 35. A's ice cream spoils, so only **2** are on time.

### Example 2: the known limitation

| Customer | id | Position | Deadline |
|---|---|---|---|
| P | 0 | +5 | 10 |
| Q | 1 | −5 | 16 |
| R (rice) | 2 | −1 | 90 |

The best route is **P → Q → R**: P at 5, Q at 15 ≤ 16, R at 19. All 3 are on time.

**Step 1** (at the restaurant, t = 0).

| Candidate | Arrives | Lost if chosen | Travel |
|---|---|---|---|
| P | 5 | 0 (Q by 15, R by 11) | 5 |
| Q | 5 | 1 (P by 15 > 10) | 5 |
| **R** | 1 | **0** (P by 7, Q by 5) | **1** |

R reports "0 lost" because P and Q each look reachable **separately**. R wins on travel.
**Go to R, t = 1.**

**Step 2** (at R, t = 1). P and Q are alive, but they cannot **both** be reached in time.
Going to P loses Q, and going to Q loses P. They tie on rule ②, and Q is nearer, so the rider
**goes to Q, t = 5**.

**Step 3** (at Q, t = 5). P would arrive at 15 > 10, so nothing is alive. The loop stops.

**Result:** R → Q, with P **dropped**. 2 of 3 delivered, where 3 were possible.
This is exactly the case the one-step lookahead cannot see. It is the motivation for Approach B.

### Example 3: loose deadlines reduce to nearest neighbour

Positions +3, −2, +7, −6, +1, and every deadline is 999. No candidate ever loses anyone, so
rule ② always ties at 0 and rule ③ (nearest first) decides every step.
SolverA produces exactly the nearest-neighbour route: **4 → 0 → 2 → 1 → 3**.
This shows Approach A is a strict extension of the baseline. It only changes NN's choice when
a deadline is at risk.

---

## 6. Complexity, trade-offs and limitations

**Time: O(n³).** There are at most n steps. Each step scans O(n) candidates, and each
candidate's lost count scans O(n) other customers. Computing `alive` adds O(n) per step, which
doesn't change the bound.

**Space: O(n)** on top of the O(n²) travel matrix that is given as input.

| Strength | Limitation |
|---|---|
| Never makes a move that a direct check shows will doom someone, if a safer move exists | Checks each remaining order **on its own**, so it can't see that two orders are each reachable but not **both** (Example 2) |
| Same as nearest neighbour when deadlines are loose (Example 3) | Greedy: never backtracks or revises an earlier stop |
| Deterministic: the same input always gives the same route | Ignores `orderTime` when choosing; it only uses it to measure wait |
| Simple arrays, no tuning weights | Drops undeliverable orders instead of delivering them late |
| | Static: no orders can arrive mid-route |

---

## 7. How the code fits the group template

The final rules are in `docs/Deadline-Aware Food Delivery Problem Template.docx`.
The data rules are in `docs/data_decisions.md`.

| Template rule | Current code |
|---|---|
| V1 deadline = order time + shelf life | Done |
| Orders that can't be on time are cancelled | Done (`dropped`) |
| Only valid routes (late = 0) | Done: a late order is never visited |
| Rider leaves at t = 0; order times can be negative | Done |
| V2 (60-minute wait cap) | Not used: the group dropped V2 and uses V1 only |
| Reads the shared instance files | Done by the shared tools: `Instance.toSolverInstance()` in `src/com/delivery/tools/` |
| Reports delivered, cancelled, average wait, longest wait, run time | Done by `RunExperiments` (every route re-scored by `Evaluate`) |
| Customer ids 1…n, matrix index k = customer k | Solver ids stay 0…n−1; the shared tools convert (customer k = solver id k − 1) |
