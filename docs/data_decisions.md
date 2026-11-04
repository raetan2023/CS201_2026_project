# Data decisions

How we turn `data/raw/MY.txt` into test data, and why. Hanlin's template
(`docs/Deadline-Aware Food Delivery Problem Template.docx`) is the final set of rules. If this
file and the template disagree, the template wins.

**Two items are still open: see [Open items](#open-items).**

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
| 6 | Keep straight-line travel times? | Yes | Set by the template. They also obey the triangle inequality, which Approach A's "definitely lost" check relies on |
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

**Planned set:** n = 5, 8, 10, 20, 50, 100 × tightness 0.5, 1.0, 1.5 × seeds 1–30 = **540 files,
about 16.5 MB (estimated)**, in `data/processed/`. V1 and V2 share the same files, because the
deadline is worked out when scoring.

**Not generated yet.** This is waiting on open item 1.

---

## Open items

1. **Confirm with Hanlin that generating and scoring can be in Java.** We recall Hanlin saying
   our tests can be in Java, but the template still says every method must use
   `generate_instance.py` and `evaluate.py`, which are Python. Those files are not in this repo.
   The brief requires "ALL source codes required to reproduce the experiments", so the
   submission needs a generator and a scorer in some language. If Java:
   - Everyone must use one shared Java generator and scorer. Separate copies would break the
     template's "same data, same scoring" rule.
   - Java's random numbers differ from Python's, so the same seed gives different test cases from
     the template's. The template's worked example won't regenerate, and needs keeping as a
     fixed file.
   - Check the Java scorer against the template's worked example: for n = 6, seed 10, the best
     V1 route has average wait 29.0 and the best V2 route 29.8.
2. **Pick a teammate to check this file** before it is used in the presentation.

---

## Repo notes

- `docs/Project_2627T1.pdf` is marked "SMU Classification: Restricted", so it is in `.gitignore`
  and is not pushed. Get the brief from eLearn.
- Results and code for each approach live under `experiments/`, as the brief requires.
