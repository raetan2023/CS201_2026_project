// Page logic: holds the state and draws it. All solving happens in solvers.js.

const PATHS = [
  { key: "nn", label: "Nearest neighbour" },
  { key: "b", label: "Approach B" },
  { key: "best", label: "Best possible (brute force)" },
];
const FOOD_ORDER = ["ice_cream", "noodles", "rice"];
const FOOD_NAME = { ice_cream: "ice cream", noodles: "noodles", rice: "rice" };
const PLAY_MS = 1700;

// Tree geometry, in SVG units. Each customer keeps one column, named in the header.
const COL = 116;
const PAD = 12;
const R = 25;
const HEADER_H = 62;
const ROOT_Y = HEADER_H + 34;
const FIRST_ROW_Y = ROOT_Y + 2 * R + 40;
const PITCH = 98;
const STEM = 18; // edges fan out this far below a node, clear of its dots

const el = (id) => document.getElementById(id);
const fmt = (x) => x.toFixed(1);
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;");

const state = {
  inst: null, places: 8, seed: 1, tight: 1,
  path: "b", step: 0, scenario: 0, timer: null, note: "",
  solved: null, rows: null, route: null,
};

const customer = (id) => state.inst.customers.find((c) => c.id === id);
const names = (ids) => ids.map((id) => customer(id).name).join(", ");

function loadOrders() {
  state.inst = loadInstance(DATA, state.places, state.seed, state.tight);
  state.note = "";
}

function solve() {
  const { inst } = state;
  const b = solveB(inst);
  state.solved = {
    b: { route: b.route, steps: b.steps },
    nn: { route: solveNN(inst) },
    best: { route: solveBest(inst) },
  };
  for (const p of PATHS) {
    state.solved[p.key].score = score(inst, state.solved[p.key].route);
  }
  state.counts = treeCounts(inst);
  selectPath(state.path);
}

function selectPath(key) {
  state.path = key;
  state.route = state.solved[key].route;
  state.rows = buildTree(state.inst, state.route);
  state.step = Math.min(state.step, state.route.length);
}

// ---- Tree ----

const colX = (id) => PAD + (state.inst.customers.findIndex((c) => c.id === id) + 0.5) * COL;
const rowY = (i) => FIRST_ROW_Y + i * PITCH;

function wrapName(name) {
  const lines = [""];
  for (const word of name.split(" ")) {
    const last = lines[lines.length - 1];
    if (last && (last + " " + word).length > 14) lines.push(word);
    else lines[lines.length - 1] = last ? last + " " + word : word;
  }
  return lines;
}

function edge(x1, y1, x2, y2, cls) {
  const mid = (y1 + y2) / 2;
  return `<path class="edge ${cls}" d="M${x1} ${y1}C${x1} ${mid} ${x2} ${mid} ${x2} ${y2}"/>`;
}

function dots(cell, x, y) {
  const all = [...cell.dies.map((id) => [id, true]), ...cell.survives.map((id) => [id, false])];
  const gap = 9.5;
  const start = x - ((all.length - 1) * gap) / 2;
  const title = cell.dies.length
    ? `Would die: ${names(cell.dies)}`
    : "Nobody dies if the rider goes here next";
  const marks = all
    .map(([, dies], i) => `<circle class="dot${dies ? " dies" : ""}" cx="${start + i * gap}" cy="${y}" r="3.6"/>`)
    .join("");
  return `<g><title>${esc(title)}</title>${marks}</g>`;
}

function nodeTitle(c, cell) {
  const base = `${c.name}, ${FOOD_NAME[c.food]}. Due by ${fmt(cell.due)}. `;
  if (!cell.alive) return `${base}Dead: can't arrive before ${fmt(cell.arrive)}.`;
  return `${base}Arrives ${fmt(cell.arrive)}, travel ${fmt(cell.travel)}, slack ${fmt(cell.due - cell.arrive)}.`;
}

