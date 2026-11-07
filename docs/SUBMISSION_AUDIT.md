# Final submission audit

Reviewed on 7 November 2026 against `Project_2627T1.pdf`, especially sections 3, 5, 6, 7 and 11. Branch at review: `ll-branch`. No commit or push was performed.

**Status: the source/data layout and tested execution paths are ready for review on your branch. The final eLearn submission is not complete until the outstanding items below are resolved.**

## Outstanding items before final submission

1. **Slide 1:** replace `Group [XX]` and `[Member names]` with the actual group/team details.
2. **Slide 24:** complete the AI acknowledgement with the actual tools, uses, original team contributions and three real representative prompts. The repository currently has no completed acknowledgement. The brief also refers to acknowledgement in a project report; confirm the expected format with the teaching team.
3. **Slide 11:** replace “Amy waits 1 more minute, Cat 12 fewer” with “Amy's arrival stays at 9 minutes; Cat arrives 12 minutes earlier.” Slides 10 and 11 both show Amy arriving at 9.
4. **Slides 6 and 15:** label the grey five-stop path as the *unfiltered nearest-neighbour order*. The experiment baseline actually skips late orders; its delivered route in this example is Dan → Ben → Cat. The displayed delivered count and average wait are correct for this example, but the diagram currently appears to visit late customers.
5. **Approach B pseudocode:** slide 14 shows its priority rules and a trace. Add an explicit loop/termination pseudocode block from `experiments/approach_B/HOW_IT_WORKS.md` to make coverage of section 7(c) unambiguous. Approach A already has concise pseudocode on slide 12.
6. **Third-party libraries:** the brief explicitly asks that used libraries be included. Java uses only its standard library. The chart script uses matplotlib and its dependencies. `requirements.txt` pins the tested matplotlib version (3.10.6, requiring Python 3.10+), but no library bundle is included. Provide an installable dependency bundle appropriate to the marking environment, or obtain clarification that installation from requirements is acceptable. This has not been silently treated as satisfied.
7. **Final packaging:** create the final archive as `G<X>T<X>.zip` using the actual group/team numbers, and upload the completed slides separately. No correctly named final submission ZIP has been created because those numbers are not supplied.
8. **Team scope confirmation:** the retained DOCX/PDF templates describe Python and both V1/V2. The implementation and current presentation use Java and V1 only. The official brief permits Java and requires two distinct approaches; it does not require V2. Confirm the internal template change with the team and use the current README/data notes as the implementation description.

The current slide file was left unchanged so the unresolved team/AI information is not fabricated. A branch push is not the same as a completed eLearn submission.

## Deliverable coverage

| Brief requirement | Audit result |
|---|---|
| Meaningfully different redesigned approaches | Present: Approach A uses deadline sorting and iterative single-stop moves; Approach B uses greedy one-step lookahead |
| ALL source needed for reported experiments | Both solvers, models, generator, scorer, exact reference, baseline, runner and plotting source present. Added a runnable check for the five-customer slide example after the old presentation source archive was removed |
| Data in the format used | Present: original `MY.txt`, metadata, 540 generated experiment JSON files and the fixed worked example |
| Meaningful experiment folders | Present: `approach_A`, `approach_B`, `comparison`; shared classes appear only once |
| README with structure and run instructions | Present: one-command Windows and macOS/Linux entry points, prerequisites, expected outputs, test descriptions and detailed links |
| Third-party libraries included | Open: requirements are documented but matplotlib/dependencies are not bundled |
| Presentation motivation, variant, redesign, results, complexity and limitations | Present in the 28-slide draft; corrections and explicit Approach B pseudocode remain as listed above |
| AI acknowledgement and three prompts | Open: slide 24 still contains placeholders |
| Correct ZIP name and eLearn upload | Open: final packaging/upload not performed |
| Week 13 peer evaluation | Team action outside the repository |

## Checks completed

