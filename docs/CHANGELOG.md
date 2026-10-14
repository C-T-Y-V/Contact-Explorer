# Changelog 📜

All notable changes to **Contact Explorer** will be documented in this file.
The project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v6.0.0] - Persistent Data Vault & Dual-Mode Export (4.4 MB)
### Added
- **Uninstall Data Retention (`android:hasFragileUserData`)**:
  - Automatically triggers Android OS's native prompt (*"Keep app data?"*) upon uninstallation so user organization is never lost.
- **Standalone Local Device Vault**:
  - Save full contact hierarchy, custom groups, tags, notes, favorites, and smart rules into a portable `.cebak` / `.json` file stored in device storage (Downloads / Documents).
  - 1-tap full structural restoration from local phone storage after re-installing or resetting devices.
- **Dual-Mode Advanced Export**:
  - **Organization Format Choice**:
    - **Organized Format (Recommended)**: Preserves custom groups (`CATEGORIES:` in vCard 3.0), tags (`X-TAGS:`), notes, and favorites (`X-FAVORITE:`).
    - **Standard / Default Format**: Clean universal vCard or CSV without extra app tags or metadata for maximum compatibility with legacy devices, car Bluetooth, or basic SIM/PCs.
  - **Destination Choice**:
    - **Offline (Phone Storage)**: Saves directly to phone storage (Downloads / Documents) via Android Storage Access Framework with zero internet or cloud dependence.
    - **Online / Cloud Share**: Generates secure shareable URI via `FileProvider` and launches native Android Sharesheet (`Intent.ACTION_SEND`), enabling 1-tap export directly to Google Drive, Gmail, WhatsApp, Telegram, etc.
- **Zero-Internet Architecture**: Contact Explorer maintains strict **0 internet permissions** (`android.permission.INTERNET` is NOT requested), guaranteeing 100% data privacy and offline integrity while delegating network transfers to trusted user apps via Android Sharesheet.

### Changed
- Unified selection and top-bar export workflows into `AdvancedExportDialog`.
- Replaced separate file export menus with a consolidated, modern modal offering granular format and destination options.

---

## [v5.0.0] - Production Polish & UX Overhaul (4.4 MB)
### Added
- **Visual Groups Dashboard**: Dedicated card-based view showing real-time member counts and name avatar previews.
- **Ungrouped Contacts Manager**: Dedicated filter and hub for organizing unsorted contacts.
- **Direct Calling**: One-tap dialing via `CALL_PHONE` and `Intent.ACTION_CALL` directly from contact list cards.
- **Group Overflow Actions**: 3-dot overflow menu for individual groups (Add Contact to Group, Merge Group, Delete Group).
- **Safe Inset Padding**: Explicit status bar and navigation bar insets preventing overlaps with camera cutouts and gesture pills.

### Changed
- **TopBar Consolidation**: Replaced crowded individual action buttons with a unified 3-dot (⋮) overflow menu.
- **Single-Line Ellipsis Headers**: Contact names and group titles now cleanly truncate with `...` and feature a click-to-expand details dialog.
- **Streamlined Feed**: Removed redundant sticky banner headers above contacts list; list items begin immediately below search bar.
- **Optimistic UI Engine**: Star, tag, delete, and group assignment operations now render with 0ms perceptual latency.

---

## [v4.0.0] - R8 Full-Mode & ProGuard Optimization (4.4 MB)
### Added
- Surgical ProGuard / R8 keep rules for Room SQLite entities and Kotlin Coroutine reflection.
- Strict release APK size budget (< 5.0 MB enforced in CI).

### Changed
- **Binary Footprint Reduction**: Shrunk APK size from 18.5 MB down to **4.4 MB** (76% reduction).
- Enabled `isMinifyEnabled = true` and `isShrinkResources = true` with full-mode tree-shaking.
- Resolved Room TypeConverter crashes under aggressive code minification.

---

## [v3.0.0] - In-Process Plugin Engine (18.5 MB)
### Added
- **`core:plugin-api`**: Domain contracts for extensible contact actions, toolbar extensions, and pluggable screens.
- **`core:plugin-registry`**: Dynamic compile-time and runtime plugin lifecycle manager.
- Pluggable AI Clustering, Appearance, and Search modules.

### Changed
- Decoupled `MainActivity` from hardcoded feature dependencies.

---

## [v2.0.0] - Multi-Module Clean Architecture (18.2 MB)
### Added
- Multi-module Gradle build graph separating `:core` and `:feature` layers.
- **30-Day Recycle Bin**: Safe soft deletion with full restoration to system contacts.
- **Granular VCF Export**: Selective field sharing (name, phone, email, notes).
- Room SQLite caching layer for metadata and offline tag indexing.

---

## [v1.0.0] - Initial Monolithic Prototype (29.7 MB)
### Added
- Core address book reading from Android `ContactsContract`.
- Basic Jetpack Compose UI with contact lists and search bar.
- Proof-of-concept AI keyword categorization heuristics.
