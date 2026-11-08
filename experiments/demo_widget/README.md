# Demo widget

An interactive search tree showing how Approach B (greedy with one-step lookahead) chooses each
stop, and which orders die.

## Run it

Double-click `demo.html`. It's one standalone file, so it needs no server, install or internet.
On a projector, press F11 for full screen.

## Controls

| Control | What it does |
|---|---|
| Next, Back, Play (or the arrow keys and Space) | Move through the route one stop at a time |
| Scenario | Load one of three prepared sets of orders |
| Orders, Order set, Tightness | Load any project input: `data/processed/n{orders}_s{set}_t{tightness}.json` |
| Click a node | Change that customer's food and solve every route again |
| Click a results row | Follow that solver's path through the tree |
| Reset orders | Undo food changes |

The page uses the deadline rule `order time + shelf life`, as the rest of the project does.

## Reading the tree

- Each customer keeps one column, with their name and deadline at the top.
- The number in a circle is the customer's id, and the bold number is the arrival time.
- The thick line is the route taken. Dashed lines are options not taken.
- A **solid black node is dead**: the rider can't reach that order by its deadline from there.
- The dots under a node are its children: ● dies if the rider goes there next, ○ survives.
  The number of ● is Approach B's "orders lost" score.

## Scenarios

| Scenario | Orders, set, tightness | NN delivers | B delivers | Best possible | What to show |
|---|---|---|---|---|---|
| Where B helps most | 8, 9, 1.5 | 6 | 8 | 8 | B skips the nearest stop twice to save an order. Click NN to see the orders it loses |
| B's blind spot | 8, 5, 1.0 | 4 | 6 | 7 | Stop 1 has all ○, yet every next move has a ●. A one-step lookahead can't see this |
| NN beats B | 8, 9, 0.5 | 5 | 4 | 5 | An honest counter-example for Q&A |

Across all 180 inputs the page can show (8 and 10 orders), B delivers more than NN on 75, and
fewer on 3.

## Where the numbers come from

The page embeds the project's own inputs for 8 and 10 orders, so with unedited food every row
in the results table equals the matching row of `results/raw_results.csv` (methods `B_greedy`,
`NN` and `BRUTE`). There are no 9-order inputs in the experiment grid, so 9 isn't offered.

Approach B runs here as a JavaScript copy of `SolverB.java`, so the page can stay a single local
file. `verify.py` proves the copy is faithful across all 180 inputs (8 and 10 orders, tightness
0.5, 1.0 and 1.5, sets 1 to 30) plus the template's worked example:

- Approach B must match `SolverB.java`, route for route.
- NN must match `NearestNeighbor.java`, route for route.
- The best route must match `BruteForce.java` on orders delivered and total wait.
- Delivered, average wait and longest wait must equal `results/raw_results.csv`.
- The black dots must equal B's "orders lost" score at every stop.
- The nodes that die must be exactly the orders each solver cancels.

## Files

| Path | What it is |
|---|---|
| `demo.html` | The finished page, built from `src/` |
| `src/solvers.js` | Approach B, NN, the best route, the scorer and the tree rows |
| `src/app.js`, `src/page.html` | Drawing, controls and styling |
| `src/scenarios.json` | The three prepared scenarios |
| `build.py` | Joins `src/` and the input files into `demo.html` |
| `verify/` | Checks the JavaScript against the Java and the results CSV |

After changing anything in `src/`, or regenerating the inputs or results, rebuild and check
from this folder:

    python build.py
    python verify/verify.py

`build.py` needs Python 3.10+. `verify.py` also needs `javac`, `java` and `node`.

If you change `SolverB.java`, make the same change in `src/solvers.js`. `verify.py` will fail until
the two match.
