# ADR 0004: Centralized Gradle Version Catalog (libs.versions.toml)

## Status
Accepted

## Context
Across 12 multi-project Gradle modules, third-party libraries (Jetpack Compose BOM, Room, AndroidX Lifecycle, Kotlin Coroutines, Material 3) were previously declared as raw, hardcoded string literals. This caused version drift, duplicate dependency resolutions, subtle binary incompatibilities during incremental builds, and high maintenance overhead when upgrading SDK targets.

## Decision
We standardized on Gradle's official **Version Catalog** mechanism (`gradle/libs.versions.toml`):
1. **Single Source of Truth**: All library coordinates, plugin identifiers, and SDK versions are pinned in `gradle/libs.versions.toml`.
2. **Type-Safe Accessors**: Module `build.gradle.kts` files reference dependencies via auto-generated typesafe accessors (e.g. `libs.compose.material3`, `libs.room.ktx`).
3. **Dependency Bundles**: Commonly co-located dependencies (such as Compose UI, Preview, and Graphics) are grouped into logical bundles (e.g. `libs.bundles.compose`).
4. **Automated Dependabot Coverage**: Dependabot is configured to continuously monitor `gradle/libs.versions.toml` for CVE security advisories and automated version bumps.

## Consequences
### Positive
- Zero version drift across all 12 modules.
- Upgrading a core framework (e.g. Kotlin, Compose BOM, Room) requires modifying only a single line in `libs.versions.toml`.
- Improved IDE autocomplete and Gradle configuration caching performance.

### Negative / Trade-offs
- Requires adhering to strict TOML syntax and version catalog naming conventions.
