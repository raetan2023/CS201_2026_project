@echo off
rem Reproduces every result: compile, check against the template, generate instances, run, plot.
rem Run from anywhere:  experiments\comparison\run_all.bat
cd /d "%~dp0\..\.."

echo [1/5] Compiling (Java 17+)...
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out ^
  experiments\approach_A\src\com\delivery\routing\Customer.java ^
  experiments\approach_A\src\com\delivery\routing\DeliveryInstance.java ^
  experiments\approach_A\src\com\delivery\routing\RouteResult.java ^
  experiments\approach_A\src\com\delivery\routing\SolverA.java ^
  experiments\approach_B\src\com\delivery\routing\SolverB.java ^
  src\com\delivery\tools\Common.java src\com\delivery\tools\Places.java src\com\delivery\tools\Instance.java ^
  src\com\delivery\tools\GenerateInstance.java src\com\delivery\tools\Evaluate.java src\com\delivery\tools\BruteForce.java ^
  src\com\delivery\tools\NearestNeighbor.java src\com\delivery\tools\CheckWorkedExample.java src\com\delivery\tools\RunExperiments.java
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

echo [5/5] Drawing charts (needs Python with matplotlib; skipped if missing)...
python experiments\comparison\plot_results.py
echo Done. See results\summary.csv and results\fig_*.png
goto :eof

:err
echo FAILED - see the message above.
exit /b 1
