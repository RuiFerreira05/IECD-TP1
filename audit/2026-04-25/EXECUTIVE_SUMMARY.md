# Executive Summary — Full Codebase Audit

**Date:** 2026-04-25
**Scope:** Server, client, common, tests, build, config
**Total Findings:** 88

---

## Severity Breakdown

| Severity | Count | Description |
|----------|-------|-------------|
| CRITICAL | 16 | Crashes, data corruption, or security breach in production |
| HIGH | 24 | Logic errors, concurrency hazards, design flaws |
| MEDIUM | 30 | Bad practices, anti-patterns, maintainability issues |
| TEST | 6 | Test quality, coverage, and reliability |
| BUILD/CONFIG | 12 | Build, logging, schema, and git hygiene |

---

## Top 5 Most Urgent Fixes

### 1. `winner.toString()` — every game-over crashes the client
`XMLServerMessageBuilder.java:299` emits `User@3b9a0c` instead of a UUID. The client's `UUID.fromString()` throws on every game-over push.
**Fix:** `winner.getUserId().toString()`

### 2. SHA-256 without salt — all passwords trivially crackable
`UserStore.java:52-62` uses unsalted SHA-256. A leaked `users.xml` exposes every password via rainbow tables.
**Fix:** Use bcrypt or Argon2id.

### 3. `sendMessage()` not thread-safe — protocol corruption
`Connection.java:101-111` allows concurrent writes to the same stream. Interleaved frames corrupt the wire protocol.
**Fix:** Synchronize all writes on the `DataOutputStream`.

### 4. XXE in all XML parsers — arbitrary server file read
6 `DocumentBuilderFactory` instances have no external entity protections. A malicious client can read `/etc/passwd` or any server file.
**Fix:** Disable DOCTYPE declarations and external entities on all parsers.

### 5. Non-atomic persistence write — crash loses all user data
`XmlUserRepository.java:138` writes directly to `users.xml`. A power loss mid-write produces a truncated file that fails schema validation on restart — zero users loaded.
**Fix:** Write to temp file + atomic rename.

---

## Critical Security Posture

| Control | Status |
|---------|--------|
| Password hashing | Unsalted SHA-256 (broken) |
| Transport encryption | None (plaintext TCP) |
| XML external entities | Vulnerable (6 parsers) |
| Session binding | Token not bound to connection |
| Authentication on search | Missing (anyone can enumerate users) |
| Rate limiting | None on any endpoint |
| Input validation | None on any handler |

**Verdict:** Not safe for production deployment without addressing items 2.1–2.7.

---

## Concurrency Health

The codebase has **11 distinct concurrency bugs**, ranging from thread-unsafe `sendMessage()` to TOCTOU races in session management, game state, and user store operations. The most impactful is the client-side thread-safety flaw (finding 3.1) where the network thread and CLI thread access screens and `StateMachine` without any synchronization.

---

## Test Coverage

- **10 test classes** exist, but `StateMachineTest` (5 tests) cannot compile — dead code
- **10 critical classes** have zero test coverage, including `GameHandler`, `DotsAndBoxesGame`, `AuthHandler`, and `GameController`
- **3 tests** have order-dependent state pollution from mutable `ServerConfiguration` statics
- **No `@Timeout`** on any test — blocking I/O tests can hang forever

**Effective coverage:** ~40% of important paths, with 5 known-broken tests.

---

## Recommended Fix Order

See the [Priority Fix Order](./COMPREHENSIVE_CODEBASE_AUDIT.md#appendix-priority-fix-order) table in the full report. The first 10 fixes address the most impactful issues with the least effort.

---

## Full Report

The complete audit with all 88 findings, detailed descriptions, and suggested fixes:

**[`COMPREHENSIVE_CODEBASE_AUDIT.md`](./COMPREHENSIVE_CODEBASE_AUDIT.md)**
