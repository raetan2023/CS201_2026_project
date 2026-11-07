# Slide flow

The deck as planned, slide by slide. Each **click** is one press of the right
arrow in the presenter (`manim-slides`). Numbers on screen come from
`config.py`, which is generated from the Java solvers' output (`ExportConfig`),
so nothing is typed by hand. 91 clicks in total (83 before the appendix),
about 3.5 minutes of animation; the talk itself runs longer with speaking time.

**Status: plan.** Method names below are the names the scenes will have; no
scene is rendered yet. Results slides (3.3, 3.4) wait for the runs on the
shared instance files.

## How to use this file

Edit this file directly to describe the flow you want, then ask Claude to
implement it. Any of these work:

- **Reorder:** move a slide's whole `###` block up or down.
- **Remove:** delete the block, or add `> REMOVE` under its heading.
- **Change a slide:** edit its clicks or point, or add a line starting
  `> CHANGE:` describing what you want.
- **Add a slide:** add a new `###` block with `> NEW` under the heading and
  describe its point and clicks.
- **Move between sections:** move the block under another `##` section.

Rough wording is fine. The method names in backticks tell Claude which code
each slide is.

**Notation:** Approach A = *plan first, then fix* (deadline order + moves).
Approach B = *one stop at a time* (greedy with lookahead). Customers in the
running example are **Amy, Ben, Cat, Dan, Eve** (A–E in the problem
statement), so a letter on screen never means an approach. Positions and
times are minutes of driving; the restaurant sits at 0, west is negative.
d = deadline, a = arrival, o = order time, w(i, j) = travel time.
Colors: Approach A purple, Approach B blue, nearest neighbour grey, best gold,
rider path orange, on time green, late or cancelled red, deadline tick amber,
fresh window dashed grey.

**Running example** (all orders at t = 0, so deadline = shelf life):

| Customer | Position | Food | Deadline |
|---|---|---|---|
| Amy | +9 | ice cream | 30 |
| Ben | −13 | noodles | 60 |
| Cat | +8 | noodles | 60 |
| Dan | −7 | rice | 90 |
| Eve | +14 | ice cream | 30 |

**The time chart** (`TimeChart` in `components.py`): position across, time
running **down**. Each customer has a dashed fresh window from t = 0 to an
amber tick at the deadline; the path hitting the line above the tick means on
time. It is built on 1.4 and stays the same object from 1.4 through 3.1.

**If the talk runs long, cut in this order:** the appendix is already after
the closing slide; then 2.1 (move to the appendix as A.1, keep its last
caption as a one-liner on 2.2); then 1.5 down to its first click; then 3.2's
two panels down to one.

---

## Section 1: Problem (`scenes/s1_problem.py`, 31 clicks)

### 1.0 Title slide
`slide_1_0_title`
- **Clicks:**
  1. "Food Before It Spoils", subtitle "Redesigning the TSP for deadline-aware delivery", group number, member names.

### 1.0 Section slide: Problem
`slide_1_0_section`
- **Clicks:**
  1. Big "1", "Problem", subtitle "One rider, five orders, three shelf lives".

### 1.1 Hook: the closest stop is not the right stop
`slide_1_1_hook`
- **Point:** a route can look efficient and still fail customers. No formulas yet.
- **Clicks:**
  1. Road with the restaurant (grey square) at 0; Amy, Ben, Cat, Dan, Eve fade in at their positions with food icons (cone, noodle bowl, rice bowl). Caption: "One rider. Five orders. One trip."
  2. A countdown ring grows on each icon; all start together and the two cones (Amy, Eve) empty first. Caption: "Ice cream lasts 30 min, noodles 60, rice 90."
  3. A grey rider always drives to the closest stop: Dan 7, Ben 13, Cat 34, Amy 35, Eve 40. Amy's and Eve's cones turn red as it arrives. Caption: "Always go to the closest: both ice creams melt."
  4. Scoreboard: "Delivered 3 of 5", "Average wait 18.0 min". Caption: "And yet its average wait looks great."
  5. Dim overlay: "What does a 'better' route even mean?"

