#!/usr/bin/env bash
# Reproduces every result: compile, check against the template, generate instances, run, plot.
# Run from anywhere:  bash experiments/comparison/run_all.sh
set -e
cd "$(dirname "$0")/../.."

echo "[1/5] Compiling (Java 17+)..."
rm -rf out
javac -encoding UTF-8 -d out src/com/delivery/routing/*.java experiments/approach_A/src/com/delivery/routing/*.java experiments/approach_B/src/com/delivery/routing/*.java src/com/delivery/tools/*.java

echo "[2/5] Checking the shared tools against the template's worked example..."
java -cp out com.delivery.tools.CheckWorkedExample data/raw/MY.txt

echo "[3/5] Generating the 540 instance files..."
java -cp out com.delivery.tools.GenerateInstance data/raw/MY.txt --all

echo "[4/5] Running NN, Approach A, Approach B and brute force (about 2-5 minutes)..."
java -cp out com.delivery.tools.RunExperiments

echo "[5/5] Drawing charts (needs Python with matplotlib)..."
python3 experiments/comparison/plot_results.py
echo "Done. See results/summary.csv and results/fig_*.png"
