#!/usr/bin/env bash
# ==============================================================================
# Script: setup-dev-env.sh
# Purpose: Professional DevOps developer workstation onboarding & validation.
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

echo -e "${CYAN}${BOLD}=== Contact Explorer: Developer Workstation Environment Setup ===${RESET}"
echo -e "${CYAN}Attribution:${RESET} Priyanshu Sharma (CTYV) — A CTYV Product"
echo ""

# 1. Check JDK Version (Requires Java 17 or 21)
echo -e "${BOLD}[1/4] Checking Java JDK Installation...${RESET}"
if ! command -v java >/dev/null 2>&1; then
    echo -e "${RED}[FAIL] Java is not installed or not in PATH!${RESET}"
    echo -e "${YELLOW}Please install OpenJDK 17 or 21 (Temurin / Corretto recommended).${RESET}"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ]; then
    echo -e "${RED}[FAIL] Java version $JAVA_VER detected. Contact Explorer requires JDK 17 or higher.${RESET}"
    exit 1
else
    echo -e "${GREEN}[OK] Compatible Java version detected (JDK $JAVA_VER).${RESET}"
fi

# 2. Check Android SDK Environment
echo -e "${BOLD}[2/4] Validating Android SDK Environment...${RESET}"
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    if [ -d "$HOME/AppData/Local/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"
        echo -e "${GREEN}[OK] Resolved standard Android SDK at $ANDROID_HOME${RESET}"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        echo -e "${GREEN}[OK] Resolved standard macOS Android SDK at $ANDROID_HOME${RESET}"
    elif [ -d "/usr/local/lib/android/sdk" ]; then
        export ANDROID_HOME="/usr/local/lib/android/sdk"
        echo -e "${GREEN}[OK] Resolved standard Linux Android SDK at $ANDROID_HOME${RESET}"
    else
        echo -e "${YELLOW}[WARN] ANDROID_HOME environment variable is not explicitly set.${RESET}"
        echo -e "${YELLOW}Ensure local.properties contains 'sdk.dir=<path_to_sdk>'.${RESET}"
    fi
else
    echo -e "${GREEN}[OK] Android SDK located: ${ANDROID_HOME:-$ANDROID_SDK_ROOT}${RESET}"
fi

# 3. Check Git & Core Configuration
echo -e "${BOLD}[3/4] Validating Git Configuration & Initializing Hooks...${RESET}"
if ! command -v git >/dev/null 2>&1; then
    echo -e "${RED}[FAIL] Git is not installed or not found in PATH!${RESET}"
    exit 1
fi
echo -e "${GREEN}[OK] Git is available: $(git --version)${RESET}"
git config core.hooksPath .githooks
echo -e "${GREEN}[OK] Configured git config core.hooksPath .githooks${RESET}"

# 4. Verify Gradle Wrapper Permissions
echo -e "${BOLD}[4/4] Verifying Gradle Executable Permissions...${RESET}"
chmod +x gradlew
echo -e "${GREEN}[OK] gradlew permissions verified (+x).${RESET}"

echo ""
echo -e "${GREEN}${BOLD}=== Developer Workstation Setup Complete! ===${RESET}"
echo -e "To verify local build health, execute: ${CYAN}./scripts/run-all-checks.sh${RESET}"