### 1.2 Why the classic TSP is not enough
`slide_1_2_classic_tsp`
- **Point:** five classic assumptions break; each change is one click.
- **Clicks:** five dots and a closed tour on the left; a two-column table ("Classic TSP" | "Our use case") builds on the right, one row per click.
  1. Closed tour draws itself. "Classic TSP: shortest closed tour, every city once."
  2. Countdown rings appear on the dots. Row: "Every city equally important → each order has a deadline".
  3. Edge labels change from km to arrival times. Row: "Only the order matters → arrival time matters".
  4. One dot greys out with a ✕. Row: "Visit every city → cancel orders that can't be on time".
  5. The edge back to the restaurant disappears. Row: "Return to start → route ends at the last customer".
  6. The goal label changes. Row: "Shortest distance → most on time, then least wait".
  7. Caption: "This is the Minimum Latency Problem with deadlines: NP-hard. We redesign heuristics, not exact solvers."

### 1.3 The new problem
`slide_1_3_variant`
- **Point:** input, output, rules, and goals compared in order.
- **Clicks:**
  1. Input box: restaurant, n orders known at t = 0, w(i, j), order time o, shelf life ℓ.
  2. Output box: "the visiting order; anyone left out is cancelled".
  3. Rules box: one rider, never returns, no time at the door, wait = a − o, "late = 0, always".
  4. Deadline table: V1 d = o + ℓ; V2 d = o + min(ℓ, 60), with "min(ℓ, 60)" boxed amber. Caption: "V2: no one waits more than an hour."
  5. Goals, numbered: "1. Fewest cancelled" (large), "2. Lowest average wait" (smaller). A thumbnail of 1.1's scoreboard slides in, its "3 of 5" turns red. Caption: "Nearest neighbour loses on goal 1 before we even look at wait."
  6. "Total wait = Σa − Σo, and Σo is fixed." Caption: "So lowering the average wait means arriving earlier overall."

