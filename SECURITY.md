# Security Policy & Vault Protection Architecture

- **Product:** Contact Explorer Enterprise Android (`ContactExplorer_KT`)
- **Brand Identity:** A CTYV Product
- **Lead Architect:** Priyanshu Sharma (CTYV)
- **Security Model:** Zero-Trust Offline Architecture
- **Legal Attribution:** Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.

---

## 1. Security Architecture Overview

Contact Explorer is engineered from first principles under a **Zero-Trust Offline Posture**. Unlike conventional OEM address books that synchronize contact records with cloud backends and third-party advertising SDKs, Contact Explorer guarantees complete privacy by physically preventing network egress at the Android OS sandbox layer.

```mermaid
flowchart TD
    subgraph OS_Sandbox["Android Linux OS Sandbox (UID Isolation)"]
        subgraph App_Boundary["Contact Explorer Process (:com.example.contactexplorer)"]
            UI["Jetpack Compose UI\n(Zero Network / Local Only)"]
            MVI["ExplorerViewModel\n(UDF Atomic StateFlow)"]
            Repo["ContactsRepository\n(300ms Debounced Observer)"]
            RoomDB[("Local SQLite Database\n(/data/data/.../databases/)\nRoom DAOs & Parameterized SQL")]
            Vault[("Local Device Vault\n(.cebak / JSON Archives)")]
        end

        Provider["Android ContactsContract\n(System ContentResolver)"]
        SAF["Storage Access Framework (SAF)\n& FileProvider (Read-Only URIs)"]
    end

    ExternalNetwork["❌ Remote Internet / Cloud\n(android.permission.INTERNET BLOCKED)"]
    ExternalApps["External Apps via Sharesheet\n(Ephemeral Single-URI Grant)"]

    UI --> MVI
    MVI --> Repo
    Repo --> RoomDB
    Repo --> Provider
    Repo --> Vault
    Vault --> SAF
    SAF --> ExternalApps

    App_Boundary -.x|Strictly Denied by OS| ExternalNetwork
```

---

## 2. Zero-Trust Offline Posture

### 2.1 Absolute Absence of Network Capabilities
- **Zero Internet Permission**: The application manifest (`AndroidManifest.xml`) **strictly omits** `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`. The Android Linux kernel physically denies network socket creation (`socket(AF_INET, ...)`) to the application UID.
- **Zero Remote Telemetry & Tracking**: The application contains no analytics libraries (Firebase Analytics, Mixpanel), no crash-reporting collectors (Crashlytics, Sentry), and no advertising SDKs.
- **No Background Phoning Home**: Background worker tasks (`WorkManager`) and foreground services are restricted solely to local on-device heuristics and SQLite indexing.

### 2.2 Controlled Sharesheet Delegation
- When exporting contact collections or vault backups to external destinations (e.g. Google Drive, email, or messaging apps), Contact Explorer never directly connects to remote APIs.
- Instead, it leverages Android's secure `Intent.ACTION_SEND` Sharesheet. The user explicitly selects the recipient application, and access is delegated via an ephemeral, cryptographically bound content URI with `Intent.FLAG_GRANT_READ_URI_PERMISSION`.

---

## 3. Local SQLite Sandbox & Vault Protection

### 3.1 OS-Level Linux UID Sandbox
- All application databases, shared preferences, and cache files reside within private internal storage:
  ```text
  /data/user/0/com.example.contactexplorer/databases/
  ```
- Android's multi-user Linux kernel assigns a unique User ID (UID) to Contact Explorer. Other applications running on the device cannot read or write to this directory without root compromise.

### 3.2 SQL Injection Immunity (CWE-89)
- All database operations are mediated by **Android Jetpack Room 2.8.4** Object-Relational Mapping (ORM).
- Room compiles all queries into SQLite parameterized statements (`SQLiteStatement` with `?` bind variables). Raw dynamic SQL concatenation is strictly forbidden across all 12 modules, entirely neutralizing SQL injection vulnerabilities.

