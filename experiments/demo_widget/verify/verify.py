"""Checks the demo's JavaScript against the project's Java and its results.

  Approach B   must match SolverB.java, route for route
  NN           must match NearestNeighbor.java, route for route
  Best route   must match BruteForce.java on orders delivered and total wait
  Results      delivered, average wait and longest wait must equal results/raw_results.csv
               (methods B_greedy, NN and BRUTE)
  Tree         black dots must equal B's lost count, and the nodes that die must be
               exactly the orders each solver cancels
  Worked example (data/processed/example_n6_s10.json) must give the template's figures

Covers 8 and 10 orders, tightness 0.5, 1.0 and 1.5, sets (seeds) 1 to 30: every input the
page can show. Exits with an error if anything differs.

Run from anywhere: python verify.py    (needs javac, java and node)
"""
import csv
import json
import subprocess
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
DEMO = HERE.parent
ROOT = DEMO.parent.parent
sys.path.insert(0, str(DEMO))
from build import SEEDS, SIZES, TIGHTS, instance_path, load_data, read  # noqa: E402

JAVA_SOURCES = [
    *(ROOT / "src" / "com" / "delivery" / "routing").glob("*.java"),
    *(ROOT / "src" / "com" / "delivery" / "tools").glob("*.java"),
    # the tools reference both solvers, so both must compile
    ROOT / "experiments" / "approach_A" / "src" / "com" / "delivery" / "routing" / "SolverA.java",
    ROOT / "experiments" / "approach_B" / "src" / "com" / "delivery" / "routing" / "SolverB.java",
    HERE / "ExportRoutes.java",
]
RESULTS_CSV = ROOT / "results" / "raw_results.csv"
CSV_METHOD = {"b": "B_greedy", "nn": "NN", "best": "BRUTE"}
EXAMPLE = ROOT / "data" / "processed" / "example_n6_s10.json"
EXAMPLE_EXPECTED = {  # route, average wait, longest wait, as printed in the group template
    "best": ([1, 2, 5, 4, 6, 3], 29.0, 62.7),
    "nn": ([1, 2, 4, 6, 3], 21.9, 49.8),
}
SCENARIOS = json.loads((DEMO / "src" / "scenarios.json").read_text(encoding="utf-8"))
EPS = 1e-9


def key_of(n, seed, tight):
    return f"n{n}_s{seed}_t{tight:.1f}"


def total_wait(inst, route):
    T = inst["travel_min"]
    ot = {c["id"]: c["order_time"] for c in inst["customers"]}
    t, cur, total = 0.0, 0, 0.0
    for k in route:
        t += T[cur][k]
        total += t - ot[k]
        cur = k
    return total


def java_routes(paths, tmp):
    listing = tmp / "inputs.txt"
    listing.write_text("\n".join(str(p) for p in paths), encoding="utf-8")
    classes = tmp / "classes"
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *map(str, JAVA_SOURCES)],
                   check=True)
    out = subprocess.run(["java", "-cp", str(classes), "com.delivery.widget.ExportRoutes",
                          str(listing)], check=True, capture_output=True, text=True).stdout
    parse = lambda s: [int(x) for x in s.split(",") if x]
    return [tuple(parse(part) for part in line.split("|")) for line in out.splitlines()]


def run_js(payload, tmp):
    src, dst = tmp / "input.json", tmp / "output.json"
    src.write_text(json.dumps(payload), encoding="utf-8")
    subprocess.run(["node", str(HERE / "run_js.js"), str(src), str(dst)], check=True)
    return json.loads(dst.read_text(encoding="utf-8"))


