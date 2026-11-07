# Approach A: Deadline order, then improve by moving stops

Algorithm: `SolverA.java`. Tests: `DemoA.java`.
See [HOW_IT_WORKS.md](HOW_IT_WORKS.md) for the design, complexity and worked examples.

Requires JDK 17+. Run these commands from the **project root**:

```text
javac -encoding UTF-8 -d out/test-A src/com/delivery/routing/*.java experiments/approach_A/src/com/delivery/routing/*.java
java -cp out/test-A com.delivery.routing.DemoA
```

Expected: `ALL CHECKS PASSED`. Both approaches use the same model classes in
`src/com/delivery/routing/` at the project root. Each approach folder contains
only its own solver and demo; the solvers do not depend on each other.

For the full dataset comparison, follow the [main README](../../README.md).
