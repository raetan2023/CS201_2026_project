@echo off
setlocal
if not "%~1"=="" if /i not "%~1"=="--all" goto :usage
if not "%~2"=="" goto :usage
pushd "%~dp0"
if errorlevel 1 exit /b 1

echo [1/5] Compiling source and tests (JDK 17+ required)...
javac -encoding UTF-8 -d out/tests src/com/delivery/routing/*.java experiments/approach_A/src/com/delivery/routing/*.java experiments/approach_B/src/com/delivery/routing/*.java src/com/delivery/tools/*.java
if errorlevel 1 goto :failed

echo [2/5] Testing Approach A: plan-and-fix...
java -cp out/tests com.delivery.routing.DemoA
if errorlevel 1 goto :failed

echo [3/5] Testing Approach B: greedy...
java -cp out/tests com.delivery.routing.DemoB
if errorlevel 1 goto :failed

echo [4/5] Checking the scorer and exact solver against the worked example...
java -cp out/tests com.delivery.tools.CheckWorkedExample data/raw/MY.txt out/tests/fixtures
if errorlevel 1 goto :failed
echo [5/5] Checking the five-customer presentation example...
java -cp out/tests com.delivery.tools.CheckPresentationExample
if errorlevel 1 goto :failed
echo.
echo ALL TEST SUITES PASSED.

if /i "%~1"=="--all" (
    echo Running the complete 540-input experiment pipeline...
    call experiments\comparison\run_all.bat
    if errorlevel 1 goto :failed
)
popd
exit /b 0

:failed
echo.
echo FAILED - see the error above.
popd
exit /b 1

:usage
echo Usage: run_tests.bat [--all]
echo   No arguments: compile and run both demos, worked-example and presentation checks.
echo   --all: also generate all 540 inputs, run experiments and draw charts.
exit /b 2
