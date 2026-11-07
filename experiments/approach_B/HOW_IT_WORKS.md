# How Approach B works

Approach B is **"sort, then fix"**. It never builds the route one stop at a time. Instead it
always holds a **complete plan**: a list of *all* customers in the order the rider will try
them. It works in two steps:

1. **Start plan:** sort the orders by deadline, most urgent first (Earliest Deadline First).
2. **Improve:** try taking one customer out of the plan and putting it back somewhere else.
   Keep the best such move if it makes the plan better, and repeat until no move helps.

The difference from Approach A in one sentence: **A decides each stop once and never looks
back; B can keep changing any stop until the plan stops improving.**

All code lives in `src/com/delivery/routing/`. See `README.md` for compile/run commands.

---

## 1. The problem the code solves

Same variant as Approach A (see `../approach_A/HOW_IT_WORKS.md` §1):

| Classic TSP | Our variant (as coded) |
|---|---|
| Every city is equally important | Every customer has a **deadline** (food spoils) |
| Only the visit order matters | The **arrival time** at each stop matters |
| Single goal: shortest tour | 1st goal: **most on-time deliveries**; 2nd goal: **low mean wait** |
| Tour returns to the start | **Open path**: the route ends at the last customer |
| Every city must be visited | Orders that can no longer arrive on time are **dropped** |

Same assumptions: the rider leaves at `t = 0` with every order, `deadline = orderTime + shelfLife`,
`wait = arrival − orderTime` for delivered customers only, travel times come from a matrix.

---

## 2. Data structures

| File | What it holds |
|---|---|
| `Customer.java` | Same as A: `(id, orderTime, shelfLife, foodType)` with `deadline()` |
| `DeliveryInstance.java` | Same as A: the travel matrix plus the customer list |
| `RouteResult.java` | Same as A: `route`, `arrivals`, `dropped`, `meanWait` |
| `SolverB.java` | The algorithm |
| `Demo.java` | Hand-worked sanity checks and the nearest-neighbour baseline |

The three model files are copies of Approach A's, so both approaches read the same input and
produce the same output type. **Indexing** is the same as A: matrix index 0 is the restaurant,
index `i + 1` is customer `i`.

**The main data structure is the plan**, a `List<Integer>` holding every customer id exactly
once (a permutation).

| Variable | Type | Meaning |
|---|---|---|
| `plan` | `List<Integer>` (size n) | Order in which the rider will *try* the customers. Includes the ones that end up cancelled |
| `best` | `RouteResult` | The result of driving the current plan |

**Why keep cancelled customers in the plan?** If an order is cancelled now, a later move may
put it earlier in the plan, where it *can* be delivered. Approach A removes an order for good
once it's out of reach. B can bring it back.

**Alternatives we considered**

| Option | Why not |
|---|---|
| Pure Earliest Deadline First with a priority queue (pop most urgent, deliver or cancel) | Simplest of all, but it ignores the map. In our trial runs it cancelled *more* orders than the nearest-neighbour baseline. We keep it only as Step 1 (a single sort is enough, so no heap is needed) |
| Greedy "least slack next" (always go to the most urgent reachable order) | Also simple, but also worse than the baseline in trial runs |
| Swap two stops instead of moving one | Works, but in trial runs it was slightly worse than "move", and "take one out, put it back" is easier to explain |

---

## 3. The rules

### Driving a plan ("walk")

```
go through the plan in order:
    if the rider can reach the next customer by its deadline → drive there, deliver
    otherwise                                                → cancel it, rider stays put
```

This gives the route, arrival times, cancelled orders and mean wait. Because a customer is
only ever delivered on time, **every result is a valid route (late = 0)**.

### Step 1: start plan

Sort all customers by deadline, smallest first. Equal deadlines go in id order, so the output
is deterministic.

### Step 2: improve

A **move** takes the customer at position `from` out of the plan and puts it back at
position `to`. Each pass tries all `n × (n − 1)` moves, drives each new plan, and remembers the
best one. Plans are compared **in goal order, with no weights**:

| # | Compare | Serves |
|---|---|---|
| ① | More orders delivered wins | Goal 1: fewest cancelled |
| ② | If tied: lower mean wait wins | Goal 2: lowest average wait |

If the best move beats the current plan, B takes it and starts another pass. If no move
beats it, B stops.

**Why it always stops:** every accepted move makes the plan strictly better, and there are only
finitely many plans, so B can never loop.

