// Solver layer for the demo page. No drawing code lives here.
//
// Each function is a port of the project's Java, and verify/verify.py checks it against
// the Java and against results/raw_results.csv:
//   solveB    experiments/approach_B/.../SolverB.java  (greedy with one-step lookahead)
//   solveNN   src/com/delivery/tools/NearestNeighbor.java
//   solveBest the same goal as src/com/delivery/tools/BruteForce.java
//   score     src/com/delivery/tools/Evaluate.java
//
// An instance has the shape of the files in data/processed: customers with ids 1..n,
// and travel_min with index 0 = restaurant, index k = customer k.
// The deadline is order_time + shelf_life.

const EPS = 1e-9;

function deadlines(inst) {
  const d = {};
  for (const c of inst.customers) d[c.id] = c.order_time + c.shelf_life;
  return d;
}

/** Lexicographic comparison from SolverB.isBetter: lost, travel, slack, id. */
function isBetter(a, b) {
  if (b === null) return true;
  if (a.lost !== b.lost) return a.lost < b.lost;
  if (Math.abs(a.travel - b.travel) > EPS) return a.travel < b.travel;
  if (Math.abs(a.slack - b.slack) > EPS) return a.slack < b.slack;
  return a.id < b.id;
}

/** Which rule separated the winner from one rival: 2 lost, 3 travel, 4 slack, 5 id. */
function ruleBetween(win, other) {
  if (win.lost !== other.lost) return 2;
  if (Math.abs(win.travel - other.travel) > EPS) return 3;
  if (Math.abs(win.slack - other.slack) > EPS) return 4;
  return 5;
}

/** Approach B. Returns the route plus, for each stop, why that stop won. */
function solveB(inst) {
  const T = inst.travel_min;
  const dl = deadlines(inst);
  const ids = inst.customers.map((c) => c.id);
  const visited = new Set();
  const route = [];
  const steps = [];
  let cur = 0;
  let t = 0;

  while (true) {
    const alive = ids.filter((v) => !visited.has(v) && t + T[cur][v] <= dl[v] + EPS);
    const scored = [];
    let best = null;
    for (const u of alive) {
      const travel = T[cur][u];
      const arrival = t + travel;
      const slack = dl[u] - arrival;
      const lost = alive.filter(
        (v) => v !== u && arrival + T[u][v] > dl[v] + EPS
      ).length;
      const cand = { id: u, lost, travel, slack };
      scored.push(cand);
      if (isBetter(cand, best)) best = cand;
    }
    if (best === null) break;

    const rivals = scored.filter((c) => c.id !== best.id);
    const rule = rivals.length ? Math.max(...rivals.map((c) => ruleBetween(best, c))) : 1;
    const nearest = scored.reduce((a, b) => (b.travel < a.travel ? b : a));
    steps.push({ chosen: best.id, rule, lost: best.lost, nearest: nearest.id, scored });

    t += best.travel;
    visited.add(best.id);
    route.push(best.id);
    cur = best.id;
  }
  return { route, steps };
}

/**
 * Baseline: the classic nearest-neighbour tour (deadlines ignored), then drive it and
 * skip every order that would arrive late. A skipped order costs no time.
 */
function solveNN(inst) {
  const T = inst.travel_min;
  const dl = deadlines(inst);
  const left = new Set(inst.customers.map((c) => c.id));
  const tour = [];
  let cur = 0;
  while (left.size) {
    let k = null;
    for (const j of left) if (k === null || T[cur][j] < T[cur][k]) k = j;
    tour.push(k);
    left.delete(k);
    cur = k;
  }
  const route = [];
  let t = 0;
  cur = 0;
  for (const k of tour) {
    const a = t + T[cur][k];
    if (a <= dl[k] + EPS) {
      route.push(k);
      t = a;
      cur = k;
    }
  }
  return route;
}

/** Exact best route: most orders delivered, then lowest total wait. */
function solveBest(inst) {
  const T = inst.travel_min;
  const dl = deadlines(inst);
  const ot = {};
  for (const c of inst.customers) ot[c.id] = c.order_time;
  const left = new Set(inst.customers.map((c) => c.id));
  let bestCount = 0;
  let bestTotal = Infinity;
  let bestRoute = [];
  const route = [];

  function dfs(cur, t, total) {
    const cnt = route.length;
    if (cnt > bestCount || (cnt === bestCount && cnt && total < bestTotal)) {
      bestCount = cnt;
      bestTotal = total;
      bestRoute = route.slice();
    }
    if (cnt + left.size < bestCount) return;
    for (const k of Array.from(left)) {
      const a = t + T[cur][k];
      if (a <= dl[k] + EPS) {
        left.delete(k);
        route.push(k);
        dfs(k, a, total + a - ot[k]);
        route.pop();
        left.add(k);
      }
    }
  }
  dfs(0, 0, 0);
  return bestRoute;
}

