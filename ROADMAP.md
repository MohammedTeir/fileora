# Roadmap and handoff

Read first: `README.md` (status), `docs/SPEC.md` (design), `docs/RELEASING.md` (CI and signing).

## Verified state (as of 2026-10-08)

- `agent-core` (pure Kotlin/JVM): 56 tests pass (`./scripts/test-core.sh`).
- `app` (Compose + SAF + Keystore): **compiles and runs on a real device** (Redmi Note 8 Pro).
- Gemini adapter: **works live** (model `gemini-3.5-flash-lite`, 0 errors).
- Dry run: works. Real run, confirmation screen, undo and delete-to-trash: **not yet verified on device**.
- CI: `setup-android` step removed (it failed on `sdkmanager tools`); first green Gradle run not yet confirmed.

## Rules for any contributor (do not break these)

1. `agent-core` must not import Android classes. All Android code goes in `app/`.
2. The model proposes, the engine authorizes. No tool may bypass `PolicyEngine`.
3. Every state-changing tool needs: plan (no side effects), policy risk level, verifier, undo entry, tests.
4. File contents, names and any device data sent to the model go under `untrusted_data` (prompt-injection defense).
5. Never log or send the API key. Use `Redactor`. Key stays in `KeystoreSecretStore`.
6. Task status must be computed from facts (`TaskStatus`), never from what the model says.
7. Every new feature: tests first, and add a mutation check (break the code on purpose, confirm a test fails).
8. Never commit keystores or secrets.

## P0: finish the MVP (do these first)

| # | Task | Acceptance criteria |
|---|---|---|
| 1 | Verify real run on device | With dry run off: create folder + move shows a confirmation with the full real paths; result says "2 changes completed and verified"; Undo restores the file; delete always asks and lands in `.agent_trash` (restorable). Fix anything that fails. |
| 2 | `PLAN_ONLY` status — **done in code, not yet run** | A dry run must not report `SUCCESS`. Add `TaskStatus.PLAN_ONLY` (or similar), update `AgentEngine`, UI label, tests. |
| 3 | Exact tool list in system prompt — **done in code, not yet run** | Model must not claim abilities it lacks (it claimed "read" and "create files"). Generate the capability list from `ToolRegistry`. Test: prompt contains every registered tool and nothing else. |
| 4 | Reduce request count — **prompt batching guidance + tests added; real request count on device not yet measured** | Dry run of "create folder and move file" took 7 AI requests / 10.7K tokens. Find why (log requests), aim for 3 or fewer. Add a test with a fake provider that counts calls. |
| 5 | Green CI | `ci.yml` passes on a branch. Bump `gradle/libs.versions.toml` to current stable versions (unverified now). Then tag `v0.1.0` and confirm `release.yml` produces signed APKs (`apksigner verify` passes). Needs the secrets in `docs/RELEASING.md`. |

## P1: make it usable

| # | Task | Acceptance criteria |
|---|---|---|
| 6 | `file.read` tool — **done in code, not yet run** | Read text files with a size cap (for example 100 KB) and encoding detection; binary refused; result wrapped as `untrusted_data`; risk level READ; tests including a file containing injection text ("ignore previous instructions"). |
| 7 | `file.write` / `file.append` | Create or edit text files. Risk MEDIUM, overwrite needs confirmation, old version saved for undo, verifier re-reads and compares. |
| 8 | Persistent history (Room) | Replace `InMemoryAuditLog` and in-memory `UndoManager` data with Room. Survives process death. History screen in UI. Secrets redacted before writing. |
| 9 | Background execution | Foreground service with notification + WorkManager. Task keeps running when the screen is off. Cancel button works. Confirmation requests arrive as notifications. |
| 10 | Checkpoint and resume | After each completed step, persist state. After app kill, offer to resume or roll back. |
| 11 | Usage and cost limits | Per-task token and request budget, hard stop with status `BLOCKED`, UI shows usage. |
| 12 | Privacy toggles | User chooses what the model sees: names only (default), or contents. Show a "what will be sent" preview. |

## P2: more capability

| # | Task | Notes |
|---|---|---|
| 13 | Optional "All files access" mode | `MANAGE_EXTERNAL_STORAGE` + `JvmFileSystem` behind an explicit opt-in screen. SAF cannot grant the storage root, Download, `Android/data` (Android 11+). Keep the protected-paths denylist and mandatory trash/undo. Not allowed on Play Store, fine for direct distribution. |
| 14 | Archive tools | zip / unzip with zip-slip protection (reject `..` entries), size and entry-count limits (zip-bomb defense). |
| 15 | Media tools | FFmpeg (compress/convert). First task that makes per-ABI APKs differ. Verify output is playable and non-empty. |
| 16 | Search by content | Index only user-approved folders, bounded size. |
| 17 | Scheduled tasks | WorkManager; scheduled tasks run in MANUAL or low-risk-only mode unless pre-approved. |
| 18 | More providers | Implement `AIProvider` for Claude / OpenAI. Reuse `ResilientProvider`. Shared contract tests for every provider. |
| 19 | Model routing | Cheap model for listing/planning, stronger model for hard recovery. |

## P3: advanced (later, higher risk)

- App/package info and (with Shizuku or root) install/uninstall: CRITICAL risk, always confirm.
- Linux runtime (proot) for scripts: sandbox hard, network off by default.
- Root / Shizuku access to `Android/data`.
- Accessibility-based UI automation: very high abuse potential, needs its own threat model.

## Storage access strategy (decision)

Android 11+ (scoped storage) will not let the Storage Access Framework (SAF) picker grant the storage root, `Download`, `Android/data` or `Android/obb`. The picker shows "Can't use this folder". This is an OS rule, not a bug. Do not try to work around it inside SAF.

| Option | What it gives | Trade-off |
|---|---|---|
| **A. SAF subfolders (current, keep as DEFAULT)** | Safest: the agent sees only folders the user picked | User grants folders one by one |
| **B. "All files access" (`MANAGE_EXTERNAL_STORAGE`)** | Whole shared storage in one grant | Large blast radius. Play Store restricts it; allowed for direct distribution |
| **C. Shizuku or root** | Reaches `Android/data` | Advanced, later phase (P3) |

Rules:
- A stays the default. B is an optional "full access mode" the user must turn on, with a clear warning screen.
- B reuses `JvmFileSystem` (normal paths); no core rewrite needed.
- Under B the sandbox, `PolicyEngine`, protected-paths list, confirmation-before-delete, trash and undo matter MORE, because a prompt-injection attack could reach everything. They must stay mandatory.
- Do not build B until the agent has been tested on a few folders under A (real run, confirmation, undo, delete-to-trash all verified).

## Known weaknesses to fix or watch

- `SafFileSystem` is slow in huge folders (linear lookups); free space is unknown under SAF.
- Prompt injection is reduced, not eliminated. A manipulated model can still propose harmless-looking moves; confirmation shows real paths for that reason.
- Dependency versions in `libs.versions.toml` and GitHub Action versions are unverified/old. Dependabot is configured.
- Gemini adapter was written from public docs; if Google changes the API shape, errors surface as `ProviderException`.
- Pin GitHub Actions to commit SHAs before relying on the release pipeline (see `docs/RELEASING.md`).

## Suggested order

1 → 2 → 3 → 4 → 5, then 6 → 8 → 9, then 7 and 12, then P2.
