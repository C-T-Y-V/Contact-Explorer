---
name: Feature Request
about: Propose a new capability, architectural improvement, or UI/UX enhancement
title: '[FEATURE] '
labels: ['enhancement', 'feature-request']
assignees: ''
---

<!--
  Contact Explorer Enterprise Android
  A CTYV PRODUCT — Copyright © 2026 Priyanshu Sharma (CTYV)
-->

## 💡 Feature Overview
A clear and high-impact description of the capability or improvement you are proposing.

## ❓ Problem Statement & User Value
Describe the user friction or limitation this feature addresses:
*e.g. "When managing large address books with > 3,000 contacts, it is difficult to quickly..."*

## 🚀 Proposed Architectural Solution
Detail the technical approach and specify which architectural modules will be involved:
- [ ] `:app` (Jetpack Compose presentation, ViewModel state flow, UI components)
- [ ] `:core:api` (Shared interface definitions, data models, theme contracts)
- [ ] `:core:contacts` (System ContentResolver queries, vCard import/export engine)
- [ ] `:core:database` (Room SQLite schema, DAOs, indices, migrations)
- [ ] `:core:permissions` (Runtime permission request handling)
- [ ] `:core:plugin-api` / `:core:plugin-registry` (Dynamic plugin hooks)
- [ ] `:feature:ai` (On-device heuristic clustering, duplicate analysis)
- [ ] `:feature:appearance` (Custom theme sets, color tokens, layout styles)
- [ ] `:feature:calling` (Telephony dispatch, call intent handling)
- [ ] `:feature:groups` (Contact grouping, batch migration, smart rules)
- [ ] `:feature:search` (Multi-field fuzzy search, phonetic indexing)

## 🔒 Zero-Trust Offline Compliance (Mandatory Invariant)
Does this feature comply with Contact Explorer's strict **Zero-Trust Offline Policy**?
- [ ] **Yes**: This capability operates 100% on-device with zero internet permissions (`android.permission.INTERNET` remains absent).
- [ ] **Yes**: No personal contact data, notes, or metadata leaves the local device sandbox.

## 📉 Binary Size & Performance Budget Impact
- Estimated impact on the release APK size (target performance budget: `< 5.0 MB`).
- Will this feature introduce new dependencies into `gradle/libs.versions.toml`? If yes, provide rationale.
