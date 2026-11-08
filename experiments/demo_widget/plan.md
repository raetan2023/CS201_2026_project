# Demo widget plan

**Status: approved and built.** This records what was agreed. For how to run the widget, see
[README.md](README.md).

## What it is

`demo.html` is a single local page for the presentation. Its main visual is a TSP-style search
tree: the restaurant at the top, and each row showing the customers the rider could go to next.
As the rider moves, the tree grows downward, and any order that can no longer arrive on time turns
solid black. A presenter can change an order and watch every route get solved again.

The tree suits this project because it shows Approach B's logic directly: B's "orders lost" score
is the number of children that would die under a candidate.

Two lines in the brief shaped it:

- **"No fake screenshots, logs, test results, or demo evidence."** Every number on screen is
  computed live, by code that's checked against the real Java.
- **"Implementing a fancy UI does not qualify as a X-factor."** So it stays plain, and exists only
  to make the algorithm easier to explain.

## Decisions

| # | Question | Decision |
|---|---|---|
| 1 | What does "dies" mean? | A node is dead when the rider can't reach that order by its deadline from where they are. It shows once in black, then leaves the tree |
| 2 | Whole tree, or one path? | One path, showing every option at each step. The full tree has thousands of nodes |
| 3 | Local server, or local file? | **Local file.** Approach B runs as a JavaScript copy of `SolverB.java`, and `verify.py` proves the two match |
| 4 | Add a callback to the solver? | Approved, then not needed. With a local file, the page never runs the Java, so `SolverB.java` is unchanged |
| 5 | Fix the NN baseline? | Yes. The page now follows main's `NearestNeighbor.java`, which skips late orders instead of driving to them |
| 6 | Keep the scenario where NN beats B? | Yes |
| 7 | Sync with main's restructure (2026-11-08)? | Yes. The greedy solver the page shows is now **Approach B** (`SolverB.java`), the deadline rule is V1 only, and the page uses main's input files instead of its own generator |

## Data

The page embeds the project's inputs for 8 and 10 orders: `data/processed/n{8,10}_s{1..30}_t*.json`,
made by `GenerateInstance.java`. "Order set k" is seed k. Only the tightness 1.0 file of each set is
stored, and `build.py` checks that the 0.5 and 1.5 files differ from it in shelf life only.

Because the inputs and the scorer match the experiments, every row of the page's results table
equals the matching row of `results/raw_results.csv` until someone changes a food, and `verify.py`
checks that. The experiment grid has no 9-order inputs, so the page offers 8 and 10 only.

## Left out on purpose

- **The full tree:** it's unreadable on a projector.
- **Expanding any node by hand:** the page follows solver paths only.
- **Other sizes:** 5 orders has less to show, and from 20 the tree gets too wide and brute force
  gets slow.
- **Real roads, map tiles and online hosting.**
- **Approach A (plan-and-fix):** the tree explains B's lookahead, which A doesn't use. Adding A as a
  results row takes a port of `SolverA.java` in `src/solvers.js`, one more row, and one more check
  in `verify.py` against `SolverA.java` and the `A_plan_fix` rows of the results CSV.
