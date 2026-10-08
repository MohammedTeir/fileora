# Device Agent — Task List (done vs. not done)

Compared: `README.md`, `ROADMAP.md`, `docs/SPEC.md` (identical to the "full idea v4" file) and the actual code in the zip.
Date of review: 2026-10-08. The core suite was run locally; the Android APK build could not proceed because this sandbox has no Android SDK.

**Legend**
- `[x]` Done and covered by tests (agent-core) or confirmed on a real device
- `[~]` Code exists but **not verified on device / not complete**
- `[ ]` Not started

---

## 0. Summary

| Area | State |
|---|---|
| Core agent loop, policy, verification, recovery, undo, Gemini | Done (83 tests pass locally) |
| Android app: UI, Keystore, SAF, Room history | Written, compiles, runs on Redmi Note 8 Pro |
| Real run / confirmation / undo / trash on device | **Not verified yet (P0 #1)** |
| Background execution | Implemented: foreground service, notification actions, cancel path, WorkManager recovery watchdog, progress/result UI integration; device verification remains |
| Checkpoints | Implemented: exact provider-neutral conversation and latest progress persist after tool-result turns; Resume/Discard implemented. Device validation remains |
| Privacy toggles, cost limits | Implemented in core/UI; device verification still pending |
| Media (FFmpeg), Linux runtime, packages, Android tools, root | Not started (spec phases 2, 4–8) |

Spec phases: **Phase 1 ≈ 90%**, **Phase 3 ≈ 70%** (no stdout/stderr because no shell tool exists yet), **Phases 2, 4, 5, 6, 7, 8: 0–10%**.

---

## 1. P0 — Finish the MVP (ROADMAP)

- [~] **#1 Verify real run on device** (dry run OFF): create folder + move shows confirmation with full real paths; "2 changes completed and verified"; Undo restores; delete asks and lands in `.agent_trash`
  - Dry run: works on device. Everything else on this line: **not verified**.
- [~] **#2 `PLAN_ONLY` status** — in code (`TaskStatus.PLAN_ONLY`, `determineStatus`), not run on device
- [~] **#3 Exact tool list in system prompt** — generated from `ToolRegistry`; not run on device
- [~] **#4 Reduce AI request count** (was 7 requests / 10.7K tokens, target ≤ 3) — prompt batching guidance + tests added; **real count on device not measured**
- [x] **#5 Green CI + signed APKs** — reported by owner (`apksigner verify` output not seen)
  - [ ] Bump `gradle/libs.versions.toml` to current stable (file still says "starting points", Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12.01)
  - [ ] Pin GitHub Actions to commit SHAs
  - [ ] Tag `v0.1.0` and confirm release workflow output

## 2. P1 — Make it usable (ROADMAP)

- [~] **#6 `file.read`** — implemented with size cap, untrusted_data wrapping, tests; not run on device
- [x] **#7 `file.write` / `file.append`** — done (100 KiB cap, confirmation, undo of old version, SHA-256 verify)
- [x] **#8 Persistent history (Room)** — `RoomAuditLog`, `RoomUndoStore`, history list in UI, latest task undoable after restart
- [~] **#9 Background execution** — foreground `AgentTaskService` owns runs when the screen is off, shows progress/confirmation notifications, supports cancellation, and delivers progress/results to the UI. A delayed unique `AgentTaskRecoveryWorker` relaunches a checkpointed task after process death; device verification remains.
- [~] **#10 Checkpoint and resume** — foreground service persists task inputs, provider-neutral messages after each tool-result turn, and latest progress; after process death the app offers Resume or Discard. Core coverage passes; device validation remains.
- [~] **#11 Usage and cost limits** — per-task request, input-token, and output-token budgets now hard-stop with `BLOCKED`; actual task usage is returned in `TaskOutcome` and shown in `usageSummary`, with core regression coverage. Not yet verified on device; provider usage is unavailable when an adapter omits usage metadata.
- [~] **#12 Privacy toggles** — names-only is the default; the user can explicitly allow file contents, the task screen shows a "what will be sent" preview, and `file.read` enforces the setting. Core tests pass; device verification remains.

## 3. P2 — More capability (ROADMAP)

- [ ] #13 Optional "All files access" mode (`MANAGE_EXTERNAL_STORAGE` + `JvmFileSystem`)
- [ ] #14 Archive tools (zip/unzip, zip-slip and zip-bomb protection)
- [ ] #15 Media tools (FFmpeg)
- [ ] #16 Search by content (indexing approved folders)
- [ ] #17 Scheduled tasks (WorkManager)
- [ ] #18 More providers (Claude / OpenAI) + shared contract tests
- [ ] #19 Model routing

## 4. P3 — Advanced (ROADMAP)

- [ ] App/package info; install/uninstall with Shizuku or root
- [ ] Linux runtime (proot)
- [ ] Root / Shizuku access to `Android/data`
- [ ] Accessibility-based UI automation (needs its own threat model)

---

## 5. Against the full spec (SPEC.md)

### 5.1 Core loop and recovery (spec §3, §9–12, §30–31)
- [x] Understand → inspect → plan → policy → execute → verify loop
- [x] Plan step with no side effects before execution
- [x] Mandatory verification (mkdir, copy, write, append, move, rename, delete verifiers)
- [x] Error classification + diagnosis (`ErrorAnalyzer`, 7 recovery actions)
- [x] Bounded retries (3 per item), identical-failing-call refusal, step/failure/call limits
- [x] Failure memory within a task (`AttemptRecord`)
- [x] Final states computed from facts (all 10 spec states + `PLAN_ONLY`)
- [ ] stdout/stderr capture (no shell/command tool exists)
- [ ] Dependency / permission / unsupported-format / network recovery playbooks (§9) beyond file errors
- [ ] "Smart alternatives" (§48) and dependency graph (§49) as real features
- [ ] Recovery history shown to the user across tasks

### 5.2 AI provider layer (spec §3A, §35)
- [x] `AIProvider` interface; no Gemini logic in core
- [x] Gemini adapter: `listModels`, validate credentials, generate with tools — **works live**
- [x] API key in Keystore (AES-GCM), header not URL, redacted in logs/audit, removable, masked in UI
- [x] Test connection, list models, select model (UI)
- [x] Failure handling: rate limit (Retry-After), network, model unavailable, context too large, bad key, safety block
- [x] Fallback model support in `ResilientProvider` (core only)
- [~] Fallback model **not exposed in UI** (`AgentViewModel` builds `ResilientProvider(gemini, tracker = usage)` only)
- [~] Usage tracking: provider, model, tokens, latency, errors recorded in memory; **no cost estimate, no usage screen, lost on restart**
- [ ] Multiple providers / "Add provider / Edit provider" UI
- [ ] Per-task models (planning / simple / recovery / vision / coding)
- [ ] Model capability registry (tool calling, vision, context, streaming…) — only token limits in `ModelInfo`
- [ ] Automatic model routing + user switch to disable it
- [ ] Custom / compatible provider (base URL, API format)
- [ ] `stream()`, `generateStructured()` from the interface list in spec
- [ ] Tool-call format rejected → repair request → retry
- [ ] No internet → pause AI steps and resume on reconnect
- [ ] Privacy mode toggles (send names / contents / media / output)

### 5.3 Tools (spec §5, §6, §39)
File tools:
- [x] `file.list`, `file.search`, `file.stat`, `file.hash`, `file.read`
- [x] `file.copy`, `file.move`, `file.rename`, `file.mkdir`, `file.write`, `file.append`
- [x] `file.delete` (to restorable `.agent_trash`)
- [x] `agent.ask_user`
- [ ] `file.restore` as a tool (restore exists only through Undo)
- [ ] `file.compare`, `file.compress`, `file.extract`, `file.watch`, `file.permissions`

Other groups:
- [~] System: only `system.storage` (no free-space under SAF)
- [ ] System: `info`, `memory`, `cpu`, `battery`, `network`, `processes`, `environment`, `permissions`, `logs`
- [ ] Media: all 11 tools (`probe`, `convert`, `extract_audio`, `extract_frames`, `thumbnail`, `resize`, `compress`, `concat`, `split`, `normalize_audio`, `transcode`)
- [ ] Linux tools (`shell`, `process`, `package`, …)
- [ ] Development tools (`git.*`, `python.run`, `node.run`, `gradle.build`, …)
- [ ] Android tools (`apps`, `app_info`, `open_app`, `intent`, `settings`, `package_install/uninstall`, …)
- [ ] Root tools
- [x] Typed tool schema with risk level + argument validation (`ToolTypes.kt`)
- [ ] Tool plugin system (§38) and capability discovery tool (§40) — registry exists, no plugin loading, no "what is available" query beyond the prompt

### 5.4 Safety, policy, confirmation (spec §13–17, §46, §50–54)
- [x] PolicyEngine with risk levels; deletes always confirm, never "allow for task"
- [x] Protected paths denylist (`/system`, `/proc`, `/dev`, `/sys`, `/data/data`, `/data/system`, `/vendor`)
- [x] Sandbox: only granted folders; `..` and symlink escapes rejected
- [x] Confirmation dialog with risk, file count, bytes, "Always allow … for this task", cancel task
- [x] Dry run (UI switch)
- [x] Manual mode (UI switch): proposals only, `WAITING_FOR_USER`
- [~] Autonomous mode: `InteractionMode.AUTONOMOUS` exists in core; **no UI to pick capabilities**
- [x] Undo: move back, delete created, restore overwritten/deleted; persisted in Room; "Undo this task" button
- [x] Prompt-injection layers (`untrusted_data`, system prompt rule, confirmation shows real targets)
- [x] Audit trail (redacted, persisted in Room)
- [ ] Review-files screen for big deletes ("[Review Files]")
- [ ] Command preview (§47) — no commands exist yet
- [ ] Execution isolation / resource limits (§50, §54) beyond step counts

### 5.5 Android app (spec §58, §59)
- [x] Compose UI: key, model, folders, goal, dry run, manual, run, result, undo, history
- [x] SAF filesystem with write support
- [~] SAF limits: slow lookups in huge folders, no free space, folder moves need provider support
- [ ] Real chat interface (current UI is a single goal box + result, not a conversation)
- [ ] File browser screen
- [ ] Foreground service / notifications / pause button / progress ("71 / 156 files")
- [ ] Settings screen for memory, privacy, models, usage
- [ ] Connectivity awareness (offline mode §37)

### 5.6 Memory, history, logs (spec §32–34)
- [~] Task history: audit entries stored and listed; **not** a per-task view with goal/plan/tools/errors/recovery/files changed
- [ ] Agent memory (preferences: visible, editable, removable, scoped)
- [ ] Human-readable timestamped execution log + structured log export
- [ ] Environment learning (§64) and "I don't know" behaviour (§62) as tested features
- [ ] Explain-actions report beyond the engine's computed `report` (§63)

### 5.7 Later phases (spec §60)
- [ ] **Phase 2 — Media:** FFmpeg, probe, extract audio, convert, compress, batch
- [ ] **Phase 4 — Linux runtime:** userspace, shell, package manager, Python, Git
- [ ] **Phase 5 — Package intelligence:** dependency detection, install/repair, verification
- [ ] **Phase 6 — Android tools:** apps, intents, permissions, APK management, settings
- [ ] **Phase 7 — Root backend:** detection, typed root tools, root safety (§22–23)
- [ ] **Phase 8 — Advanced agent:** scheduling, autonomous mode, multi-step workflows, long-running tasks, checkpoints, plugins, extra providers, routing
- [ ] Spec demos: VOA example (§56), 7 "killer demo" tasks (§57), Python/APK/root examples (§42–45)

---

## 6. Repo hygiene / inconsistencies found

- [x] README status table and known limits now reflect Room history, SAF writes, and the 82-test baseline.
- [x] CI/release action references are pinned to immutable commit SHAs.
- [ ] README says "Written; device build/run reported by the owner" while ROADMAP says "compiles and runs on a real device" — pick one wording.
- [x] README no longer lists background execution/checkpoints as not started.
- [~] `libs.versions.toml` retains the generated-version note; Android dependency resolution still needs a GitHub/Android-SDK build.
- [ ] Tests use a custom harness (no JUnit); fine for now, but consider JUnit 5 so IDE/CI report per-test results.
- [ ] Add a live Gemini contract test (opt-in, needs key) so API-shape changes are caught.
- [x] Privacy risk is documented and protected by names-only default plus explicit content opt-in.

---

## 7. Suggested next order

1. **P0 #1 and #4** — verify the real run and measure request count on the phone
2. **P0 #5** — run GitHub CI/release with Android SDK and signing secrets; tag only after green verification
3. **P1 #10** — exact provider conversation/tool-step checkpoint restore
4. **P1 #9** — physical-device validation of service, notification actions, and WorkManager recovery
5. Then P2: archive tools → All-files access → media (FFmpeg) → more providers/routing
