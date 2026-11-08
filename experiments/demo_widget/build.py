"""Builds demo.html: one standalone file you can double-click, with no server or internet.

It joins src/page.html, src/solvers.js, src/app.js and src/scenarios.json, and embeds the
project's own inputs for 8 and 10 orders (data/processed/n{8,10}_s{1..30}_t*.json).

Only the tightness 1.0 file of each set is stored. The page multiplies the base shelf life by
the tightness, so this script checks that the 0.5 and 1.5 files differ from it in shelf life only.

Run after changing anything in src/ or data/processed/:  python build.py
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
PROCESSED = ROOT / "data" / "processed"

SIZES = (8, 10)
SEEDS = range(1, 31)
TIGHTS = (0.5, 1.0, 1.5)
FOOD_SHELF = {"ice_cream": 30.0, "noodles": 60.0, "rice": 90.0}
KEPT = ("name", "lat", "lon", "order_time", "food")


def instance_path(n, seed, tight):
    return PROCESSED / f"n{n}_s{seed}_t{tight:.1f}.json"


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_data():
    restaurant = None
    sets = {}
    for n in SIZES:
        for seed in SEEDS:
            base = read(instance_path(n, seed, 1.0))
            for tight in TIGHTS:
                inst = base if tight == 1.0 else read(instance_path(n, seed, tight))
                where = instance_path(n, seed, tight).name
                assert inst["travel_min"] == base["travel_min"], f"{where}: travel differs"
                for c, b in zip(inst["customers"], base["customers"], strict=True):
                    assert all(c[k] == b[k] for k in KEPT), f"{where}: customer {c['id']} differs"
                    assert c["shelf_life"] == FOOD_SHELF[c["food"]] * tight, \
                        f"{where}: customer {c['id']} shelf life is not base x tightness"
            restaurant = restaurant or base["restaurant"]
            assert base["restaurant"] == restaurant, "restaurant differs between files"
            sets[f"n{n}_s{seed}"] = {
                "customers": [{k: c[k] for k in KEPT} for c in base["customers"]],
                "travel_min": base["travel_min"],
            }
    return {"restaurant": {"name": restaurant["name"], "lat": restaurant["lat"],
                           "lon": restaurant["lon"]}, "sets": sets}


def main():
    data = load_data()
    page = (HERE / "src" / "page.html").read_text(encoding="utf-8")
    solvers = (HERE / "src" / "solvers.js").read_text(encoding="utf-8")
    app = (HERE / "src" / "app.js").read_text(encoding="utf-8")
    scenarios = (HERE / "src" / "scenarios.json").read_text(encoding="utf-8").strip()
    script = (f"const DATA = {json.dumps(data, ensure_ascii=False, separators=(',', ':'))};\n"
              f"const SCENARIOS = {scenarios};\n{solvers}\n{app}")
    out = HERE / "demo.html"
    out.write_text(page.replace("/*__SCRIPT__*/", script), encoding="utf-8", newline="\n")
    print(f"wrote {out.name} ({out.stat().st_size // 1024} KB, {len(data['sets'])} order sets)")


if __name__ == "__main__":
    main()
