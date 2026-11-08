#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 1 || ( $# -eq 1 && "$1" != "--all" ) ]]; then
    echo "Usage: bash run_tests.sh [--all]" >&2
    echo "  No arguments: compile and run both demos, worked-example and presentation checks." >&2
    echo "  --all: also generate all 540 inputs, run experiments and draw charts." >&2
    exit 2
fi

cd "$(dirname "$0")"
trap 'echo "FAILED - see the error above." >&2' ERR

echo "[1/5] Compiling source and tests (JDK 17+ required)..."
javac -encoding UTF-8 -d out/tests src/com/delivery/routing/*.java experiments/approach_A/src/com/delivery/routing/*.java experiments/approach_B/src/com/delivery/routing/*.java src/com/delivery/tools/*.java

echo "[2/5] Testing Approach A: plan-and-fix..."
java -cp out/tests com.delivery.routing.DemoA

echo "[3/5] Testing Approach B: greedy..."
java -cp out/tests com.delivery.routing.DemoB

echo "[4/5] Checking the scorer and exact solver against the worked example..."
java -cp out/tests com.delivery.tools.CheckWorkedExample data/raw/MY.txt out/tests/fixtures
echo "[5/5] Checking the five-customer presentation example..."
java -cp out/tests com.delivery.tools.CheckPresentationExample
echo
echo "ALL TEST SUITES PASSED."

if [[ "${1:-}" == "--all" ]]; then
    echo "Running the complete 540-input experiment pipeline..."
    bash experiments/comparison/run_all.sh
fi
