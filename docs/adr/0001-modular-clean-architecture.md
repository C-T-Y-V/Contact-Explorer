# ADR 0001: Modular Clean Architecture & Plugin Extension System

## Status
Accepted

## Context
Default Android address book applications often coalesce into monolithic Activities containing multiple responsibilities: direct cursor manipulation, SQL querying, layout inflation, telephony intent handling, and visual theme styling. In Contact Explorer, this led to a legacy `MainActivity.kt` exceeding 2,700 lines of code with brittle state synchronization and high regression risk.

To support enterprise scalability, clean testing boundaries, and collaborative engineering, a modular architecture standard was required that respects Google's Guide to App Architecture while preserving rapid local compilation for solo developers.

## Decision
We adopted a **Multi-Module Clean Architecture** segregated into three tiers:
1. **Presentation (`:app`)**: Application shell, navigation graph, theme initialization, and modular Jetpack Compose screens.
2. **Domain & Contracts (`:core:api`, `:core:plugin-api`, `:core:plugin-registry`)**: Pure Kotlin domain models, extension contracts (`ContactAction`, `ToolbarAction`, `ScreenContribution`), and an in-process plugin bus.
3. **Data & Infrastructure (`:core:contacts`, `:core:database`, `:core:permissions`)**: ContentProvider queries, Room SQLite persistence, and runtime permission handlers.
4. **Feature Modules (`:feature:*`)**: Isolated capabilities (`search`, `ai`, `appearance`, `calling`, `groups`) that communicate exclusively via the plugin bus.

## Consequences
### Positive
- Strict DAG dependency tree prevents circular coupling.
- Pure Kotlin domain layer allows lightning-fast JVM unit testing without Android emulator or Robolectric overhead.
- Features can be independently toggled, compiled, or refactored without breaking core contacts management.

### Negative / Trade-offs
- Increased initial Gradle configuration complexity (managed and mitigated via Gradle Version Catalog).
