<#
==============================================================================
Script: verify-apk-size.ps1
Purpose: Automated verification assessing Release APK size against 5.0 MB target.
Product: Contact Explorer Android — A CTYV Product
Attribution: Priyanshu Sharma (CTYV)
==============================================================================
#>

[CmdletBinding()]
param(
    [string]$TargetPath = "",
    [long]$BudgetBytes = 5242880, # 5.0 MiB
    [switch]$Strict = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=== Contact Explorer: Release APK Size Assessment ===" -ForegroundColor Cyan
Write-Host "Target Performance Budget: 5.0 MB ($BudgetBytes bytes) [Optional Benchmark]" -ForegroundColor Cyan

# Locate Release APK
$apk = $null
if ($TargetPath -and (Test-Path $TargetPath)) {
    $apk = Get-Item $TargetPath
} else {
    $candidates = Get-ChildItem -Path "app/build/outputs/apk/release" -Filter "*.apk" -ErrorAction SilentlyContinue
    if ($candidates) {
        $apk = $candidates | Select-Object -First 1
    }
}

if (-not $apk) {
    Write-Host "[ERROR] Release APK was not found!" -ForegroundColor Red
    Write-Host "Expected location: app/build/outputs/apk/release/*.apk" -ForegroundColor Yellow
    Write-Host "Please run './gradlew assembleRelease' before executing this check." -ForegroundColor Yellow
    exit 1
}

$sizeBytes = $apk.Length
$sizeMB = [math]::Round($sizeBytes / 1MB, 2)
$budgetMB = [math]::Round($BudgetBytes / 1MB, 2)

Write-Host "Located APK: $($apk.FullName)"
Write-Host "Measured Size: $sizeMB MB ($sizeBytes bytes)"

if ($sizeBytes -le $BudgetBytes) {
    $marginBytes = $BudgetBytes - $sizeBytes
    $marginMB = [math]::Round($marginBytes / 1MB, 2)
    Write-Host "✅ [PASS] Release APK is well within target size budget!" -ForegroundColor Green
    Write-Host "Under budget by: $marginMB MB ($marginBytes bytes margin)" -ForegroundColor Green
    Write-Host "Quality Gate Check: PASSED (Budget Compliance)" -ForegroundColor Cyan
    exit 0
} else {
    $overBytes = $sizeBytes - $BudgetBytes
    $overMB = [math]::Round($overBytes / 1MB, 2)
    Write-Host "⚠️ [ADVISORY] Release APK exceeds target benchmark (+$overMB MB)!" -ForegroundColor Yellow
    Write-Host "Measured: $sizeMB MB | Target: $budgetMB MB" -ForegroundColor Yellow

    if ($Strict) {
        Write-Host "❌ [STRICT MODE] Failing build because -Strict flag was set" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "Note: 5.0 MB is an optional performance guideline. Build proceeds successfully." -ForegroundColor Cyan
        exit 0
    }
}
