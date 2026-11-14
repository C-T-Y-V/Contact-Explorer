# ADR 0002: Unidirectional Data Flow (UDF / MVI) State Management

## Status
Accepted

## Context
Earlier versions of Contact Explorer used fragmented state variables (`mutableStateOf` across ViewModel properties) coupled with an integer counter revision hack (`dataRevision.value++`) to force UI recomposition upon background database mutations. This caused state tearing, race conditions during rapid search input, and unpredictable cursor reload loops.

## Decision
We migrated the presentation layer to an **immutable Unidirectional Data Flow (MVI)** centered on a single source of truth:
1. **`ExplorerUiState`**: An immutable data class encapsulating 100% of the visible UI state (contacts, filtered contacts, active search query, selected filter mode, active theme preset, vault progress, dialog states).
2. **Atomic Mutations**: State transitions occur exclusively via `MutableStateFlow.update { ... }` within the ViewModel.
3. **Optimistic UI Updates**: User actions (e.g. toggling favorite, deleting contact, selecting group) immediately transform in-memory state for 0ms visual feedback, followed by asynchronous background persistence in Room SQLite and system ContactsProvider.

## Consequences
### Positive
- Zero state tearing and elimination of the legacy `dataRevision` hack.
- Simplified unit testing: test cases simply dispatch events to ViewModel or directly invoke state transformation reducers and assert on `ExplorerUiState`.
- Predictable recomposition in Jetpack Compose with minimal overhead.

### Negative / Trade-offs
- Requires creating immutable state copies on every user keystroke (negligible memory allocation on modern Android ART garbage collection).