function columnHeader(cell) {
  const c = customer(cell.id);
  const x = colX(cell.id);
  const lines = wrapName(c.name).slice(0, 2);
  const top = lines.length === 1 ? 29 : 14;
  const text = lines
    .map((line, i) => `<text class="label" x="${x}" y="${top + i * 15}">${esc(line)}</text>`)
    .join("");
  return `<g class="head"><title>${esc(`${c.id}. ${c.name}, ${FOOD_NAME[c.food]}`)}</title>${text}` +
    `<text class="label due" x="${x}" y="${top + lines.length * 15 + 1}">${c.id}: due ${fmt(cell.due)}</text></g>`;
}

function drawTree(animate) {
  const { rows, step, inst } = state;
  const width = inst.customers.length * COL + PAD * 2;
  const rootX = width / 2;
  const height = rowY(Math.max(rows.length - 1, 0)) + R + 46;
  const visible = Math.min(step + 1, rows.length);
  const edges = [];
  const nodes = [];
  const heads = rows.length ? rows[0].cells.map(columnHeader).join("") : "";

  for (let i = 0; i < visible; i++) {
    const row = rows[i];
    const picked = i < step ? row.chosen : null;
    const parentX = i === 0 ? rootX : colX(rows[i - 1].chosen);
    const parentY = (i === 0 ? ROOT_Y : rowY(i - 1)) + R;
    const y = rowY(i);
    edges.push(`<path class="edge taken" d="M${parentX} ${parentY}V${parentY + STEM}"/>`);

    for (const cell of row.cells) {
      const c = customer(cell.id);
      const x = colX(cell.id);
      const chosen = cell.id === picked;
      const dying = !cell.alive && animate && i === visible - 1;
      edges.push(edge(parentX, parentY + STEM, x, y - R, chosen ? "taken" : cell.alive ? "option" : "dead"));

      const cls = ["node", c.food, cell.alive ? "" : "dead", dying ? "dying" : "", chosen ? "chosen" : ""].join(" ");
      nodes.push(
        `<g class="${cls}" data-id="${c.id}"${i === 0 ? ' tabindex="0" role="button"' : ""}>` +
        `<title>${esc(nodeTitle(c, cell))}</title>` +
        `<circle cx="${x}" cy="${y}" r="${R}"/>` +
        `<text class="num" x="${x}" y="${y - 8}">${c.id}</text>` +
        `<text class="time" x="${x}" y="${y + 12}">${fmt(cell.arrive)}</text></g>`
      );
      if (cell.alive) {
        nodes.push(dots(cell, x, y + R + 9));
      } else {
        wrapName(c.name).forEach((line, k) => {
          nodes.push(`<text class="label dead-name" x="${x}" y="${y + R + 15 + k * 15}">${esc(line)}</text>`);
        });
      }
    }
  }

  const root =
    `<g class="rootnode"><circle cx="${rootX}" cy="${ROOT_Y}" r="${R}"/>` +
    `<text class="time" x="${rootX}" y="${ROOT_Y + 6}">R</text></g>` +
    `<text class="root-label" x="${rootX + R + 10}" y="${ROOT_Y + 5}">${esc(DATA.restaurant.name)}, t = 0</text>`;

  const svg = el("tree");
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.setAttribute("preserveAspectRatio", "xMidYMin meet");
  svg.style.minHeight = `${Math.round(height * 0.68)}px`;
  svg.innerHTML = edges.join("") + root + nodes.join("") +
    `<g id="heads"><rect width="${width}" height="${HEADER_H}" fill="#fff"/>${heads}` +
    `<line id="headRule" x1="0" x2="${width}" y1="${HEADER_H}" y2="${HEADER_H}" stroke="#000"/></g>`;

  const scroller = el("treeScroll");
  if (scroller.scrollHeight > scroller.clientHeight) {
    scroller.scrollTop = Math.max(0, (rowY(visible - 1) / height) * scroller.scrollHeight - scroller.clientHeight / 2);
  }
  pinHeader();
}