- Read the project brief and all source, shell/batch/Python scripts, Markdown documentation and metadata. Checked the historical template's DOCX text and both PDF files.
- Scanned every file outside `.git/` and ignored `out/` build/audit output. The inventory below lists each non-generated file; all 540 generated inputs were individually parsed and validated.
- Verified both DOCX/XLSX/PPTX ZIP container integrity and all three PNG files. The spreadsheet contains 71,710 rows on its data sheet (header plus 71,709 records) and an empty second sheet. It is reference material, not a runtime dependency; no claim is made that every spreadsheet cell was compared with the TXT.
- Checked all 71,709 GeoNames records: 19 columns per row, unique IDs, 40 selected populated places within the radius and 33 after deduplication.
- Verified all 540 JSON filenames/settings, customer IDs/counts, finite nonnegative symmetric travel matrices, zero diagonals, coordinate/name linkage to `MY.txt`, order-time range, food shelf-life scaling, and identical scenario data across the three tightness variants. Total generated input size: 13,446,240 bytes, plus the worked example.
- Checked all 1,890 raw CSV rows and 63 summary groups for coverage/counts, zero late deliveries, delivered/cancelled consistency, means and cancellation standard deviations.
- Re-ran all 540 inputs after the final solver adjustment. All 1,890 raw-result rows and all 63 summary rows match the checked-in results after excluding runtime columns.
- Independently enumerated feasible routes without the Java pruning rules for 93 inputs: all 90 five-order cases and seed 1 at eight orders for all three tightness levels. Delivered counts and average waits agree with the exact-reference CSV values.
- Compiled all Java source using `javac --release 17` to check Java 17 API compatibility. Executed with the installed JDK 25; a separate JDK 17 runtime was not available for this review.
- Both algorithm demos, fixed-template checks and the new slide-example checks pass. The slide example independently enumerates all 120 full visiting orders and confirms 15 deliver all customers, with best mean wait 21.4.
- Built a temporary source/data ZIP, extracted it into a fresh directory and ran the Windows and Bash quick-test entry points there. Bash was tested through Git Bash on Windows, not on a physical Mac. The complete Java experiment runner was tested separately; the full `--all` wrapper was not re-run twice solely to repeat the same experiments.
- Regenerated the three charts from the extracted package and visually checked them. Pixel/byte identity with the existing images is not expected across font/rendering environments; the chart values come from the same CSVs.
- Opened the presentation read-only through PowerPoint and exported/inspected previews of all 28 slides. Inspected slide text, speaker notes and chart caches. No obvious static clipping was found. Interactive animation playback was not exercised.
- Checked local Markdown link targets and `git diff --check`. Generated build files remain ignored. New source, scripts and data are not ignored and must be included in the branch commit.

## Fixes made during this audit

- Generated and retained the 540 missing JSON experiment inputs.
- Added `CheckPresentationExample.java` and included it in both quick-test runners, preserving a reproducible source for the five-customer slide example.
- Replaced sorting of each candidate's cancelled-order list in `SolverA.walk` with a linear scan. The returned list remains in ID order, while the implementation now matches the documented O(n) walk and O(n³) per-pass bound. Full comparison results are unchanged except timing.
- Made both full-run scripts return a failure if chart generation fails instead of claiming success with missing/stale plots.
- Added `requirements.txt` with the locally verified matplotlib version and documented its Python requirement.
- Corrected the “best route” label in Approach B's worked example, its claim that order time is ignored, and the heap-complexity explanation. A bottom-up heap can be built in O(n); recalculating all candidate scores is the issue, not an unavoidable O(n log n) heap build.
- Removed unsupported empirical claims about alternative algorithms for which no trial source/results are included. The alternatives remain described as design choices.
- Clarified current Java/V1 scope versus the historical templates and corrected the data notes' false claim that the root project brief was ignored/untracked.
- Corrected the exact-solver comment: minimising total arrival time is equivalent to minimising wait only for a fixed delivered customer set. The implementation already sums arrival minus order time correctly.

## Results and slides: provenance notes

The checked-in CSV runtime values and slide 20 are the original benchmark capture. The current code uses a linear cancellation-list pass rather than a sort, and current rerun timings differ by machine and revision. The audit preserved the original CSVs/charts so the slide values remain traceable. If the team adopts new timing results, regenerate charts and update the slide timing statements together.

The demo classes contain a teaching nearest-neighbour helper that visits the unfiltered order and counts late stops. `NearestNeighbor.java`, used by `RunExperiments`, filters that fixed order by skipping late stops. The demo helper is not used for the 540-input benchmark. This distinction should be retained when explaining the slides.

The historical templates are reference documents, not the submitted implementation contract. Besides Python/V2, their “sum of arrival times” explanation assumes the same delivered set. The current implementation correctly optimises average waiting time for equally large delivered sets even when their members differ.

## Packaging and Git notes

