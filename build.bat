@echo off
echo [PassiveMobs] Building...
gradle clean build
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================================
    echo BUILD OK: build\libs\passivemobs-1.0.0.jar
    echo Copy to mods/ folder and launch.
    echo ====================================================
) else (
    echo BUILD FAILED. Check Java version and Gradle output.
)