/** Keeps the customer names in view when a short screen has to scroll the tree. */
function pinHeader() {
  const heads = el("heads");
  const scale = el("tree").getScreenCTM()?.a;
  if (!heads || !scale) return;
  const top = el("treeScroll").scrollTop;
  heads.setAttribute("transform", `translate(0 ${top / scale})`);
  el("headRule").style.display = top > 0 ? "" : "none";
}

// ---- Map ----

function drawMap() {
  const { inst, step, route, rows } = state;
  const pts = [inst.restaurant, ...inst.customers];
  const cosLat = Math.cos((inst.restaurant.lat * Math.PI) / 180);
  const xs = pts.map((p) => p.lon * cosLat);
  const ys = pts.map((p) => p.lat);
  const [x0, x1, y0, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...ys), Math.max(...ys)];
  const W = 308, H = 220, M = 18;
  const k = Math.min((W - 2 * M) / (x1 - x0), (H - 2 * M) / (y1 - y0));
  const ox = (W - k * (x1 - x0)) / 2;
  const oy = (H - k * (y1 - y0)) / 2;
  const px = (p) => ox + k * (p.lon * cosLat - x0);
  const py = (p) => H - (oy + k * (p.lat - y0));

  const stops = [inst.restaurant, ...route.slice(0, step).map(customer)];
  const line = stops.map((p) => `${px(p).toFixed(1)},${py(p).toFixed(1)}`).join(" ");
  const done = new Set(route.slice(0, step));
  const gone = new Set();
  rows.slice(0, step + 1).forEach((r) => r.cells.forEach((c) => { if (!c.alive) gone.add(c.id); }));

  let out = `<polyline class="road" points="${line}"/>`;
  const rx = px(inst.restaurant), ry = py(inst.restaurant);
  out += `<rect x="${rx - 7}" y="${ry - 7}" width="14" height="14" fill="#fff" stroke="#000" stroke-width="2"/>`;
  out += `<text class="pin-num" x="${rx}" y="${ry + 4}">R</text>`;
  for (const c of inst.customers) {
    const x = px(c), y = py(c);
    const cls = gone.has(c.id) ? "gone" : done.has(c.id) ? "done" : "";
    out += `<g><title>${esc(c.name)}</title><circle class="pin ${c.food} ${cls}" cx="${x}" cy="${y}" r="9"/>`;
    out += `<text class="pin-num ${gone.has(c.id) ? "gone" : ""}" x="${x}" y="${y + 4}">${c.id}</text></g>`;
  }
  const last = stops[stops.length - 1];
  if (step > 0) out += `<circle class="rider" cx="${px(last)}" cy="${py(last)}" r="13"/>`;
  el("map").innerHTML = out;
}

// ---- Explanation ----

function whyB(stepInfo, cell) {
  const near = customer(stepInfo.nearest);
  switch (stepInfo.rule) {
    case 1: return "It's the only order still reachable.";
    case 2: {
      const lost = stepInfo.lost === 0 ? "no other order" : `${stepInfo.lost} other order${stepInfo.lost > 1 ? "s" : ""}`;
      const row = state.rows[state.step - 1];
      const nearCell = row.cells.find((c) => c.id === stepInfo.nearest);
      const bypass = stepInfo.nearest !== cell.id && nearCell.dies.length
        ? ` ${near.name} is nearer, but going there kills ${names(nearCell.dies)}.`
        : "";
      return `Fewest orders lost: this loses ${lost}.${bypass}`;
    }
    case 3: return "Tied on orders lost, so the shortest trip wins.";
    case 4: return "Tied on orders lost and travel, so the most urgent order wins.";
    default: return "Tied on everything, so the lowest id wins.";
  }
}

