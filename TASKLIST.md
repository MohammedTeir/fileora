# Fileora — Task List (done vs. not done)

Compared: `README.md`, `ROADMAP.md`, `docs/SPEC.md` (identical to the "full idea v4" file), current main, and the supplied UX archive.
Date of review: 2026-10-09. The core suite (100), app-helper tests (6), API 35 debug APK build, and Android lint all pass locally. Physical-device validation remains outstanding.

**Legend**
- `[x]` Done and covered by tests (agent-core) or confirmed on a real device
- `[~]` Code exists but **not verified on device / not complete**
- `[ ]` Not started

---

## 0. Summary

| Area | State |
|---|---|
| Core agent loop, policy, verification, recovery, undo, model routing, Gemini/OpenAI/Claude providers | Done in core (100 tests pass locally) |
| Android app: Home/Activity/Settings UX, approval bridge/notifications, Keystore, SAF, optional All Files Access, Room history | API 35 APK and lint pass; 6 helper tests pass. Existing SAF app runs on Redmi Note 8 Pro; new UX and All Files Access still need device validation |
| Real run / confirmation / undo / trash on device | **Not verified yet (P0 #1)** |
| Background execution | Implemented: foreground service, notification actions, cancel path, WorkManager recovery watchdog, progress/result UI integration; device verification remains |
| Checkpoints | Implemented: exact provider-neutral conversation and latest progress persist after tool-result turns; Resume/Discard implemented. Device validation remains |
| Privacy toggles, cost limits | Implemented in core/UI; device verification still pending |
| Media/FFmpeg | Core probe/convert/extract-audio contracts and JVM FFmpeg backend implemented; Android SAF local probe/MP4 remux/M4A extraction implemented; arbitrary FFmpeg processing and remaining media tools pending device validation |

Spec phases: **Phase 1 ≈ 90%**, **Phase 3 ≈ 70%** (no stdout/stderr because no shell tool exists yet), **Phases 2, 4, 5, 6, 7, 8: 0–10%**.

---

## 1. P0 — Finish the MVP (ROADMAP)

- [~] **#1 Verify real run on device** (dry run OFF): owner verified the real move, approval dialog and complete Undo flow on device; delete-to-trash still needs explicit verification before this acceptance criterion is fully complete
  - Dry run, real move, approval dialog and Undo: **verified by owner**. Delete-to-restorable-trash: **pending explicit device check**.
- [~] **#2 `PLAN_ONLY` status** — in code (`TaskStatus.PLAN_ONLY`, `determineStatus`), not run on device
- [~] **#3 Exact tool list in system prompt** — generated from `ToolRegistry`; not run on device
- [~] **#4 Reduce AI request count** (was 7 requests / 10.7K tokens, target ≤ 3) — prompt batching guidance plus `file.move_matching` for bulk moves; core test added; **real count on device not measured**
- [~] **#5 Green CI + signed APKs** — CI passed on `main`; Android/signing verification and release tag remain external checks. Version catalog and workflow action references are hardened in code.

## 2. P1 — Make it usable (ROADMAP)

- [~] **#6 `file.read`** — implemented with size cap, untrusted_data wrapping, tests; not run on device
- [x] **#7 `file.write` / `file.append` / `file.move_matching`** — core done (100 KiB cap for writes; bulk matching moves are confirmation-protected, verified, and undoable)
- [~] **Review Changes page** — per-file approve/reject for `file.move_matching` (2–500 files): the engine re-plans against an allow-list of approved files, so unticked or newly appeared files are never touched; kept files are reported as PARTIAL_SUCCESS. Core: 9 tests (mutation-checked). UI and the notification path **need device validation**; other batch tools still approve all-or-nothing
- [~] **Launch and onboarding** — splash (brand background, first-launch loading text), 3-page welcome with Skip/Get Started, setup & permissions intro (folder picker with cancelled state; notifications with denied and permanently-denied → Android Settings), optional AI intro, optional restore of AI choices from a settings file (keys, folders and the privacy switch are never exported or imported), setup-complete checklist; existing installs skip it. JVM-tested: settings codec and permission-state logic. Compose screens and the permission flows **need device validation** (denied / "don't ask again" paths especially)
- [x] **#8 Persistent history (Room)** — `RoomAuditLog`, `RoomUndoStore`, history list in UI, latest task undoable after restart
- [~] **#9 Background execution** — foreground `AgentTaskService` owns runs when the screen is off, shows progress/confirmation notifications, supports cancellation, and delivers progress/results to the UI. A delayed unique `AgentTaskRecoveryWorker` relaunches a checkpointed task after process death; device verification remains.
- [~] **#10 Checkpoint and resume** — foreground service persists task inputs, provider-neutral messages after each tool-result turn, and latest progress; after process death the app offers Resume or Discard. Core coverage passes; device validation remains.
- [~] **#11 Usage and cost limits** — per-task request, input-token, and output-token budgets now hard-stop with `BLOCKED`; actual task usage is returned in `TaskOutcome` and shown in `usageSummary`, with core regression coverage. Not yet verified on device; provider usage is unavailable when an adapter omits usage metadata.
- [~] **#12 Privacy toggles** — names-only is the default; the user can explicitly allow file contents, the task screen shows a "what will be sent" preview, and `file.read` enforces the setting. Core tests pass; device verification remains.

## 3. P2 — More capability (ROADMAP)

- [~] #13 Optional "All files access" mode — default-off `MANAGE_EXTERNAL_STORAGE` settings flow, permission checks, Android-reported shared-volume `JvmFileSystem`, `Android/data`/`Android/obb` denylist, scheduled-task persistence, and filesystem-matched undo implemented; physical-device validation remains. P0 real-run/confirmation/undo/trash checks are still outstanding.
- [~] #14 Archive tools — `file.compress` / `file.extract` implemented with ZIP entry/path/expanded-size limits, no-overwrite behavior, undo, and core round-trip/zip-slip tests; Android/SAF validation remains
- [~] #15 Media tools — core `media.probe` / `media.convert` / `media.extract_audio`, JVM FFmpeg backend, and SAF-local Android probe/MP4 remux/M4A extraction are implemented; full Android FFmpeg, remaining media tools, and device validation remain
- [~] #16 Search by content — `file.search_content` searches approved roots with binary skipping, bounded reads/results, short untrusted snippets, and names-only privacy blocking; Android/SAF validation remains
- [~] #17 Scheduled tasks — `ScheduledTasks.schedule/cancel/list`, Room persistence/migration, `ScheduledTaskWorker`, and Compose schedule/cancel controls implemented with manual-mode-by-default execution; device validation remains
- [~] #18 More providers — Gemini, OpenAI-compatible, and Claude adapters, provider-specific Keystore keys, Compose provider selection, foreground/scheduled wiring, and shared tests implemented; device/live-provider validation remains
- [~] #19 Model routing — default-off routing policy, user toggle, same-provider recovery-model selection, capability guard, foreground/resume/schedule wiring, and regression tests implemented; Android/device validation remains

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
- [~] Multiple providers / provider selection UI — Gemini, OpenAI-compatible, and Claude choices use separate Keystore entries; adding custom providers remains future work
- [ ] Per-task models (planning / simple / recovery / vision / coding)
- [ ] Model capability registry (tool calling, vision, context, streaming…) — only token limits in `ModelInfo`
- [~] Automatic model routing + user switch to disable it — same-provider recovery routing is implemented; richer capability metadata and device validation remain
- [ ] Custom / compatible provider (base URL, API format)
- [ ] `stream()`, `generateStructured()` from the interface list in spec
- [ ] Tool-call format rejected → repair request → retry
- [ ] No internet → pause AI steps and resume on reconnect
- [ ] Privacy mode toggles (send names / contents / media / output)

### 5.3 Tools (spec §5, §6, §39)
File tools:
- [x] `file.list`, `file.search`, `file.stat`, `file.hash`, `file.read`
- [x] `file.copy`, `file.move`, `file.move_matching`, `file.rename`, `file.mkdir`, `file.write`, `file.append`
- [x] `file.delete` (to restorable `.agent_trash`)
- [x] `agent.ask_user`
- [ ] `file.restore` as a tool (restore exists only through Undo)
- [ ] `file.compare`, `file.watch`, `file.permissions`

Other groups:
- [~] System: only `system.storage` (no free-space under SAF)
- [ ] System: `info`, `memory`, `cpu`, `battery`, `network`, `processes`, `environment`, `permissions`, `logs`
- [~] Media: `probe`, `convert`, and `extract_audio` are implemented; remaining 8 of 11 tools (`extract_frames`, `thumbnail`, `resize`, `compress`, `concat`, `split`, `normalize_audio`, `transcode`) are pending
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
- [x] Compose Home/Activity/Settings UI: providers/models, folders/storage, task entry, dry run/manual, results, undo, task history, repeating tasks
- [x] SAF filesystem with write support
- [~] SAF limits: slow lookups in huge folders, no free space, folder moves need provider support
- [ ] Real chat interface (current UI is a single goal box + result, not a conversation)
- [ ] File browser screen
- [~] Foreground service / approval and result notifications / stop / progress are implemented; physical-device behavior remains to validate
- [x] Settings screen for providers/models/API keys, privacy and storage modes; memory controls and usage dashboard remain unimplemented
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

- [x] README status table and known limits reflect Room history, SAF writes, bulk moves, and the 100-core/6-app-test baseline.
- [x] CI/release action references are pinned to immutable commit SHAs.
- [x] README and ROADMAP consistently state that the existing SAF build was reported running on a real device; the reworked UX and All Files Access flow still need validation.
- [x] README no longer lists background execution/checkpoints as not started.
- [~] `libs.versions.toml` retains the generated-version note; local API 35 Android build/lint now pass; GitHub CI for the UX commit remains to run.
- [~] Core tests use the custom harness; app UX-helper tests use JUnit 4. Consider moving the core suite to a standard test framework later.
- [ ] Add a live Gemini contract test (opt-in, needs key) so API-shape changes are caught.
- [x] Privacy risk is documented and protected by names-only default plus explicit content opt-in.

---

## 7. Suggested next order

1. **P0 #1 and #4** — verify the real run and measure request count on the phone
2. **P0 #5** — run GitHub CI/release with Android SDK and signing secrets; tag only after green verification
3. **P1 #10** — exact provider conversation/tool-step checkpoint restore
4. **P1 #9** — physical-device validation of service, notification actions, and WorkManager recovery
5. Then remaining P2 validation/implementation: validate #13 on-device, including the still-pending P0 real-run/confirmation/undo/trash gate, before relying on broad access; #15 full Android FFmpeg/AAR build, eight remaining media tools, and device validation. #19 is implemented; validate routing on Android/device.
