1. The dataset was obtained from the following source: https://www.geonames.org/
2. MY.txt was got from the zip file at hgttps://download.geonames.org/export/dump/

3. Metadata.txt has the metadata content downloaded from the Source GeoNames
4. MY-All-columns_with_header.xlsx contains the same data in My.txt but with headers added. 
5. FeatureStatistic (if required) is present at
https://www.geonames.org/statistics/malaysia.html

## Project structure

| Folder | Contents |
|---|---|
| `data/raw/` | `MY.txt` (GeoNames, never edited), its metadata, and an `.xlsx` copy with headers |
| `data/processed/` | 540 instance files (`n8_s1_t1.0.json` = 8 orders, seed 1, tightness 1.0) plus the template's worked example |
| `src/com/delivery/tools/` | Shared Java tools: instance generator, scorer, brute force, nearest-neighbour baseline, experiment runner |
| `experiments/approach_A/` | Greedy with one-step lookahead (`SolverA`) — **Approach B** in the presentation |
| `experiments/approach_B/` | Deadline order, then improve by moving stops (`SolverB`) — **Approach A** in the presentation |
| `experiments/comparison/` | How to run everything (`README.md`, `run_all.bat`, `run_all.sh`) and the chart script |
| `results/` | `raw_results.csv`, `summary.csv`, charts, and a guide to the columns |
| `docs/` | Problem template and data decisions |

## How to run

Requires Java 17+ (Python 3 + matplotlib only for the charts). From this folder:

    experiments\comparison\run_all.bat        (Windows)
    bash experiments/comparison/run_all.sh    (macOS / Linux)

Details and single-instance commands: `experiments/comparison/README.md`.

