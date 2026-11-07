# Deadline-Aware Food Delivery Routing

CS201 project: **Beyond the Classic TSP**.

This project compares two redesigned routing algorithms for one rider delivering orders with different food shelf lives. Unlike classic TSP, the route ends at the last customer and orders may be cancelled. The objectives are, in order: **deliver as many orders on time as possible**, then **minimise the average waiting time of delivered orders**.

The experiments use V1: `deadline = order time + shelf life`. All orders are known when the rider leaves at time 0. The optional V2 waiting-time cap in the problem template is not part of these experiments.

## 1. Requirements

- **JDK 17 or newer**, with both `java` and `javac` on PATH. The Java code uses only the standard library; no Maven, Gradle or external JARs are required.
- **Python 3.10+ with matplotlib**, only to regenerate the charts. Python is not required to compile the algorithms, run tests or produce CSV results.
- Run commands from the **project root**, the folder containing this README. Extract the submission ZIP before running it.

Check Java:

```text
java -version
javac -version
```

For chart generation, install matplotlib in your Python environment:

```text
python -m pip install -r requirements.txt
```

On macOS/Linux, use `python3` instead of `python` where needed. The Bash script uses `python3`; the Windows script uses `python`.

## 2. Folder structure and algorithm names

| Path | Purpose |
|---|---|
| `src/com/delivery/routing/` | Shared `Customer`, `DeliveryInstance` and `RouteResult` model classes |
| `src/com/delivery/tools/` | Shared data generator, scorer, nearest-neighbour baseline, exact solver, worked-example checks and experiment runner |
| `experiments/approach_A/src/com/delivery/routing/` | `SolverA.java` (plan-and-fix) and `DemoA.java` tests |
| `experiments/approach_B/src/com/delivery/routing/` | `SolverB.java` (greedy) and `DemoB.java` tests |
| `experiments/comparison/` | Windows/Bash experiment scripts, plotting script and detailed run guide |
| `data/raw/` | Original GeoNames `MY.txt`, metadata and an optional spreadsheet copy |
| `data/processed/` | Worked-example JSON and generated experiment JSON inputs |
| `results/` | Per-instance results, aggregated results, charts and a column guide |
| `docs/` | Data decisions, historical problem template, and final submission audit |
| `slides.pptx` | Current presentation draft; outstanding corrections are listed in `docs/SUBMISSION_AUDIT.md` |
| `requirements.txt` | Tested matplotlib version for chart regeneration |
| `run_tests.bat` / `run_tests.sh` | One-command entry points for Windows and macOS/Linux |
| `out/` | Compiled classes generated locally; excluded from submission |

**Folder names, Java solver names, presentation labels and CSV method names are aligned:**

| Presentation label | Algorithm | Class used by comparison runner | CSV method |
|---|---|---|---|
| Approach A | Sort by deadline, then improve the plan by moving stops | `approach_A/.../SolverA.java` | `A_plan_fix` |
| Approach B | Greedy selection with one-step lookahead | `approach_B/.../SolverB.java` | `B_greedy` |
| Baseline | Nearest neighbour, dropping late orders | `NearestNeighbor.java` | `NN` |
| Exact reference | Brute force for at most 10 orders | `BruteForce.java` | `BRUTE` |

Each approach folder contains only its own solver and demo. The three model classes are shared in `src/com/delivery/routing/`, and the distinct `DemoA` and `DemoB` class names allow all source files to compile together.

## 3. Run tests and experiments

Open a terminal in the extracted project folder. **No separate compilation commands are needed.** Choose your operating system:

| What to run | Windows (PowerShell / Command Prompt) | macOS / Linux (Terminal) |
|---|---|---|
| Quick tests | `.\run_tests.bat` | `bash run_tests.sh` |
| Tests + all 540 experiment inputs + charts | `.\run_tests.bat --all` | `bash run_tests.sh --all` |

Using `bash run_tests.sh` does not require changing file permissions with `chmod`.

### Quick tests

The script compiles the code, runs five plan-and-fix examples (`DemoA`), three greedy examples (`DemoB`), the shared scorer/exact-solver checks against the worked example, and the five-customer slide example (including all 120 permutations). It stops with a nonzero exit code on a compilation or test failure.

