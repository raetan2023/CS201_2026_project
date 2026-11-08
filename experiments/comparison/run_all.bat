@echo off
rem Reproduces every result: compile, check against the template, generate instances, run, plot.
rem Run from anywhere:  experiments\comparison\run_all.bat
cd /d "%~dp0\..\.."

echo [1/5] Compiling (Java 17+)...
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out src/com/delivery/routing/*.java experiments/approach_A/src/com/delivery/routing/*.java experiments/approach_B/src/com/delivery/routing/*.java src/com/delivery/tools/*.java
if errorlevel 1 goto :err

echo [2/5] Checking the shared tools against the template's worked example...
java -cp out com.delivery.tools.CheckWorkedExample data\raw\MY.txt
if errorlevel 1 goto :err

echo [3/5] Generating the 540 instance files...
java -cp out com.delivery.tools.GenerateInstance data\raw\MY.txt --all
if errorlevel 1 goto :err

echo [4/5] Running NN, Approach A, Approach B and brute force (about 2-5 minutes)...
java -cp out com.delivery.tools.RunExperiments
if errorlevel 1 goto :err

echo [5/5] Drawing charts (needs Python with matplotlib)...
python experiments\comparison\plot_results.py
if errorlevel 1 goto :err
echo Done. See results\summary.csv and results\fig_*.png
exit /b 0

:err
echo FAILED - see the message above.
exit /b 1
