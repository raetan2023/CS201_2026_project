"""Charts from results/summary.csv (needs Python 3 + matplotlib).

    python experiments/comparison/plot_results.py

Writes results/fig_cancelled.png, fig_gap_to_best.png, fig_runtime.png.
"""
import csv
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "results")
rows = list(csv.DictReader(open(os.path.join(RES, "summary.csv"))))
NS = [5, 8, 10, 20, 50, 100]
TIGHTS = ["0.5", "1.0", "1.5"]
STYLE = {"NN": ("Nearest neighbour", "#9ca3af", "o"), "B_greedy": ("Approach B (greedy)", "#3b82f6", "s"),
         "A_plan_fix": ("Approach A (plan + fix)", "#8b5cf6", "D"), "BRUTE": ("Exact best (n ≤ 10)", "#eab308", "*")}


def get(t, m, col):
    out = []
    for n in NS:
        r = [x for x in rows if x["tight"] == t and x["method"] == m and int(x["n"]) == n]
        out.append(float(r[0][col]) if r and r[0][col] != "" else None)
    return out


def pct_cancelled(t, m):
    return [None if c is None else 100 * c / n for c, n in zip(get(t, m, "cancelled_mean"), NS)]


if True:
    fig, axes = plt.subplots(1, 3, figsize=(15, 4.6), sharey=True)
    for ax, t in zip(axes, TIGHTS):
        for m, (lbl, col, mk) in STYLE.items():
            ys = pct_cancelled(t, m)
            xs = [i for i, y in enumerate(ys) if y is not None]
            ax.plot(xs, [ys[i] for i in xs], marker=mk, color=col, label=lbl, lw=2, ms=7 if mk != "*" else 12)
        ax.set_xticks(range(len(NS)), [str(n) for n in NS])
        ax.set_title(f"Shelf-life tightness × {t}")
        ax.set_xlabel("orders n")
        ax.grid(alpha=.3)
    axes[0].set_ylabel("orders cancelled (% of n, mean of 30)")
    axes[0].legend(frameon=False)
    fig.suptitle("Orders cancelled (lower is better)")
    fig.tight_layout()
    fig.savefig(os.path.join(RES, "fig_cancelled.png"), dpi=150)
    plt.close(fig)

fig, ax = plt.subplots(figsize=(7.5, 4.4))
if True:
    labels, w = [], 0.25
    for j, m in enumerate(["NN", "B_greedy", "A_plan_fix"]):
        lbl, col, _ = STYLE[m]
        vals, xs = [], []
        for i, t in enumerate(TIGHTS):
            for k, n in enumerate([5, 8, 10]):
                r = [x for x in rows if x["tight"] == t and x["method"] == m and int(x["n"]) == n][0]
                vals.append(float(r["extra_cancelled_vs_best_mean"]))
                xs.append(i * 3.6 + k + (j - 1) * w)
        ax.bar(xs, vals, width=w, color=col, label=lbl)
    ax.set_xticks([i * 3.6 + k for i in range(3) for k in range(3)],
                  [f"n={n}\n×{t}" for t in TIGHTS for n in (5, 8, 10)])
    ax.set_title("Extra orders cancelled vs the exact best (n ≤ 10)")
    ax.grid(axis="y", alpha=.3)
ax.set_ylabel("mean extra cancelled per instance")
ax.legend(frameon=False)
fig.tight_layout()
fig.savefig(os.path.join(RES, "fig_gap_to_best.png"), dpi=150)
plt.close(fig)

fig, ax = plt.subplots(figsize=(7.5, 4.6))
for m in ["NN", "B_greedy", "A_plan_fix"]:
    lbl, col, mk = STYLE[m]
    ys = get("1.0", m, "runtime_ms_mean")
    ax.plot(range(len(NS)), ys, marker=mk, color=col, label=lbl, lw=2)
ax.set_yscale("log")
ax.set_xticks(range(len(NS)), [str(n) for n in NS])
ax.set_xlabel("orders n")
ax.set_ylabel("run time per instance (ms, log scale)")
ax.set_title("Run time (tightness × 1.0)")
ax.grid(alpha=.3, which="both")
ax.legend(frameon=False)
fig.tight_layout()
fig.savefig(os.path.join(RES, "fig_runtime.png"), dpi=150)
plt.close(fig)
print("wrote charts to", RES)
