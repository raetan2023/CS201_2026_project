# Comparison: how to run the experiments

We use one version of the problem (the template's V1): deadline = order time + shelf life.
The template's V2 (a 60-minute wait cap) was dropped by the group.

Everything here is Java 17+, no extra libraries. The charts need Python 3 with matplotlib
(optional).

## Names

| In the presentation | Code | Folder |
|---|---|---|
| Approach A: deadline order, then improve by moving stops | `SolverB.java` | `experiments/approach_B/` |
| Approach B: greedy with one-step lookahead | `SolverA.java` | `experiments/approach_A/` |
| Nearest neighbour baseline | `NearestNeighbor.java` | `src/com/delivery/tools/` |
| Exact best (n ≤ 10) | `BruteForce.java` | `src/com/delivery/tools/` |

In `results/*.csv` the methods are `A_plan_fix`, `B_greedy`, `NN` and `BRUTE`.

## Run everything (one command)

From the project folder:

    experiments\comparison\run_all.bat          (Windows Command Prompt)
    bash experiments/comparison/run_all.sh      (macOS / Linux / Git Bash)

It does the five steps below and takes 2 to 5 minutes. Instance generation is deterministic,
so you get exactly the same 540 files and the same cancelled counts and waits as the committed
results (run times will differ by machine).

## Step by step (what the script does)

**1. Compile** the two solvers and the shared tools into `out/` (ignored by git):

    javac -encoding UTF-8 -d out experiments/approach_A/src/com/delivery/routing/Customer.java experiments/approach_A/src/com/delivery/routing/DeliveryInstance.java experiments/approach_A/src/com/delivery/routing/RouteResult.java experiments/approach_A/src/com/delivery/routing/SolverA.java experiments/approach_B/src/com/delivery/routing/SolverB.java src/com/delivery/tools/*.java

Only one copy of `Customer`, `DeliveryInstance` and `RouteResult` is compiled (the two
approach folders have identical copies), and the two `Demo.java` files are left out because
they share a class name.

**2. Check the shared tools** against the template's worked example (n = 6, seed 10):

    java -cp out com.delivery.tools.CheckWorkedExample data/raw/MY.txt

All checks should pass: best route 1→2→5→4→6→3 (average wait 29.0), nearest neighbour
cancels Kuala Lumpur Village (21.9). It also saves that instance as
`data/processed/example_n6_s10.json`.

**3. Generate instances** from `MY.txt`:

    java -cp out com.delivery.tools.GenerateInstance data/raw/MY.txt --all                    (540 files)
    java -cp out com.delivery.tools.GenerateInstance data/raw/MY.txt --n 8 --seed 1 --tight 1.0  (one file)

Files go to `data/processed/`, named like `n8_s1_t1.0.json` (8 orders, seed 1, tightness 1.0).

**4. Run the experiments:**

    java -cp out com.delivery.tools.RunExperiments

For every instance it runs NN, Approach A, Approach B and (n ≤ 10) brute
force; re-scores every route with `Evaluate.java`; stops with an error if any route has a late
order; and writes `results/raw_results.csv` and `results/summary.csv`.

**5. Draw the charts:**

    python experiments/comparison/plot_results.py

## Try one instance by hand

    java -cp out com.delivery.tools.BruteForce data/processed/n8_s1_t1.0.json
    java -cp out com.delivery.tools.NearestNeighbor data/processed/n8_s1_t1.0.json
    java -cp out com.delivery.tools.Evaluate data/processed/n8_s1_t1.0.json 3 1 4 2

`Evaluate` takes customer ids in delivery order (ids left out are cancelled) and
prints delivered, cancelled, late, average and longest wait, plus each stop's arrival and deadline.

## Plug in another method

Write a method that takes an `Instance` and returns customer ids (1..n) in
delivery order, then add one line to `METHODS` in `RunExperiments.java`. For a solver that uses
the `com.delivery.routing` model, `Instance.toSolverInstance()` builds its input and `RunExperiments.fromSolver` converts its ids back.