function blindSpot(i) {
  const { rows } = state;
  if (state.path !== "b" || i + 1 >= rows.length) return false;
  const cell = rows[i].cells.find((c) => c.id === rows[i].chosen);
  const nextAlive = rows[i + 1].cells.filter((c) => c.alive);
  return cell.dies.length === 0 && nextAlive.length > 1 && nextAlive.every((c) => c.dies.length > 0);
}

function drawExplanation() {
  const { step, rows, route, path, inst } = state;
  const n = inst.customers.length;
  const parts = [];
  let where;

  if (step === 0) {
    where = `Before the first stop, t = 0`;
    if (state.note) parts.push(state.note);
    parts.push(`The rider leaves ${DATA.restaurant.name} with ${n} orders. Press Next to see the first choice.`);
    const dead = rows[0] ? rows[0].cells.filter((c) => !c.alive) : [];
    for (const c of dead) parts.push(deathLine(c));
  } else {
    const row = rows[step - 1];
    const cell = row.cells.find((c) => c.id === row.chosen);
    where = `Stop ${step} of ${route.length}, t = ${fmt(cell.arrive)}`;
    parts.push(`Go to ${customer(cell.id).name}, arriving at ${fmt(cell.arrive)}.`);
    if (path === "b") parts.push(whyB(state.solved.b.steps[step - 1], cell));
    if (path === "nn") parts.push("Nearest neighbour takes the closest order still on its route.");
    if (path === "best") parts.push("Brute force tried every route, and this stop is on the best one.");
    if (blindSpot(step - 1)) {
      parts.push("It looked safe, but every next move now loses an order. A one-step lookahead can't see this.");
    }
    if (rows[step]) for (const c of rows[step].cells) if (!c.alive) parts.push(deathLine(c));
    if (step === route.length) parts.push(`Route finished: ${route.length} of ${n} delivered.`);
  }
  el("where").textContent = where;
  el("why").innerHTML = parts.map((p) => {
    const death = p.startsWith("†");
    return `<p${death ? ' class="death"' : ""}>${esc(death ? p.slice(1) : p)}</p>`;
  }).join("");
}

function deathLine(c) {
  return `†${customer(c.id).name} dies: the rider can't arrive before ${fmt(c.arrive)}, and it's due by ${fmt(c.due)}.`;
}

// ---- Results and controls ----

function drawResults() {
  const n = state.inst.customers.length;
  el("results").innerHTML = PATHS.map((p) => {
    const s = state.solved[p.key].score;
    return `<tr data-path="${p.key}" class="${p.key === state.path ? "on" : ""}">` +
      `<td><button type="button" aria-pressed="${p.key === state.path}">${p.label}</button></td>` +
      `<td>${s.delivered}/${n}</td><td>${s.cancelled}</td>` +
      `<td>${fmt(s.avgWait)}</td><td>${fmt(s.maxWait)}</td></tr>`;
  }).join("");
  const { classic, alive, dead } = state.counts;
  const shown = state.rows.reduce((sum, r) => sum + r.cells.length, 0);
  el("size").textContent =
    `Full tree: ${classic.toLocaleString("en")} nodes, ${(alive + dead).toLocaleString("en")} with deadlines. Shown: ${shown}.`;
}

function drawControls() {
  el("scenario").value = String(state.scenario);
  el("places").value = String(state.places);
  el("seed").value = String(state.seed);
  el("tight").value = String(state.tight);
  el("back").disabled = state.step === 0;
  el("next").disabled = state.step >= state.route.length;
  el("play").disabled = !state.timer && state.step >= state.route.length;
  el("play").textContent = state.timer ? "Pause" : "Play";
}

function draw(animate = false) {
  drawTree(animate);
  drawMap();
  drawExplanation();
  drawResults();
  drawControls();
}

function stop() {
  clearInterval(state.timer);
  state.timer = null;
}

function move(delta) {
  const next = state.step + delta;
  if (next < 0 || next > state.route.length) return false;
  state.step = next;
  draw(delta > 0);
  return true;
}

function restart() {
  stop();
  state.step = 0;
  solve();
  draw();
}