Include `README.md`, both root test scripts, `requirements.txt`, `src/`, `experiments/`, `data/raw/MY.txt`, `data/raw/Metadata.txt`, all `data/processed/*.json`, `results/`, `docs/data_decisions.md`, the completed AI acknowledgement, and the required library bundle. Include this audit for the team's reference if useful. Upload the completed `slides.pptx` alongside the correctly named ZIP.

Keep `.git/`, `out/`, Office lock files and old archives out of the submission ZIP. The raw XLSX and historical problem templates can remain in the working repository but are not needed to run the submitted program. `Project_2627T1.pdf` is already Git-tracked at the root; an ignore rule for `docs/Project_2627T1.pdf` does not untrack it. Do not claim the brief is excluded from a push merely because that ignore rule exists.

The many deleted Java paths in `git status` are expected from consolidating duplicate models and renaming the demos. Commit the new `src/com/delivery/routing/` files and `DemoA.java`/`DemoB.java` together with those deletions. The removal of `Claude outputs/` was requested by the user and was preserved.

## File inventory

| File | Role / disposition |
|---|---|
| `.gitattributes` | Repository hygiene and platform line endings |
| `.gitignore` | Repository hygiene and platform line endings |
| `data/processed/example_n6_s10.json` | Fixed template example; reproduced and checked |
| `data/raw/Metadata.txt` | Dataset schema and attribution/licence metadata |
| `data/raw/MY-All-columns_with_header.xlsx` | Optional reference copy; container and row counts checked |
| `data/raw/MY.txt` | Primary dataset; all rows structurally checked |
| `Deadline-Aware Food Delivery Problem Template.pdf` | Reference document; parsed, not required runtime input |
| `docs/data_decisions.md` | Documentation; reviewed (this audit records its own coverage) |
| `docs/Deadline-Aware Food Delivery Problem Template.docx` | Reference document; parsed, not required runtime input |
| `docs/SUBMISSION_AUDIT.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/approach_A/HOW_IT_WORKS.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/approach_A/README.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/approach_A/src/com/delivery/routing/DemoA.java` | Submitted Java source; reviewed and compiled |
| `experiments/approach_A/src/com/delivery/routing/SolverA.java` | Submitted Java source; reviewed and compiled |
| `experiments/approach_B/HOW_IT_WORKS.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/approach_B/README.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/approach_B/src/com/delivery/routing/DemoB.java` | Submitted Java source; reviewed and compiled |
| `experiments/approach_B/src/com/delivery/routing/SolverB.java` | Submitted Java source; reviewed and compiled |
| `experiments/comparison/plot_results.py` | Chart source; executed from fresh extraction |
| `experiments/comparison/README.md` | Documentation; reviewed (this audit records its own coverage) |
| `experiments/comparison/run_all.bat` | Run entry point / pipeline; checked |
| `experiments/comparison/run_all.sh` | Run entry point / pipeline; checked |
| `Project_2627T1.pdf` | Reference document; parsed, not required runtime input |
| `README.md` | Documentation; reviewed (this audit records its own coverage) |
| `requirements.txt` | Pinned chart dependency; does not bundle its packages |
| `results/fig_cancelled.png` | Result chart; file integrity and chart generation checked |
| `results/fig_gap_to_best.png` | Result chart; file integrity and chart generation checked |
| `results/fig_runtime.png` | Result chart; file integrity and chart generation checked |
| `results/raw_results.csv` | Benchmark data; all rows checked |
| `results/README.md` | Documentation; reviewed (this audit records its own coverage) |
| `results/summary.csv` | Benchmark data; all rows checked |
| `run_tests.bat` | Run entry point / pipeline; checked |
| `run_tests.sh` | Run entry point / pipeline; checked |
| `slides.pptx` | 28-slide draft; required corrections listed above |
| `src/com/delivery/routing/Customer.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/routing/DeliveryInstance.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/routing/RouteResult.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/BruteForce.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/CheckPresentationExample.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/CheckWorkedExample.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/Common.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/Evaluate.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/GenerateInstance.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/Instance.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/NearestNeighbor.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/Places.java` | Submitted Java source; reviewed and compiled |
| `src/com/delivery/tools/RunExperiments.java` | Submitted Java source; reviewed and compiled |
| `data/processed/n*_s*_t*.json` (540 files) | Every generated file parsed and validated; include in submission |

Total: **587 files**, excluding Git internals and ignored build/audit output.