**Why it never does worse than Step 1:** a move is only accepted if it is better, so the final
plan is at least as good as the deadline order.

---

## 4. Pseudocode

```
SolverB(instance):
    plan ← all customer ids sorted by (deadline, id)            # Step 1, O(n log n)
    best ← walk(plan)

    loop:                                                         # Step 2
        bestPlan ← none
        for from in 0 .. n−1:
            for to in 0 .. n−1, to ≠ from:
                candidate ← plan with plan[from] moved to position to   # O(n)
                result    ← walk(candidate)                             # O(n)
                if result is better than best:        # more delivered, then lower mean wait
                    best ← result ; bestPlan ← candidate
        if bestPlan is none: break                                # no move helps
        plan ← bestPlan

    return best

walk(plan):
    t ← 0 ; cur ← RESTAURANT
    for id in plan:
        arr ← t + travel(cur, id)
        if arr ≤ deadline(id): deliver id at arr ; t ← arr ; cur ← id
        else:                  cancel id
    return (route, arrivals, cancelled, mean wait)
```

---

## 5. Worked examples (these are the checks in `Demo.java`)

All examples put customers on a line, with the restaurant at position 0.
Travel time = distance, all orders are placed at `t = 0`, so `deadline = shelfLife`.
**Examples 1–3 are the same instances as Approach A's**, so you can compare directly.

### Example 1: same answer as A

| Customer | id | Position | Deadline |
|---|---|---|---|
| A (ice cream) | 0 | +15 | 30 |
| B (rice) | 1 | −5 | 90 |
| C (rice) | 2 | −10 | 90 |

**Step 1.** Deadline order: A, B, C (B and C tie at 90, so id order).
Drive it: A at 15, B at 35, C at 40. All 3 on time, mean wait **30.0**.

**Pass 1.** The best move puts A after B, giving **B, A, C**: B at 5, A at 25, C at 50.
All 3 on time, mean wait **26.67**. Better, so B takes it.

**Pass 2.** No move improves on 26.67. Stop.

**Result:** B → A → C, the same route as SolverA. Nearest neighbour goes B → C → A, and A's ice cream
spoils (only 2 on time).

### Example 2: A's trap does not affect B

| Customer | id | Position | Deadline |
|---|---|---|---|
| P | 0 | +5 | 10 |
| Q | 1 | −5 | 16 |
| R (rice) | 2 | −1 | 90 |