### 1.4 Reading the picture
`slide_1_4_time_chart`
- **Point:** one chart shows where and when; this exact chart is used for the rest of the talk.
- **Clicks:**
  1. The road from 1.1 slides to the top; a time axis grows downward, ticks 0 to 60 min.
  2. Amy's fresh window: dashed line from t = 0 down to an amber tick at 30.
  3. A path from the restaurant reaches Amy at 9: green dot above the tick, label "on time".
  4. A second path reaches Amy at 35: red hollow dot below the tick, label "spoiled". It fades out.
  5. The other four windows drop in (Dan's runs off the bottom: 90). Caption: "On time ⇔ the path hits the line above the tick."

### 1.5 The data
`slide_1_5_data`
- **Point:** real Kuala Lumpur locations, and everything we add is documented.
- **Clicks:**
  1. Funnel: "MY.txt: 71,709 rows" → "class P within 6 km of Bukit Bintang Plaza: 40" → "duplicate coordinates dropped: 33".
  2. The 33 locations as dots around the restaurant with a 6 km circle (latitude/longitude). Caption: "MY.txt has no restaurants, so a mall stands in."
  3. Three tags fly onto one dot: "order time (last 10 min)", "food: 30 / 60 / 90", "travel = distance ÷ 20 km/h". Caption: "MY.txt is never edited; additions live in the instance files."
  4. Experiment grid: n = 5, 8, 10, 20, 50, 100 × tightness 0.5, 1.0, 1.5 × 30 seeds.

### 1.6 The question
`slide_1_6_question`
- **Clicks:**
  1. The five-customer time chart with no path. Caption: "In what order do we visit them, quickly, when deadlines conflict?"
  2. Two arrows from the restaurant: purple "Approach A: plan everything, then fix it", blue "Approach B: decide one stop at a time".

---

## Section 2: Two approaches (`scenes/s2_approaches.py`, 21 clicks)

### 2.0 Section slide: Two approaches
`slide_2_0_section`
- **Clicks:**
  1. Big "2", "Two approaches", subtitle "Plan first, or decide as you go".

### 2.1 One fact both approaches rely on
`slide_2_1_dropping_is_free`
- **Point:** an order that is out of reach now is out of reach forever, so cancelling it costs nothing.
- **Clicks:**
  1. Rider at c at time t, order v; the straight segment labelled w(c, v); "t + w(c, v) > d_v" in red.
  2. A detour c → x₁ → x₂ → v in orange; two length bars side by side, the detour bar always at least as long. Label "triangle inequality".
  3. The chain builds term by term: a_v = t + w(c, x₁) + … + w(x_k, v) ≥ t + w(c, v) > d_v.
  4. Caption: "Cancelling is free. If anything went wrong, it went wrong earlier."

### 2.2 Approach A: the plan
`slide_2_2_a_plan`
- **Point:** A holds a complete plan (a list of every customer) and scores it by driving it.
- **Clicks:** five cards on top, the time chart below.
  1. Five name cards in a row, label "the plan = List of all n customers". Caption: "Cancelled customers stay in the list."
  2. Walk rule: a cursor steps along the cards; each card turns green ("drive there") or red ("too late: cancel, stay put") while the path draws below.
  3. Step 1, sort by deadline: the cards reorder to Amy 30, Eve 30, Ben 60, Cat 60, Dan 90. Walk: Amy 9, Eve 14, Ben 41, Cat ✗ (would be 62), Dan 47. Scoreboard: delivered 4, average wait 27.75.
  4. Caption: "Sorting by urgency ignores the map: Cat is lost."

### 2.3 Approach A: fixing the plan
`slide_2_3_a_moves`
- **Point:** keep making the single best move until no move helps.
- **Clicks:**
  1. "A move: take one card out, put it back somewhere else." A ghost card demonstrates once and returns.
  2. Pass 1: a counter runs 0 → 20 moves, 6 flash as improving; the best lifts Ben to the end. The path re-forms (Cat is picked up at 20 on the way back). Scoreboard: 5 delivered, 27.75 → 23.80.
  3. Pass 2: Cat lifts to the front. The 1-minute leg Cat → Amy pulses. Scoreboard: 23.80 → 21.40.
  4. Pass 3: counter 0 → 20, none flash. "No move helps: stop."
  5. Pseudocode box (sort; repeat: try every move, keep the best if better; stop). Caption: "Every accepted move strictly improves the plan, so it always stops."

### 2.4 Approach B: one stop at a time
`slide_2_4_b_idea`
- **Point:** before each move, B asks how many other orders the move would doom.
- **Clicks:** the time chart, rider at the restaurant.
  1. Dashed arrows to all five with labels "lost 0" (Amy, Cat, Dan, Eve) and "lost 2" (Ben); on Ben's arrow, Amy's and Eve's windows flash red.
  2. Rules funnel, one box per rule: reachable → fewest lost → shortest travel → least slack → lowest id. The four "lost 0" candidates fall through to "shortest travel": Dan (7 min) wins.
  3. Data structure strip: `visited[ ]`, `alive[ ]`. Caption: "No heap: every score changes after every move."

### 2.5 Approach B in action
`slide_2_5_b_run`
- **Clicks:**
  1. At Dan, t = 7: Cat (lost 0, 15 min) beats Amy (lost 0, 16 min); Eve would doom Amy; Ben would doom 2. Path to Cat at 22.
  2. Amy at 23, then Eve at 28 (Ben would doom Eve).
  3. Ben at 55. Scoreboard: 5 delivered, average wait 27.00.
  4. Caption: "No food spoils, but rice first sent the rider the wrong way, and B never takes a choice back."

---

## Section 3: Evaluation (`scenes/s3_evaluation.py`, 20 clicks)

### 3.0 Section slide: Evaluation
`slide_3_0_section`
- **Clicks:**
  1. Big "3", "Evaluation", subtitle "Who wins, when, and why".

### 3.1 Head to head on the running example
`slide_3_1_head_to_head`
- **Point:** on this example Approach A finds the true best.
- **Clicks:**
  1. Three small time charts side by side, each with its scoreboard: nearest neighbour (3 delivered, 18.0), B (5, 27.0), A (5, 21.4).
  2. A 10 × 12 grid of 120 squares, one per visiting order; 105 fade grey (someone cancelled), 15 stay green (all five delivered).
  3. 14 of the green squares dim; one turns gold, "21.4"; an arrow runs to A's chart. Caption: "All 120 orders checked: A's answer is the best possible here."

### 3.2 Each approach's trap
`slide_3_2_traps`
- **Point:** they fail on opposite structures.
- **Clicks:** two small time charts.
  1. Left, P (+5, d 10), Q (−5, d 16), R (−1, d 90): B goes to R first ("looks safe"), then P and Q can't both be saved. 2 of 3.
  2. A on the same: P 5, R 11, Q 15. 3 of 3, average 10.33.
  3. Right, S (−5, d 16), T (+5, d 31), U (−12, d 24), V (+4, d 20): A's deadline order loses U; V and T would both need to move ahead of S; "no single move helps". 3 of 4.
  4. B on the same: V 4, T 5, S 15, U 22. 4 of 4.
  5. Caption: "B checks orders one at a time. A changes the plan one stop at a time."

### 3.3 Results: orders cancelled
`slide_3_3_results_cancelled`
- **Point:** which approach cancels fewer orders, and how close to the best.
- **Clicks:** numbers from `results/summary.csv`.
  1. Grouped bars, average cancelled per n at tightness 1.0: nearest neighbour, B, A, best (n ≤ 10).
  2. Small multiples for tightness 0.5 and 1.5.
  3. Gap to brute force for n ≤ 10, V1 and V2.
- > TODO: fill from the shared-instance runs. The synthetic pre-check (random points, not MY.txt) had A cancelling fewer orders than B in 16 of 18 settings; do not show it as a result.

### 3.4 Results: speed
`slide_3_4_results_speed`
- **Clicks:**
  1. Run time vs n, log scale, A and B lines.
  2. Caption: "A pays for its fixes in time; B stays near-instant." (check against the real runs)

### 3.5 Complexity and trade-offs
`slide_3_5_tradeoffs`
- **Clicks:** a table, one row per click, columns A (purple) and B (blue).
  1. Idea: full plan, then improve | one stop at a time.
  2. Data structure: list of all n customers | `visited` / `alive` arrays.
  3. Time: O(n log n) + O(n³) per pass × passes | O(n³), fixed.
  4. Space: O(n) | O(n).
  5. Can undo an early choice / bring a cancelled order back: yes | no.
  6. Trap: needs two moves at once | checks orders one at a time.

---

## Section 4: Wrap-up (`scenes/s4_wrapup.py`, 11 clicks)

### 4.0 Section slide: Wrap-up
`slide_4_0_section`
- **Clicks:**
  1. Big "4", "Wrap-up", subtitle "What we learned, and what's next".

### 4.1 Recap
`slide_4_1_recap`
- **Clicks:**
  1. The 1.1 road with all five icons green. "Goals in order: most on time, then least wait." Purple and blue thumbnails: "Plan first, then fix" and "Decide one stop at a time".

### 4.2 Limits and next steps
`slide_4_2_limits`
- **Clicks:** one point per click; the previous point dims.
  1. "Static model": no new orders mid-route, one rider, straight-line travel times.
  2. "Both are local": A stops at a one-move optimum, B never looks back. Thumbnail of 3.2.
  3. "Next": try two-stop moves; start A from B's route instead of deadline order.

### 4.3 What makes this stand out
`slide_4_3_x_factor`
- **Clicks:**
  1. "A picture where 'on time' is visible": time chart thumbnail.
  2. "A proof that cancelling is free": the 2.1 chain.
  3. "Exact answers to grade against": the 120-square grid, plus paired traps.

### 4.4 AI usage
`slide_4_4_ai_usage`
- **Clicks:**
  1. Tools used, and where: design brainstorming, code drafting, examples, slides. What is our own work.
  2. Three representative prompts (required by the brief, §11.3).

### 4.5 Closing
`slide_4_5_closing`
- **Clicks:**
  1. "Thank you", "Questions?"

### A.1 Appendix (backup): the V2 cap changes the route
`slide_a_1_v2_example`
- **Clicks:** the template's worked example (n = 6, seed 10).
  1. Best V1 route 1→2→5→4→6→3: all 6 delivered, average 29.0, but Sentul waits 62.7.
  2. V2 cuts Sentul's deadline from 84.7 to 54.7: best route 1→2→5→3→4→6, average 29.8, longest wait 55.2.
  3. Caption: "A fairness cap costs 0.8 min on the average."

### A.2 Appendix (backup): how many passes does A need?
`slide_a_2_passes`
- **Clicks:**
  1. Passes vs n from the real runs (scatter, one dot per instance).
  2. Caption: "Passes grow with n and with looser deadlines; total time is passes × O(n³)."

### A.3 Appendix (backup): why B has no heap
`slide_a_3_no_heap`
- **Clicks:**
  1. A heap keyed on "lost count": after one move every key changes, so the heap rebuilds each step (O(n log n)); a linear scan is O(n).
  2. Contrast: the deadline is fixed, so sorting once is enough for A's start plan.

### A.4 Appendix (backup): data limitations
`slide_a_4_data_limits`
- **Clicks:**
  1. Mall instead of a restaurant; five villages share one coordinate; some points are not real customers (city centre, palace, park); locations repeat above n = 33.
