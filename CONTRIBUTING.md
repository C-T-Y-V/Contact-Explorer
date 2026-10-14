# Contributing to Contact Explorer

Thank you for your interest in contributing to **Contact Explorer**! We welcome contributions from developers of all skill levels.

---

## 🛠️ Development Setup

1. **Prerequisites**:
   - Android Studio (Ladybug / Meerkat or later)
   - JDK 17 or JDK 21
   - Android SDK 35 (Android 15)

2. **Clone and Build**:
   ```bash
   git clone https://github.com/C-T-Y-V/contact-explorer.git
   cd contact-explorer
   ./gradlew assembleDebug
   ```

---

## 📐 Architecture Principles

Before opening a Pull Request, please ensure your changes adhere to our core architectural values:

1. **Zero Cloud Dependencies & 100% Privacy**:
   - Contact Explorer must **never** request `android.permission.INTERNET`.
   - All AI, grouping, fuzzy searching, and sorting must run 100% on-device.

2. **Modular Decoupling**:
   - New features should be implemented as separate feature modules (`:feature:your-feature`) or plugins adhering to the `ExplorerPlugin` contract in `:core:plugin-api`.
   - Do not add direct circular dependencies between feature modules.

3. **0ms Optimistic UI**:
   - User interactions (favorites, deletes, tags, groups) must reflect immediately using Compose `State`. Avoid blocking the UI thread on disk or ContentResolver I/O.

4. **Footprint Awareness**:
   - Keep dependencies lean. Avoid adding heavy third-party libraries when Android Jetpack or standard Kotlin APIs suffice.

---

## 🧪 Testing and Verification

Before submitting your Pull Request, verify that all release compilation and minification passes:

```bash
# Verify release compilation & Kotlin syntax
./gradlew compileReleaseKotlin

# Verify R8 minification and resource shrinking
./gradlew assembleRelease
```

---

## 📝 Commit Conventions

We follow standard Conventional Commits:
- `feat:` for new user-facing features or capabilities
- `fix:` for bug fixes
- `perf:` for performance optimizations and binary footprint reductions
- `refactor:` for code restructuring without behavioral changes
- `docs:` for documentation updates

---

## 📄 License

By contributing to Contact Explorer, you agree that your contributions will be licensed under the project's [Apache 2.0 License](LICENSE).