SolverA goes to R first (it's nearest and looks safe) and then loses P: **2 delivered**.

**Step 1.** Deadline order: P, Q, R. P at 5, Q at 15 ≤ 16, R at 19. **All 3 on time**, mean
wait **13.0**. The urgent orders are first, so the trap never happens.

**Pass 1.** The best move puts Q at the end: **P, R, Q**. P at 5, R at 11 (picked up on the way
back), Q at 15 ≤ 16. Mean wait **10.33**.

**Pass 2.** Nothing better. Stop.

**Result:** P → R → Q, all 3 delivered. This is also better than the P → Q → R route given as
"best" in Approach A's notes, which has a mean wait of 13.0.

### Example 3: loose deadlines

Positions +3, −2, +7, −6, +1 (ids 0–4), every deadline 999.

**Step 1.** All deadlines tie, so the plan is id order 0, 1, 2, 3, 4, which zig-zags across the
restaurant: arrivals 3, 8, 17, 30, 37, mean wait **19.0**.

**Pass 1.** Move id 4 (at +1) to the front: arrivals 1, 3, 8, 17, 30, mean **11.8**.
**Pass 2.** Move id 1 (at −2) after id 2 (at +7): plan 4, 0, 2, 1, 3, arrivals 1, 3, 7, 16, 20,
mean **9.4**.
**Pass 3.** Nothing better. Stop.

**Result:** 4 → 0 → 2 → 1 → 3, the same as nearest neighbour (and SolverA). When deadlines don't
matter, Step 2 turns into a pure "shorten the waits" search.

### Example 4: why Step 2 is needed (the "on the way" case)

| Customer | id | Position | Deadline |
|---|---|---|---|
| X | 0 | +10 | 10 |
| Y | 1 | +5 | 11 |

**Step 1.** Deadline order: X, Y. X at 10, then back to Y at 15 > 11, so **Y is cancelled**.
Sorting by deadline alone ignores the map: Y sits right on the way to X.

**Pass 1.** Moving X after Y gives Y at 5, X at 10: **both on time**. B takes it.

**Result:** Y → X, 2 of 2 delivered. Pure Earliest Deadline First would stop at 1.

### Example 5: the known limitation

| Customer | id | Position | Deadline |
|---|---|---|---|
| S | 0 | −5 | 16 |
| T | 1 | +5 | 31 |
| U | 2 | −12 | 24 |
| V | 3 | +4 | 20 |

The best route is **V → T → S → U**: east first (V at 4, T at 5), then west (S at 15, U at 22).
**All 4 are on time**, and SolverA finds this route.

**Step 1.** Deadline order: S, V, U, T. S at 5, V at 14, U at 30 > 24 (**cancelled**), T at 15.
3 delivered, mean wait **11.33**.

**Pass 1.** To reach V, T, S, U, *both* V and T must move ahead of S. Each single move on its
own still cancels someone:

| Move tried | Plan | Delivered | Mean wait |
|---|---|---|---|
| V to the front | V, S, U, T | 3 (T late at 37) | 12.33 |
| T to the front | T, S, V, U | 3 (V late at 24) | 14.00 |
| … all 12 moves | | at most 3 | none below 11.33 |

No move is strictly better, so B stops after the first pass with **3 delivered** where 4 were
possible. This is a **local optimum**: B only looks one move away from the current plan. It
mirrors A's Example 2. Each approach handles the other's trap.

---

## 6. Complexity, trade-offs and limitations

**Step 1: O(n log n)** for one sort.

**One pass of Step 2: O(n³).** There are `n(n − 1)` moves, and each one costs O(n) to copy the
plan and O(n) to drive it.

**Number of passes:** this depends on the instance and has no small fixed bound. Each pass must
strictly improve the plan, so it always ends. In practice it grows with n and with looser
deadlines (more room to reorder), so measure it in the experiments. Total time is
**O(passes × n³)**.

**Space: O(n)**: the current plan plus one candidate and the best candidate, on top of the
O(n²) travel matrix.

### A vs B at a glance

| | Approach A | Approach B |
|---|---|---|
| Idea | Build the route one stop at a time | Start from a full plan, then keep improving it |
| Main data structure | `visited` / `alive` arrays, linear scan | The plan: a list of all n ids (a permutation) |
| Rules to explain | 5 tie-break rules plus a lookahead count | 1 sort, 1 kind of move, 1 comparison |
| Can it undo an early choice? | No | Yes: any stop can be moved in a later pass |
| Time | O(n³), fixed | O(n³) per pass × number of passes (slower for large n) |
| Typical trap | Each remaining order checked on its own (Example 2) | Fix needs two moves at once (Example 5) |

| Strength | Limitation |
|---|---|
| Easy to explain: "sort by urgency, then keep fixing the order" | Local search: stops at the first plan no single move can improve (Example 5) |
| Can bring a cancelled order back later | Slower than A for large n, because every candidate plan is driven from the start |
| Never worse than plain deadline order; equal to nearest neighbour when deadlines are loose (Example 3) | Run time is hard to predict in advance (number of passes varies) |
| Deterministic, no tuning weights | Ignores `orderTime` except through the deadline and the wait score |
| Uses the same scoring rule as the evaluator, so "better" means exactly the project's goals | Drops undeliverable orders instead of delivering them late; static, no new orders mid-route |

**Possible extensions (not implemented, to keep B simple):** also try swaps or moves of two
stops (fixes Example 5, costs more time); restart from a few different start plans; stop after a
fixed number of passes to cap run time.

---

## 7. How the code fits the group template

The final rules are in `docs/Deadline-Aware Food Delivery Problem Template.docx`.
The data rules are in `docs/data_decisions.md`. B matches A's current status:

| Template rule | Current code |
|---|---|
| V1 deadline = order time + shelf life | Done |
| Orders that can't be on time are cancelled | Done (`dropped`) |
| Only valid routes (late = 0) | Done: `walk` only delivers on time |
| Rider leaves at t = 0; order times can be negative | Done |
| Goals compared in order (cancelled first, wait breaks ties) | Done: built into `isBetter` |
| V2 deadline = order time + min(shelf life, 60) | **Not yet**: V1 only. Only `Customer.deadline()` needs changing |
| Reads the shared instance files | **Not yet**: only the hand-built examples in `Demo.java` |
| Reports delivered, cancelled, average wait, longest wait, run time | **Partly**: no longest wait or run time |
| Customer ids 1…n, matrix index k = customer k | **Not yet**: ids are 0…n−1, customer i is at index i + 1 |