Success ends with **`ALL TEST SUITES PASSED.`** The worked-example checks include the best route `[1, 2, 5, 4, 6, 3]` and average wait of 29.0 minutes. Only JDK 17+ is needed. Test output goes into `out/tests/`; existing experiment data and results are preserved.

### Full experiment run (`--all`)

After the quick tests pass, the script generates all 540 inputs, runs both approaches and the reference methods, writes result CSVs, and attempts to regenerate charts. Allow several minutes. Python with matplotlib is required for charts; the Java tests and CSV generation do not need Python.

The full run replaces `out/`, generated inputs, result CSVs and charts. A chart error now causes the full command to return a failure status, rather than claiming that all outputs were produced. If plotting fails, install matplotlib and rerun just the plotting step:

| Windows | macOS / Linux |
|---|---|
| `python experiments/comparison/plot_results.py` | `python3 experiments/comparison/plot_results.py` |

For manual compilation, individual inputs and other advanced commands, see [the detailed experiment guide](experiments/comparison/README.md). Each approach's README also documents its standalone tests.

### Expected outputs

| Output | Expected contents |
|---|---|
| `data/processed/n*_s*_t*.json` | 540 inputs: order counts 5, 8, 10, 20, 50, 100; shelf-life multipliers 0.5, 1.0, 1.5; seeds 1–30 |
| `data/processed/example_n6_s10.json` | Fixed six-customer worked example, separate from the experiment grid |
| `results/raw_results.csv` | 1,890 data rows, one per input and applicable method |
| `results/summary.csv` | 63 data rows, aggregated over 30 seeds per setting and method |
| `results/fig_cancelled.png` | Cancelled orders by input size and shelf-life tightness |
| `results/fig_gap_to_best.png` | Cancellation gap relative to brute force on small inputs |
| `results/fig_runtime.png` | Runtime comparison |

The generator is deterministic. Timing measurements vary by machine. Each returned route is independently scored; the experiment runner rejects routes with late deliveries. Compare cancelled orders first, then average wait when delivered counts tie. See [results/README.md](results/README.md) for column definitions.

## 4. Dataset and reproducibility

