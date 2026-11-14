# Contributing to Contact Explorer

Thank you for your interest in contributing to **Contact Explorer — A CTYV Product**! We welcome contributions that align with our high standards of privacy, performance, and code quality.

---

## 🛠️ Development Setup

1. **Prerequisites**:
   - Android Studio (Ladybug / Meerkat or later)
   - JDK 17 or JDK 21
   - Android SDK 35 (Android 15) or Android SDK 36 preview

2. **Clone and Build**:
   ```bash
   git clone https://github.com/C-T-Y-V/Contact-Explorer.git
   cd Contact-Explorer
   ./gradlew assembleDebug
   ```

---

## 📐 Architecture & Dependency Rules

Before opening a Pull Request, please ensure your changes adhere to our core architectural values:

1. **Zero Internet & True Privacy**:
   - Contact Explorer must **never** declare `android.permission.INTERNET`.
   - All AI clustering, fuzzy searching, sorting, and serialization must execute 100% on-device.

2. **Gradle Version Catalog (`gradle/libs.versions.toml`)**:
   - **Zero Hardcoded Strings:** Never declare dependency versions as string literals in `build.gradle.kts`.
   - All dependencies and plugins must be added through `gradle/libs.versions.toml` using `libs.*` accessors.
   - Compose libraries must defer to `libs.compose.bom`.

3. **Code Style & Formatting**:
   - Adhere strictly to the repository's `.editorconfig` (LF line endings, 4-space indentation for Kotlin, UTF-8).
   - Use `./gradlew lintDebug` to verify static analysis compliance.

4. **Modular Decoupling**:
   - New features must reside within dedicated modules (`:feature:your-feature`) or follow the `ExplorerPlugin` contract in `:core:plugin-api`.
   - Circular dependencies between feature modules are strictly forbidden.

5. **Binary Footprint Budget**:
   - The compiled release APK must strictly remain **under 5.0 MB**. Avoid pulling in large third-party transitive dependencies.

---

## 🧪 Testing & Verification Checklist

Before submitting a Pull Request, verify:
```bash
# 1. Verify compilation
./gradlew compileReleaseKotlin

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Verify minification & size budget (< 5.0 MB)
./gradlew assembleRelease
```

---

## 📝 Commit Conventions

We follow Conventional Commits:
- `feat:` for new user-facing features or capabilities
- `fix:` for bug fixes
- `perf:` for performance optimizations and binary footprint reductions
- `refactor:` for code restructuring without behavioral changes
- `docs:` for documentation updates
- `chore:` for build tooling, version catalog, or maintenance

---

## 📄 License & Attribution Agreement

By contributing to Contact Explorer, you agree that your contributions will be licensed under the project's **MIT License (with Attribution)**.

```text
Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
```

All redistributions, forks, and derivative works must maintain credit stating: *"A CTYV Product"* or *"Inspired by A CTYV Product"*.