### 3.3 Standalone Local Device Vault (`.cebak`)
- User-created contact hierarchies, custom categories, tags, notes, and call pattern audits can be snapshotted into local `.cebak` vault archives.
- Vault archives are stored locally within user-selected documents directories via the Android Storage Access Framework (SAF), enabling full data preservation independently of device OEM backups.
- **Uninstall Data Retention (`android:hasFragileUserData="true"`)**: Configured in `AndroidManifest.xml` to prompt the user to retain application databases upon uninstallation, preventing accidental loss of custom organization structures.

---

## 4. Safe vCard & File Import/Export Boundaries

### 4.1 Path Traversal Immunity (CWE-22)
- Contact Explorer strictly utilizes Android's `FileProvider` with a hardened XML path configuration (`res/xml/file_paths.xml`).
- Direct file path operations via raw strings (e.g., `new File(path)`) on external storage are banned.
- Imported content is read exclusively through Android's `ContentResolver.openInputStream(uri)`, preventing directory traversal payloads (e.g., `../../../../etc/passwd` or overwrite attacks on private databases).

### 4.2 Safe vCard Parser Boundaries (RFC 6350 / RFC 2426)
- **Tokenized Stream Reading**: `ContactTransfer.kt` parses incoming vCard streams line-by-line with bounded line length buffers.
- **Memory Bomb & DoS Protection**:
  - File streams exceeding reasonable address book sizes are processed in streaming batches rather than loaded in a single memory array, mitigating Zip Bomb and memory exhaustion attacks.
  - Large embedded base64 assets (such as high-resolution contact photos) are capped and sanitized to prevent JVM heap exhaustion.
- **Sanitized Custom Fields**:
  - Custom organizational tags (`X-TAGS:`, `X-FAVORITE:`, `CATEGORIES:`) are strictly alphanumeric-validated and length-capped before insertion into local SQLite tables.
  - Semicolon and comma delimiter escape sequences are normalized to prevent delimiter injection.

---

## 5. Automated Security Auditing & CodeQL Integration

To guarantee that no security regressions or data leaks enter the codebase, Contact Explorer enforces automated continuous security analysis:

- **GitHub Actions CodeQL Scan (`.github/workflows/codeql.yml`)**:
  - Automatically runs on all Pull Requests, pushes to `main`, and on a weekly Monday schedule (`cron: '30 4 * * 1'`).
  - Executes deep semantic Abstract Syntax Tree (AST) analysis for `java-kotlin` with the extended `+security-and-quality` ruleset.
  - Scans for taint flows, insecure intent dispatching, path traversal, and exported component leaks.
- **Android Lint Security Suite (`./gradlew lintDebug`)**:
  - Automated check verifying zero network permission leaks, secure `FileProvider` declarations, and protected component exports (`android:exported="false"` on non-launcher activities).

---

## 6. Supported Versions & Security Maintenance

| Version Line | Release Status | Security Support Level |
| :--- | :---: | :--- |
| **v6.x (Current - v6.3.0)** | :white_check_mark: Active | Full security patches, dependency updates, CodeQL CI/CD |
| **v5.x** | :warning: EoL | Maintenance only; upgrade to v6.x strongly recommended |
| **< v5.0** | :x: Deprecated | Unsupported legacy prototypes |

---

## 7. Vulnerability Disclosure Protocol

If you discover a security vulnerability or privacy concern within Contact Explorer:

1. **Do Not Open a Public Issue**: Please do not file public issues on GitHub for undisclosed security vulnerabilities.
2. **Report Privately**: Submit a confidential report via [GitHub Security Advisories](https://github.com/C-T-Y-V/Contact-Explorer/security/advisories/new) or contact the lead architect directly.
3. **Report Contents**:
   - Detailed description of the vulnerability and its potential impact.
   - Minimal proof-of-concept (PoC) or reproduction steps with sanitized sample data.
   - Device model, Android OS version, and Contact Explorer build version.
4. **SLA & Response Timeline**:
   - **Initial Acknowledgment**: Within **48 hours**.
   - **Triage & Severity Assessment**: Within **5 business days**.
   - **Remediation & Patch Release**: Within **14 business days** for high-severity issues.
