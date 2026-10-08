# Roadmap and handoff

Read first: `README.md` (status), `docs/SPEC.md` (design), `docs/RELEASING.md` (CI and signing).

## Verified state (as of 2026-10-08)

- `agent-core` (pure Kotlin/JVM): 89 tests pass (`./gradlew :agent-core:check`).
- `app` (Compose + SAF + Keystore): **compiles and runs on a real device** (Redmi Note 8 Pro).
- Gemini adapter: **works live** (model `gemini-3.5-flash-lite`, 0 errors).
- Dry run: works. Real run, confirmation screen, undo and delete-to-trash: **not yet verified on device**.
- CI: green, and the release workflow builds signed APKs (reported by the owner; `apksigner verify` output not seen by me).
- Local validation: the core suite passes; the sandbox Android build is blocked by the missing Android SDK, not a reported source error.

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
| 5 | Green CI/release — **workflow hardening implemented; GitHub run and signing verification remain** | CI/release workflows now pin actions to immutable SHAs and run core tests, Android builds, signature checks, and checksums. Run them on GitHub with Android SDK/signing secrets, then tag `v0.1.0`; the sandbox cannot perform this external verification. |

## P1: make it usable

| # | Task | Acceptance criteria |
|---|---|---|
| 6 | `file.read` tool — **done in code, not yet run** | Read text files with a size cap (for example 100 KB) and encoding detection; binary refused; result wrapped as `untrusted_data`; risk level READ; tests including a file containing injection text ("ignore previous instructions"). |
| 7 | `file.write` / `file.append` — **done** | Creates or edits bounded UTF-8 text files (100 KiB cap). Moderate-risk writes require confirmation, old versions are saved for undo, and the verifier checks the resulting size and SHA-256. JVM and SAF implementations are covered by core tests where possible. |
| 8 | Persistent history (Room) — **done** | `RoomAuditLog` persists redacted audit entries, `RoomUndoStore` persists move/delete/restore actions, the app shows recent history, and the latest persisted task remains undoable after process restart. |
| 9 | Background execution — **implemented; device verification remains** | Foreground service owns the run with a notification, cancellation, confirmation actions, progress broadcasts, and result delivery to the UI. A delayed unique WorkManager watchdog relaunches a checkpointed task only after the service process has died. |
| 10 | Checkpoint and resume — **implemented; device validation remains** | The service persists task inputs, provider-neutral messages, and latest progress after each tool-result turn; Resume restores the conversation and Discard clears it. |
| 11 | Usage and cost limits — **implemented in core/UI, not yet verified on device** | Per-task request, input-token, and output-token budgets hard-stop with status `BLOCKED`; current-task usage is shown in the UI. |
| 12 | Privacy toggles — **implemented in core/UI, not yet verified on device** | User chooses what the model sees: names only (default), or contents. The task screen shows a "what will be sent" preview, and `file.read` enforces the choice. |

## P2: more capability

| # | Task | Notes |
|---|---|---|
| 13 | Optional "All files access" mode | `MANAGE_EXTERNAL_STORAGE` + `JvmFileSystem` behind an explicit opt-in screen. SAF cannot grant the storage root, Download, `Android/data` (Android 11+). Keep the protected-paths denylist and mandatory trash/undo. Not allowed on Play Store, fine for direct distribution. |
| 14 | Archive tools — **implemented in core** | `file.compress` / `file.extract` use bounded ZIP archives, reject zip-slip paths, enforce entry and expanded-size limits, never overwrite, and record undo actions. Device/SAF validation remains. |
| 15 | Media tools — **core foundation implemented** | `media.probe` / `media.convert` use an injected backend; `JvmFfmpegBackend` invokes FFmpeg/FFprobe without a shell and verifies non-empty playable output. Android native/SAF backend and remaining media operations remain. |
| 17 | Scheduled tasks — **implemented** | Room-backed schedule definitions, WorkManager periodic scheduling, foreground-service launch, and Compose schedule/cancel controls are implemented. Scheduled runs default to manual mode; physical-device validation remains. |
| 16 | Search by content — **implemented in core** | `file.search_content` searches approved roots with binary skipping, per-file/total byte bounds, result caps, short untrusted snippets, and names-only privacy blocking. Device/SAF validation remains. |
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