The primary source is the supplied Malaysian [GeoNames dataset](https://www.geonames.org/), distributed as `MY.txt` through the [GeoNames download directory](https://download.geonames.org/export/dump/). The original file is retained unchanged; `Metadata.txt` documents its fields. The spreadsheet copy is for reference and is not read by the code.

The generator uses GeoNameID, name, latitude, longitude and feature class. It selects populated places within 6 km of Bukit Bintang Plaza and removes duplicate coordinates, leaving 33 locations. Larger inputs reuse locations. Derived attributes are order time, food type, shelf life and a travel-time matrix calculated from straight-line distance at 20 km/h. Inputs store these attributes as JSON so all algorithms receive the same data.

Selection rationale, preprocessing, parameter choices and limitations are documented in [docs/data_decisions.md](docs/data_decisions.md). The Java generator uses seeded `java.util.Random`; its generated inputs differ from the older Python template's random sequence. The template's worked example is therefore reconstructed from its fixed table and checked separately.

### How the 540 inputs are generated

`run_all.bat` / `run_all.sh` calls `GenerateInstance --all` before calling `RunExperiments`. The generator uses the settings in `Common.java`:

**6 order counts × 3 shelf-life multipliers × 30 seeds = 540 JSON inputs.**

For each order count and seed, it shuffles the 33 available locations, generates order times within the 10 minutes before departure (shifting the latest to time 0), and randomly assigns ice cream, noodles or rice to each order. Above 33 orders, it repeats shuffled rounds of locations. Travel times are calculated from coordinates, not randomly assigned.

The seed makes these random choices repeatable: the same settings and unchanged source dataset produce the same input. All algorithms read the same JSON inputs. The three multiplier variants for a given order count and seed share the same locations, order times and food types; only shelf lives differ.

| Multiplier (`--tight`) | Ice cream | Noodles | Rice | Meaning |
|---|---|---|---|---|
| 0.5 | 15 min | 30 min | 45 min | Shorter shelf lives, stricter deadlines |
| 1.0 | 30 min | 60 min | 90 min | Base assumptions |
| 1.5 | 45 min | 90 min | 135 min | Longer shelf lives, more relaxed deadlines |

`effective shelf life = base shelf life × multiplier`, and `deadline = order time + effective shelf life`. For example, ice cream ordered at time -5 has a deadline of 10, 25 or 40 minutes after departure for multipliers 0.5, 1.0 or 1.5 respectively. These are experimental assumptions for comparing deadline pressure, not measured food-safety limits.

A file named `n8_s1_t0.5.json` contains 8 orders, uses seed 1, and applies multiplier 0.5. There are 180 randomly generated scenarios, each evaluated under three shelf-life settings; the 540 inputs are not all independent random scenarios. Thirty seeds per setting allow the results to describe variation instead of relying on one example.

## 5. Submission checklist

The brief requires source/data in **`G<X>T<X>.zip`** (replace the placeholders with your group and team numbers), plus **presentation slides**, through eLearn by **Week 12 Sunday, 11 pm**. Peer evaluation is due in class in Week 13.

### Source and data ZIP

- [ ] Include this README, `run_tests.bat`, `run_tests.sh`, `requirements.txt`, `src/`, and `experiments/` with all Java source and reproduction scripts.
- [ ] Include `data/raw/MY.txt`, `data/raw/Metadata.txt`, and all **540 generated JSON inputs**, plus the worked example, in `data/processed/`. The 540 inputs and worked example are now included.
- [ ] Include `results/` and `docs/data_decisions.md` so the submitted results and preprocessing are documented.
- [ ] Include any additional source/data needed to reproduce examples or experiments shown in the final slides. The current draft is `slides.pptx`; upload the completed slides alongside the source ZIP.
- [ ] Include third-party libraries used, as required by the brief, with installation/run instructions. Java needs none; chart regeneration uses matplotlib, which is **not currently bundled**. A pip command alone does not fulfil the brief's instruction to include libraries.
- [ ] Extract the final ZIP into a fresh folder and run the quick tests and full experiment command in section 3.
- [ ] Exclude `.git/`, `out/`, Office lock files and obsolete archives. The assignment brief and optional spreadsheet copy are not required runtime inputs.

### Presentation and acknowledgement

- [ ] Include motivation, why classic TSP is insufficient, and the new inputs, outputs and constraints.
- [ ] Explain both meaningfully different approaches, their data structures, pseudocode, complexity, results, trade-offs, limitations and interesting findings.
- [ ] Use consistent Approach A/B labels, following the mapping in section 2.
- [ ] Upload the final slides separately, with team/member details and final experimental results. Verify slide/video playback before submission.
- [ ] Acknowledge AI tools used, how/where they were used, which contributions are original, and **three actual representative prompts**. Include this documentation in the submission and acknowledge AI use in the slides. Section 11.3 refers to a project report although the deliverables list specifies code and slides; confirm the intended report format with the teaching team.
- [ ] Ensure all team members can explain and defend the implementation and results.

This README covers folder structure, prerequisites, execution, test cases, data provenance and packaging instructions. The unchecked items above must be completed before the submission as a whole meets the brief. See [the final audit](docs/SUBMISSION_AUDIT.md) for verified files and remaining issues.

## 6. Troubleshooting

| Symptom | Action |
|---|---|
| `javac` is not recognised | Install a JDK, add its `bin` directory to PATH and reopen the terminal |
| Missing `Customer`, `DeliveryInstance` or `RouteResult` during compilation | Use the test runner, which includes the shared classes automatically |
| Missing `MY.txt` or relative-path errors | Run from the extracted project root and check `data/raw/MY.txt` exists |
| `missing ... (run GenerateInstance --all first)` | Generate all 540 inputs before running `RunExperiments` |
| `No module named matplotlib` | Install it using the same Python interpreter used for chart generation |
| `ClassNotFoundException` | Use the test runner to compile and run with the correct classpath |