/** Port of Evaluate.evaluate(). Waits count only for on-time stops. */
function score(inst, route) {
  const T = inst.travel_min;
  const byId = {};
  for (const c of inst.customers) byId[c.id] = c;
  let t = 0;
  let cur = 0;
  const stops = [];
  for (const k of route) {
    t += T[cur][k];
    const c = byId[k];
    stops.push({ id: k, arrive: t, wait: t - c.order_time, late: t > c.order_time + c.shelf_life + EPS });
    cur = k;
  }
  const waits = stops.filter((s) => !s.late).map((s) => s.wait);
  return {
    stops,
    delivered: waits.length,
    cancelled: inst.customers.length - route.length,
    late: stops.filter((s) => s.late).length,
    avgWait: waits.length ? waits.reduce((a, b) => a + b, 0) / waits.length : 0,
    maxWait: waits.length ? Math.max(...waits) : 0,
  };
}

/**
 * Lays a route out as tree rows. Row i holds every option the rider has before
 * stop i + 1. A cell is dead if it can't be reached by its deadline from the
 * rider's position. A dead customer is shown once, then leaves the tree.
 */
function buildTree(inst, route) {
  const T = inst.travel_min;
  const dl = deadlines(inst);
  const ids = inst.customers.map((c) => c.id);
  const visited = new Set();
  const gone = new Set();
  const rows = [];
  let cur = 0;
  let t = 0;

  for (let i = 0; i <= route.length; i++) {
    const left = ids.filter((v) => !visited.has(v) && !gone.has(v));
    if (!left.length) break;
    const alive = left.filter((v) => t + T[cur][v] <= dl[v] + EPS);
    const cells = left.map((id) => {
      const arrive = t + T[cur][id];
      const isAlive = alive.includes(id);
      const others = isAlive ? alive.filter((v) => v !== id) : [];
      return {
        id,
        arrive,
        due: dl[id],
        travel: T[cur][id],
        alive: isAlive,
        dies: others.filter((v) => arrive + T[id][v] > dl[v] + EPS),
        survives: others.filter((v) => arrive + T[id][v] <= dl[v] + EPS),
      };
    });
    const next = i < route.length ? route[i] : null;
    rows.push({ from: cur, t, cells, chosen: next });
    for (const c of cells) if (!c.alive) gone.add(c.id);
    if (next === null) break;
    t += T[cur][next];
    cur = next;
    visited.add(next);
  }
  return rows;
}

/** Size of the whole search tree, with and without deadlines (restaurant not counted). */
function treeCounts(inst) {
  const n = inst.customers.length;
  let classic = 0;
  let level = 1;
  for (let k = 0; k < n; k++) {
    level *= n - k;
    classic += level;
  }
  const T = inst.travel_min;
  const dl = deadlines(inst);
  const ids = inst.customers.map((c) => c.id);
  const seen = new Set();
  let alive = 0;
  let dead = 0;
  (function walk(cur, t) {
    for (const k of ids) {
      if (seen.has(k)) continue;
      const a = t + T[cur][k];
      if (a <= dl[k] + EPS) {
        alive++;
        seen.add(k);
        walk(k, a);
        seen.delete(k);
      } else {
        dead++;
      }
    }
  })(0, 0);
  return { classic, alive, dead };
}

// ---- Order sets ----
// The page carries the project's own inputs, data/processed/n{orders}_s{set}_t1.0.json.
// The three tightness files for one set differ only in shelf life (base x tightness),
// and build.py checks that, so only the 1.0 file is stored.

const FOOD_SHELF = { ice_cream: 30, noodles: 60, rice: 90 };

/** A fresh, editable copy of one input file: n orders, order set (seed), tightness. */
function loadInstance(data, n, seed, tight) {
  const base = data.sets[`n${n}_s${seed}`];
  return {
    restaurant: data.restaurant,
    customers: base.customers.map((c, i) => ({
      id: i + 1,
      name: c.name,
      lat: c.lat,
      lon: c.lon,
      order_time: c.order_time,
      food: c.food,
      shelf_life: FOOD_SHELF[c.food] * tight,
    })),
    travel_min: base.travel_min,
  };
}

if (typeof module !== "undefined") {
  module.exports = {
    solveB, solveNN, solveBest, score, buildTree, treeCounts, deadlines, loadInstance, FOOD_SHELF,
  };
}
