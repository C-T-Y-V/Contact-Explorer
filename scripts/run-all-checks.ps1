<#
==============================================================================
Script: run-all-checks.ps1
Purpose: One-shot local CI pre-push verification runner for developers.
Product: Contact Explorer Android — A CTYV Product
Attribution: Priyanshu Sharma (CTYV)
==============================================================================
#>

[CmdletBinding()]
param(
    [switch]$SkipRelease = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=== Contact Explorer: Pre-Push Local CI Quality Gates ===" -ForegroundColor Cyan
Write-Host "Attribution: Priyanshu Sharma (CTYV) — A CTYV Product" -ForegroundColor Cyan
Write-Host ""

# Gate 1: Android Lint Static Analysis
Write-Host "[Gate 1/4] Running Static Analysis & Android Lint..." -ForegroundColor Yellow
cmd.exe /c "gradlew.bat lintDebug --stacktrace"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] Android lint checks failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "[PASS] Static analysis completed cleanly." -ForegroundColor Green
Write-Host ""

# Gate 2: Unit Test Suite
Write-Host "[Gate 2/4] Executing Comprehensive Unit Test Suites..." -ForegroundColor Yellow
cmd.exe /c "gradlew.bat testDebugUnitTest --stacktrace"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] Unit test suites failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "[PASS] All unit test suites passed (100% green)." -ForegroundColor Green
Write-Host ""

if (-not $SkipRelease) {
    # Gate 3: Release Build Compilation
    Write-Host "[Gate 3/4] Compiling Release APK with R8 Optimization..." -ForegroundColor Yellow
    cmd.exe /c "gradlew.bat assembleRelease --stacktrace"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] Release compilation failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "[PASS] Release APK assembled successfully." -ForegroundColor Green
    Write-Host ""

    # Gate 4: Size Budget Assertion
    Write-Host "[Gate 4/4] Validating Release APK Size Against 5.0 MB Target..." -ForegroundColor Yellow
    & ".\scripts\verify-apk-size.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] Size budget assertion failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "[PASS] Performance size budget verified." -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[INFO] Gates 3 and 4 skipped via -SkipRelease switch." -ForegroundColor Cyan
}

Write-Host "=== ALL QUALITY GATES PASSED! READY FOR GIT PUSH ===" -ForegroundColor Green
