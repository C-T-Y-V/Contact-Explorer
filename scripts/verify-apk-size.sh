#!/usr/bin/env bash
# ==============================================================================
# Script: verify-apk-size.sh
# Purpose: Automated verification assessing Release APK size against 5.0 MB target.
# Product: Contact Explorer Android — A CTYV Product
# Attribution: Priyanshu Sharma (CTYV)
# ==============================================================================

set -euo pipefail

# Target performance budget: 5.0 MiB (5,242,880 bytes) - Optional target benchmark
BUDGET_BYTES="${MAX_APK_SIZE_BYTES:-5242880}"
STRICT_BUDGET="${STRICT_BUDGET:-false}"
TARGET_PATH="${1:-}"

# Terminal color codes (if supported)
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    CYAN='\033[0;36m'
    YELLOW='\033[1;33m'
    BOLD='\033[1m'
    RESET='\033[0m'
else
    GREEN=''
    RED=''
    CYAN=''
    YELLOW=''
    BOLD=''
    RESET=''
fi

echo -e "${CYAN}${BOLD}=== Contact Explorer: Release APK Size Assessment ===${RESET}"
echo -e "${CYAN}Target Performance Budget:${RESET} 5.0 MB (${BUDGET_BYTES} bytes) [Optional Benchmark]"

# Locate Release APK
if [ -n "$TARGET_PATH" ] && [ -f "$TARGET_PATH" ]; then
    APK_FILE="$TARGET_PATH"
else
    # Find newest release APK in standard output path
    APK_FILE=$(find app/build/outputs/apk/release -type f -name "*.apk" 2>/dev/null | head -n 1 || true)
fi

if [ -z "$APK_FILE" ] || [ ! -f "$APK_FILE" ]; then
    echo -e "${RED}${BOLD}[ERROR] Release APK was not found!${RESET}"
    echo -e "${YELLOW}Expected location: app/build/outputs/apk/release/*.apk${RESET}"
    echo -e "${YELLOW}Please run './gradlew assembleRelease' before executing this check.${RESET}"
    exit 1
fi

# Extract file size portably (Linux / macOS / Git Bash)
if stat -c%s "$APK_FILE" >/dev/null 2>&1; then
    SIZE_BYTES=$(stat -c%s "$APK_FILE")
elif stat -f%z "$APK_FILE" >/dev/null 2>&1; then
    SIZE_BYTES=$(stat -f%z "$APK_FILE")
else
    SIZE_BYTES=$(wc -c < "$APK_FILE" | tr -d ' ')
fi

# Calculate size in MB (using awk for floating-point precision)
SIZE_MB=$(awk -v size="$SIZE_BYTES" 'BEGIN { printf "%.2f", size / 1048576 }')
BUDGET_MB=$(awk -v budget="$BUDGET_BYTES" 'BEGIN { printf "%.2f", budget / 1048576 }')

echo -e "Located APK: ${BOLD}${APK_FILE}${RESET}"
echo -e "Measured Size: ${BOLD}${SIZE_MB} MB${RESET} (${SIZE_BYTES} bytes)"

# Budget Assessment
if [ "$SIZE_BYTES" -le "$BUDGET_BYTES" ]; then
    SAVED_BYTES=$((BUDGET_BYTES - SIZE_BYTES))
    SAVED_MB=$(awk -v diff="$SAVED_BYTES" 'BEGIN { printf "%.2f", diff / 1048576 }')
    echo -e "${GREEN}${BOLD}✅ [PASS] Release APK is well within target size budget!${RESET}"
    echo -e "${GREEN}Under budget by: ${SAVED_MB} MB (${SAVED_BYTES} bytes margin)${RESET}"
    echo -e "${CYAN}Quality Gate Check: PASSED (Budget Compliance)${RESET}"
    exit 0
else
    OVER_BYTES=$((SIZE_BYTES - BUDGET_BYTES))
    OVER_MB=$(awk -v diff="$OVER_BYTES" 'BEGIN { printf "%.2f", diff / 1048576 }')
    echo -e "${YELLOW}${BOLD}⚠️ [ADVISORY] Release APK exceeds target benchmark (+${OVER_MB} MB)!${RESET}"
    echo -e "${YELLOW}Measured: ${SIZE_MB} MB | Target: ${BUDGET_MB} MB${RESET}"

    if [ "$STRICT_BUDGET" = "true" ]; then
        echo -e "${RED}${BOLD}❌ [STRICT MODE] Failing build because STRICT_BUDGET=true${RESET}"
        exit 1
    else
        echo -e "${CYAN}Note: 5.0 MB is an optional performance guideline. Build proceeds successfully.${RESET}"
        if [ -n "${GITHUB_ACTIONS:-}" ]; then
            echo "::warning::Release APK size (${SIZE_MB} MB) exceeds target benchmark of ${BUDGET_MB} MB (optional guideline, non-blocking)."
        fi
        exit 0
    fi
fi
