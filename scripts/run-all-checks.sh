#!/usr/bin/env bash
# ==============================================================================
# Script: run-all-checks.sh
# Purpose: One-shot local CI pre-push verification runner for developers.
# Product: Contact Explorer Android — A CTYV Product
# Attribution: Priyanshu Sharma (CTYV)
# ==============================================================================

set -euo pipefail

CYAN='\033[0;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
RESET='\033[0m'

echo -e "${CYAN}${BOLD}=== Contact Explorer: Pre-Push Local CI Quality Gates ===${RESET}"
echo -e "${CYAN}Attribution:${RESET} Priyanshu Sharma (CTYV) — A CTYV Product"
echo ""

# Gate 1: Android Lint Static Analysis
echo -e "${BOLD}[Gate 1/4] Running Static Analysis & Android Lint...${RESET}"
./gradlew lintDebug --stacktrace
echo -e "${GREEN}[PASS] Static analysis completed cleanly.${RESET}"
echo ""

# Gate 2: Unit Test Matrix
echo -e "${BOLD}[Gate 2/4] Executing Comprehensive Unit Test Suites...${RESET}"
./gradlew testDebugUnitTest --stacktrace
echo -e "${GREEN}[PASS] All unit test suites passed (100% green).${RESET}"
echo ""

# Gate 3: Release Build Compilation
echo -e "${BOLD}[Gate 3/4] Compiling Release APK with R8 Optimization...${RESET}"
./gradlew assembleRelease --stacktrace
echo -e "${GREEN}[PASS] Release APK assembled successfully.${RESET}"
echo ""

# Gate 4: Performance Size Budget Assertion
echo -e "${BOLD}[Gate 4/4] Validating Release APK Size Against 5.0 MB Target...${RESET}"
chmod +x scripts/verify-apk-size.sh
./scripts/verify-apk-size.sh
echo -e "${GREEN}[PASS] Performance size budget verified.${RESET}"
echo ""

echo -e "${GREEN}${BOLD}=== ALL 4 QUALITY GATES PASSED! READY FOR GIT PUSH ===${RESET}"
