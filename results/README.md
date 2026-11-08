# Results

Produced by `experiments/comparison/run_all.bat` (or `.sh`) from the 540 instance files in
`data/processed/`: n = 5, 8, 10, 20, 50, 100 × tightness 0.5, 1.0, 1.5 × seeds 1–30.
Deadline = order time + shelf life. Every route was re-scored independently and has late = 0.

## Files

| File | Contents |
|---|---|
| `raw_results.csv` | One row per instance × method (1,890 rows) |
| `summary.csv` | Mean and spread per n × tightness × method (30 runs each) |
| `fig_cancelled.png` | Orders cancelled, % of n, by tightness |
| `fig_gap_to_best.png` | Extra orders cancelled compared with brute force, n ≤ 10 |
| `fig_runtime.png` | Run time per instance, log scale |

## Columns

| Column | Meaning |
|---|---|
| `method` | `NN` nearest neighbour · `A_plan_fix` Approach A (code `SolverA`) · `B_greedy` Approach B (code `SolverB`) · `BRUTE` exact best |
| `delivered`, `cancelled`, `late` | Counts; `late` is always 0 |
| `avg_wait`, `longest_wait` | Minutes, over delivered customers (wait = arrival − order time) |
| `runtime_ms` | Median wall time of repeated runs (1–20 depending on n) |
| `best_delivered` | Brute-force delivered count (n ≤ 10 only) |
| `extra_cancelled_vs_best` | How many more orders the method cancels than the exact best |
| `avg_wait_gap_vs_best` | Average-wait difference, only when the method delivers as many as the best |
| `pct_runs_as_many_delivered_as_best` | Share of the 30 instances where the method matches the best on goal 1 |
| `pct_runs_exactly_best` | Share where it matches on both goals |

## Headline (tightness 1.0, mean orders cancelled over 30 instances)

| n | Nearest neighbour | Approach B | Approach A | Exact best |
|---|---|---|---|---|
| 5 | 0.97 | 0.77 | 0.60 | 0.60 |
| 8 | 2.00 | 1.53 | 1.17 | 1.07 |
| 10 | 2.80 | 2.20 | 1.73 | 1.57 |
| 20 | 8.27 | 8.17 | 7.73 | – |
| 50 | 26.00 | 25.77 | 27.23 | – |
| 100 | 53.87 | 54.80 | 63.20 | – |

Run time at n = 100: Approach A about 640 ms, Approach B about 0.5 ms, nearest neighbour
about 0.03 ms (one laptop-class core; times differ by machine).

Notes for reading the numbers:
- Above n = 33 locations repeat (customers 0 min apart) and one rider must drop most orders, so
  n = 50 and 100 mostly test speed and how each method behaves when most orders are lost.
- Run times below about 0.01 ms are at the timer's resolution; compare them only roughly.

## Benchmark provenance

The checked-in timing values are the original benchmark capture used by `slides.pptx`. They are hardware-dependent. The final audit changed cancellation-list construction in `SolverA` from sorting to a linear scan, preserving its outputs and the documented O(n³) per-pass bound. Re-running the scripts measures the current revision and replaces CSVs/charts; update the slide timing claims if you use that new capture.
