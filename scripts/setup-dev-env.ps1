<#
==============================================================================
Script: setup-dev-env.ps1
Purpose: Professional DevOps developer workstation onboarding & validation.
Product: Contact Explorer Android — A CTYV Product
Attribution: Priyanshu Sharma (CTYV)
==============================================================================
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "=== Contact Explorer: Developer Workstation Environment Setup ===" -ForegroundColor Cyan
Write-Host "Attribution: Priyanshu Sharma (CTYV) — A CTYV Product" -ForegroundColor Cyan
Write-Host ""

# 1. Check Java JDK
Write-Host "[1/4] Checking Java JDK Installation..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1 | Out-String
    if ($javaOutput -match 'version "(\d+)') {
        $javaVer = [int]$matches[1]
        if ($javaVer -lt 17) {
            Write-Host "[FAIL] Java version $javaVer detected. Contact Explorer requires JDK 17 or higher." -ForegroundColor Red
            exit 1
        } else {
            Write-Host "[OK] Compatible Java version detected (JDK $javaVer)." -ForegroundColor Green
        }
    }
} catch {
    Write-Host "[FAIL] Java is not installed or not in PATH!" -ForegroundColor Red
    Write-Host "Please install OpenJDK 17 or 21 (Temurin / Microsoft OpenJDK recommended)." -ForegroundColor Yellow
    exit 1
}

# 2. Check Android SDK
Write-Host "[2/4] Validating Android SDK Environment..." -ForegroundColor Yellow
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) {
    $defaultWinSdk = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $defaultWinSdk) {
        $sdkPath = $defaultWinSdk
        Write-Host "[OK] Resolved standard Windows Android SDK at $sdkPath" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Neither ANDROID_HOME nor local SDK directory was automatically detected." -ForegroundColor Yellow
        Write-Host "Ensure local.properties contains 'sdk.dir=<path_to_sdk>'." -ForegroundColor Yellow
    }
} else {
    Write-Host "[OK] Android SDK located: $sdkPath" -ForegroundColor Green
}

# 3. Check Git & Configure Git Hooks
Write-Host "[3/4] Validating Git Configuration & Initializing Hooks..." -ForegroundColor Yellow
try {
    $gitVer = git --version
    Write-Host "[OK] Git is available: $gitVer" -ForegroundColor Green
    git config core.hooksPath .githooks
    Write-Host "[OK] Configured git config core.hooksPath .githooks" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Git command failed. Verify Git for Windows is installed." -ForegroundColor Red
    exit 1
}

# 4. Check Gradle Wrapper
Write-Host "[4/4] Verifying Gradle Wrapper..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    Write-Host "[OK] gradlew.bat located successfully." -ForegroundColor Green
} else {
    Write-Host "[FAIL] gradlew.bat not found in working directory." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Developer Workstation Setup Complete! ===" -ForegroundColor Green
Write-Host "To verify local build health, execute: .\scripts\run-all-checks.ps1" -ForegroundColor Cyan