function loadScenario(i) {
  const s = SCENARIOS[i];
  Object.assign(state, { scenario: i, places: s.places, seed: s.seed, tight: s.tight });
  loadOrders();
  restart();
}

function matchScenario() {
  const i = SCENARIOS.findIndex(
    (s) => s.places === state.places && s.seed === state.seed && s.tight === state.tight
  );
  state.scenario = i >= 0 ? i : "custom";
}

function ordersChanged() {
  matchScenario();
  loadOrders();
  restart();
}

function changeFood(id) {
  const c = customer(id);
  c.food = FOOD_ORDER[(FOOD_ORDER.indexOf(c.food) + 1) % FOOD_ORDER.length];
  c.shelf_life = FOOD_SHELF[c.food] * state.tight;
  state.scenario = "custom";
  state.note = `${c.name} now ordered ${FOOD_NAME[c.food]}, so every route was solved again.`;
  restart();
}

function togglePlay() {
  if (state.timer) {
    stop();
    drawControls();
    return;
  }
  if (state.step >= state.route.length) return;
  state.timer = setInterval(() => {
    if (!move(1) || state.step >= state.route.length) {
      stop();
      drawControls();
    }
  }, PLAY_MS);
  drawControls();
}

const ORDER_SETS = 30; // "Set k" is data/processed/n{orders}_s{k}_t{tightness}.json

function setup() {
  const options = SCENARIOS.map((s, i) => `<option value="${i}">${esc(s.name)}</option>`);
  el("scenario").innerHTML = options.join("") + '<option value="custom">Custom</option>';
  el("seed").innerHTML = Array.from({ length: ORDER_SETS }, (_, i) =>
    `<option value="${i + 1}">Set ${i + 1}</option>`).join("");

  el("scenario").addEventListener("change", (e) => {
    if (e.target.value !== "custom") loadScenario(Number(e.target.value));
  });
  el("places").addEventListener("change", (e) => { state.places = Number(e.target.value); ordersChanged(); });
  el("tight").addEventListener("change", (e) => { state.tight = Number(e.target.value); ordersChanged(); });
  el("seed").addEventListener("change", (e) => { state.seed = Number(e.target.value); ordersChanged(); });
  el("reset").addEventListener("click", ordersChanged);
  el("back").addEventListener("click", () => { stop(); move(-1); });
  el("next").addEventListener("click", () => { stop(); move(1); });
  el("play").addEventListener("click", togglePlay);

  el("results").addEventListener("click", (e) => {
    const tr = e.target.closest("tr");
    if (!tr) return;
    stop();
    state.step = 0;
    selectPath(tr.dataset.path);
    draw();
  });
  el("treeScroll").addEventListener("scroll", pinHeader);
  window.addEventListener("resize", pinHeader);
  el("tree").addEventListener("click", (e) => {
    const g = e.target.closest(".node[data-id]");
    if (g) changeFood(Number(g.dataset.id));
  });
  el("tree").addEventListener("keydown", (e) => {
    const g = e.target.closest(".node[data-id]");
    if (g && (e.key === "Enter" || e.key === " ")) {
      e.preventDefault();
      changeFood(Number(g.dataset.id));
    }
  });
  document.addEventListener("keydown", (e) => {
    if (e.target.closest("input, select, button, .node")) return;
    if (e.key === "ArrowRight") { stop(); move(1); }
    if (e.key === "ArrowLeft") { stop(); move(-1); }
    if (e.key === " ") { e.preventDefault(); togglePlay(); }
  });

  // A link can open a set view, for example demo.html#scenario=1&step=3&path=best
  const hash = new URLSearchParams(location.hash.slice(1));
  loadScenario(Number(hash.get("scenario")) || 0);
  if (hash.has("path")) selectPath(hash.get("path"));
  if (hash.has("step")) state.step = Math.min(Number(hash.get("step")), state.route.length);
  draw();
}

setup();