def csv_rows():
    rows = {}
    with open(RESULTS_CSV, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            rows[(key_of(int(r["n"]), int(r["seed"]), float(r["tight"])), r["method"])] = r
    return rows


def main():
    data = load_data()  # also checks the tightness files agree
    cases = [dict(key=key_of(n, s, t), n=n, seed=s, tight=t)
             for n in SIZES for t in TIGHTS for s in SEEDS]
    paths = [instance_path(c["n"], c["seed"], c["tight"]) for c in cases] + [EXAMPLE]
    fixed = [dict(key="worked_example", inst=read(EXAMPLE))]

    with tempfile.TemporaryDirectory() as t:
        tmp = Path(t)
        results = run_js(dict(data=data, cases=cases, fixed=fixed), tmp)
        java = java_routes(paths, tmp)

    by_key = {r["key"]: r for r in results}
    insts = {c["key"]: read(p) for c, p in zip(cases, paths)}
    insts["worked_example"] = fixed[0]["inst"]
    keys = [c["key"] for c in cases] + ["worked_example"]
    assert len(java) == len(keys)

    expected_csv = csv_rows()
    failures = []
    same = {"b": 0, "nn": 0, "best": 0, "csv": 0}
    for key, (java_b, java_nn, java_best) in zip(keys, java):
        r, inst = by_key[key], insts[key]
        if r["b"]["route"] == java_b:
            same["b"] += 1
        else:
            failures.append(f"{key}: Approach B: js {r['b']['route']} java {java_b}")
        if r["nn"]["route"] == java_nn:
            same["nn"] += 1
        else:
            failures.append(f"{key}: NN: js {r['nn']['route']} java {java_nn}")
        js_total = total_wait(inst, r["best"]["route"])
        java_total = total_wait(inst, java_best)
        if r["best"]["delivered"] == len(java_best) and abs(js_total - java_total) < 1e-6:
            same["best"] += 1
        else:
            failures.append(f"{key}: best: js {r['best']['delivered']} {js_total}, "
                            f"java {len(java_best)} {java_total}")

        for name in ("b", "nn", "best"):
            if r[name]["late"]:
                failures.append(f"{key}: {name} delivers late")
            failures += [f"{key}: {name}: {p}" for p in r["pathProblems"][name]]
        if r["dotMismatches"]:
            failures.append(f"{key}: {r['dotMismatches']} dot counts differ from lost counts")

        if key == "worked_example":
            continue
        ok = True
        for name, method in CSV_METHOD.items():
            row = expected_csv.get((key, method))
            got = r[name]
            if row is None:
                ok = False
                failures.append(f"{key}: no {method} row in {RESULTS_CSV.name}")
            elif (got["delivered"] != int(row["delivered"])
                  or abs(got["avg"] - float(row["avg_wait"])) > 1e-3
                  or abs(got["max"] - float(row["longest_wait"])) > 1e-3):
                ok = False
                failures.append(f"{key}: {method}: page {got['delivered']} {got['avg']:.4f} "
                                f"{got['max']:.4f}, csv {row['delivered']} {row['avg_wait']} "
                                f"{row['longest_wait']}")
        same["csv"] += ok

    example = by_key["worked_example"]
    for name, (route, avg, longest) in EXAMPLE_EXPECTED.items():
        got = example[name]
        if (got["route"], round(got["avg"], 1), round(got["max"], 1)) != (route, avg, longest):
            failures.append(f"worked example {name}: expected {route} {avg} {longest}, "
                            f"got {got['route']} {got['avg']} {got['max']}")

    for s in SCENARIOS:
        key = key_of(s["places"], s["seed"], s["tight"])
        r = by_key[key]
        got = [r["nn"]["delivered"], r["b"]["delivered"], r["best"]["delivered"]]
        if got != s["expect"]:
            failures.append(f"scenario {s['name']} ({key}): expected {s['expect']}, got {got}")

    total = len(keys)
    print(f"Approach B matches SolverB.java:          {same['b']}/{total}")
    print(f"NN matches NearestNeighbor.java:          {same['nn']}/{total}")
    print(f"Best route matches BruteForce.java:       {same['best']}/{total}")
    print(f"Table matches results/raw_results.csv:   {same['csv']}/{len(cases)}")
    print(f"({len(cases)} input files plus the worked example)")
    if failures:
        print(f"\n{len(failures)} FAILURE(S):")
        print("\n".join(failures[:30]))
        sys.exit(1)
    print("Tree dots, dead nodes, worked example and scenarios: all OK")
    print("ALL CHECKS PASSED")


if __name__ == "__main__":
    main()
