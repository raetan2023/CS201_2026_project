// Runs the demo's JavaScript on the embedded order sets and on any fixed cases, and
// prints the results as JSON for verify.py to compare with the Java and the results CSV.
// Usage: node run_js.js input.json output.json
//   input = {data, cases: [{key, n, seed, tight}], fixed: [{key, inst}]}
const fs = require("fs");
const path = require("path");
const S = require(path.join(__dirname, "..", "src", "solvers.js"));

function pathProblems(inst, route) {
  const rows = S.buildTree(inst, route);
  const dead = rows.flatMap((r) => r.cells.filter((c) => !c.alive).map((c) => c.id));
  const problems = [];
  if (route.some((id) => dead.includes(id))) problems.push("delivered a dead node");
  const cancelled = inst.customers.map((c) => c.id).filter((id) => !route.includes(id));
  const same = dead.length === cancelled.length && cancelled.every((id) => dead.includes(id));
  if (!same) problems.push(`dead [${dead}] but cancelled [${cancelled}]`);
  return problems;
}

function run(key, inst) {
  const b = S.solveB(inst);
  const nn = S.solveNN(inst);
  const best = S.solveBest(inst);

  // The tree's black dots must equal the solver's own lost count at every stop.
  const rows = S.buildTree(inst, b.route);
  let dotMismatches = 0;
  b.steps.forEach((step, i) => {
    for (const cand of step.scored) {
      const cell = rows[i].cells.find((c) => c.id === cand.id);
      if (!cell || !cell.alive || cell.dies.length !== cand.lost) dotMismatches++;
    }
  });
  const summary = (route) => {
    const s = S.score(inst, route);
    return { route, delivered: s.delivered, late: s.late, avg: s.avgWait, max: s.maxWait };
  };
  return {
    key,
    b: summary(b.route), nn: summary(nn), best: summary(best),
    dotMismatches,
    pathProblems: {
      b: pathProblems(inst, b.route),
      nn: pathProblems(inst, nn),
      best: pathProblems(inst, best),
    },
  };
}

const input = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
const out = [];
for (const { key, inst } of input.fixed) out.push(run(key, inst));
for (const { key, n, seed, tight } of input.cases) {
  out.push(run(key, S.loadInstance(input.data, n, seed, tight)));
}
fs.writeFileSync(process.argv[3], JSON.stringify(out));
