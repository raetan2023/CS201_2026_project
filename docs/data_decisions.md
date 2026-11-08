# Data decisions

How we turn `data/raw/MY.txt` into test data, and why. The implementation, problem template and submitted experiments use Java with one deadline rule: **deadline = order time + shelf life**. The DOCX/PDF templates describe this same problem and the shared Java tools.

---

## How the data is built

| Step | Result |
|---|---|
| Start from `MY.txt` | 71,709 rows, all with the expected 19 columns |
| Restaurant | Bukit Bintang Plaza (GeoNameID 7284944, class S / MALL) |
| Keep populated places (class P) within 6 km of the restaurant | 40 places |
| Drop rows with exactly the same coordinates | 7 dropped, **33 customer locations** left |
| Per test case: pick n locations, then add an order time, food type and shelf life | One instance file |

**Columns used from `MY.txt`:** GeoNameID, name, latitude, longitude, feature class.
`MY.txt` itself is never edited.

**Attributes we add** (stored in the instance files and linked by GeoNameID):

| Attribute | Value |
|---|---|
| Order time | Random, within the 10 min before the rider leaves; the last order is at t = 0 |
| Food type and shelf life | Ice cream 30 min, noodles 60 min, rice 90 min, picked at random |
| Travel time | Straight-line distance ÷ 20 km/h, in minutes |

---

## Decisions

| # | Question | Decision | Why |
|---|---|---|---|
| 1 | Are 33 locations enough? | Yes. Class P only, no hotels | See [Why hotels are left out](#why-hotels-are-left-out) |
| 2 | Keep unrealistic places (Kuala Lumpur, Istana Negara, Lake Gardens, Bukit Bintang)? | Keep all, and state it as a limitation | The solver only uses coordinates, so each point stands for an address in that area. Removing any location reshuffles every test case |
| 3 | Which row to keep for duplicate coordinates? | No change to the rule | The row kept is already a `PPL` in all 7 cases. Duplicates share coordinates, so only the name could differ |
| 4 | Are Kampung Baru and Kampong Baharu the same place? | Keep both | They are 1.4 km apart, and nothing in the data proves they are the same place. Kampong Baharu is also in the template's worked example |
| 5 | Use the population column? | No | Many of the 33 locations show 0, and nothing in the template needs it |
| 6 | Keep straight-line travel times? | Yes | Set by the template. They also obey the triangle inequality, which Approach B's "definitely lost" check relies on |
| 7 | Include the `.xlsx` in the submission? | Keep it in the repo, leave it out of the zip | The brief asks for data "in the format used", and nothing reads it |

### Why hotels are left out

Hotels are realistic delivery addresses, and adding them would avoid reusing locations at
n = 50 and 100. We still leave them out, for three reasons:

| | Populated places (P) | Hotels (HTL) |
|---|---|---|
| Unique locations within 6 km | 33 | 363 |
| Median distance from restaurant | 3.42 km | 1.34 km |
| Within 1 km of restaurant | 3 | 160 |

1. **It would make the problem easier.** Hotels cluster around Bukit Bintang, so trips get
   shorter and fewer deadlines conflict. The template's settings were tuned so that the problem
   is "hard but not hopeless".
2. **It would change the shared data.** Any change to the location pool reshuffles every test
   case. Removing just 3 locations changed 5 of the 6 customers in the n = 6, seed 10 example.
3. **It isn't needed.** Runs up to n = 33 use distinct locations, and the template treats
   n = 50 and 100 as mainly speed tests.

---

## Rows dropped as duplicates

| Dropped | Code | Same coordinates as (kept) |
|---|---|---|
| Kampong Semerah Padi (1764614) | PPL | Pudu Ulu (1749799) |
| Kampong Puah Seberang (1765340) | PPL | Kampong Sungai Merali (1763915) |
| Kampong Puah (1765341) | PPL | Kampong Sungai Merali (1763915) |
| Kampong Chempedak (1769845) | PPL | Kampong Sungai Merali (1763915) |
| Kampong Bandar Dalam (1771195) | PPL | Kampong Sungai Merali (1763915) |
| Kampong Haji Abdullah Hukum (1769086) | PPLX | Seputeh (1744508) |
| Kampong Datok Keramat (1769671) | PPLX | Kampong Datuk Keramat (1768334) |

---

## Known data limitations (say these in the presentation)

- **No restaurants in the data.** `MY.txt` has 0 rows with feature code `REST`, so a mall stands
  in for the restaurant.
- **Some coordinates look approximate.** Five villages share one exact point (the four above
  plus Kampong Sungai Merali).
- **Some points aren't real customers.** Kuala Lumpur is the capital's centre point, Istana
  Negara is the national palace and Lake Gardens is a park. Bukit Bintang is only 0.1 km from
  the restaurant.
- **Locations repeat above n = 33.** Customers at the same spot are 0 minutes apart, so use
  n = 50 and 100 for speed, not for route quality.

---

## Instance files

An instance file is **one ready-to-run test case**: the restaurant, n customers with their order
time, food and shelf life, and the full travel-time matrix. Solvers read these files and never
touch `MY.txt`. This means:

- Approach A and Approach B run on exactly the same test cases, so the comparison is fair.
- Each setting has 30 random versions, so we can report the mean and spread.
- The submission contains "the data files in the format used", as the brief asks.

**Name format:** `n8_s1_t1.0.json` means 8 orders, seed 1, tightness 1.0.

**Included set:** n = 5, 8, 10, 20, 50, 100 × tightness 0.5, 1.0, 1.5 × seeds 1–30 = **540 files,
about 14 MB**, in `data/processed/`. The deadline (order time + shelf life) is worked out
when scoring.

**Generated** with the shared Java tools in `src/com/delivery/tools/` (`GenerateInstance`): 540 files, about 14 MB. Randomness comes from `java.util.Random`, seeded from (n, seed); the tightness only scales the shelf lives, so the three tightness files for one (n, seed) share customers, order times and food. How to regenerate and run: `experiments/comparison/README.md`.

The template's worked example can't be regenerated (Java's random numbers differ from Python's), so it is rebuilt from the template's table and kept as `data/processed/example_n6_s10.json`. `CheckWorkedExample` confirms the Java scorer and brute force reproduce the template: best route 1→2→5→4→6→3 (average 29.0, longest 62.7), nearest neighbour cancels Kuala Lumpur Village (21.9). Travel times match to 0.1 min except Kampong Baharu (6.85 min, shown as 6.9 in the template).

---

## Shared implementation

All methods use `GenerateInstance.java` and `Evaluate.java`, with parameters in `Common.java`. `CheckWorkedExample` reconstructs the fixed six-customer example and confirms the scorer and exact solver against its published answers. No separate Python generator or scorer is required.

---

## Repo notes

- `Project_2627T1.pdf` (the assignment brief, marked SMU Restricted) is no longer tracked by Git and is listed in `.gitignore`. It is a reference, not a runtime input, so it stays out of the repository and the final source/data ZIP.
- Approach source and explanations live under `experiments/`; shared tooling is in `src/`, and comparison output is in `results/`.
- Minimising average wait is equivalent to minimising total arrival time only for the same delivered customer set. Different delivered subsets can have different sums of order times. The actual scorer correctly uses `arrival - order_time`.
