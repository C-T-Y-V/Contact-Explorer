# ADR 0003: Zero-Trust Offline Architecture & Local Sandbox Security

## Status
Accepted

## Context
Commercial mobile contact managers and default OEM dialers routinely embed analytics SDKs, advertising trackers, and cloud synchronization daemons. This introduces critical privacy risks: corporate executive directories, sensitive phone numbers, and communication histories are frequently exposed to third-party telemetry endpoints or unauthorized exfiltration.

## Decision
Contact Explorer establishes an uncompromising **Zero-Trust Offline Security Posture**:
1. **Network Prohibition**: The application completely omits `android.permission.INTERNET` from its `AndroidManifest.xml`.
2. **Local Sandboxing**: All persistent intelligence (custom groups, interaction statistics, call notes) is stored in a private Room SQLite database encrypted by the Android OS user-space sandbox.
3. **Safe Export Boundary**: Backup vaults and vCard exchanges are mediated strictly via local file descriptors and Android's `FileProvider` with temporary read-only permissions (`FLAG_GRANT_READ_URI_PERMISSION`).
4. **Resilient vCard Parsing**: The offline vCard parser defends against memory exhaustion, recursive folder expansion, and malformed base64 injection by enforcing byte-boundary limits and defensive field sanitization.

## Consequences
### Positive
- Absolute data confidentiality: impossible to leak contacts over HTTP, HTTPS, WebSockets, or DNS tunneling.
- Full compliance with strict corporate enterprise governance, GDPR, and defense-grade air-gapped deployment criteria.

### Negative / Trade-offs
- Cloud synchronization must be performed out-of-band by the user or enterprise MDM tool (e.g. via local vCard vault transfer).
