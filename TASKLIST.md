# Device Agent — Task List (done vs. not done)

Compared: `README.md`, `ROADMAP.md`, `docs/SPEC.md` (identical to the "full idea v4" file) and the actual code in the zip.
Date of review: 2026-10-08. I read the code; I did **not** run the tests or the app.

**Legend**
- `[x]` Done and covered by tests (agent-core) or confirmed on a real device
- `[~]` Code exists but **not verified on device / not complete**
- `[ ]` Not started

---

## 0. Summary

| Area | State |
|---|---|
| Core agent loop, policy, verification, recovery, undo, Gemini | Done (80 tests, per README) |
| Android app: UI, Keystore, SAF, Room history | Written, compiles, runs on Redmi Note 8 Pro |
| Real run / confirmation / undo / trash on device | **Not verified yet (P0 #1)** |
| Background execution, checkpoints, privacy toggles, cost limits | Not started |
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
- [ ] **#9 Background execution** — foreground service + notification + WorkManager; no service in manifest, nothing in code
- [ ] **#10 Checkpoint and resume** — nothing persisted per step
- [~] **#11 Usage and cost limits** — usage is **tracked and shown** (`UsageTracker`, `usageSummary`), but there is **no per-task token/request budget with hard stop** (only `maxSteps = 40`)
- [ ] **#12 Privacy toggles** — no "names only vs contents" setting, no "what will be sent" preview. `file.read` can currently send any file in a granted folder to Gemini

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

- [ ] README "Status" table says **history in Room: not started**, and "Known limits" says **audit log and undo are in memory** — but ROADMAP #8 and the code (`RoomAuditLog.kt`, `RoomUndoStore`) say it is done. Update README.
- [ ] README says SAF write support and Room are "included" in one row and "not started" in another — same fix.
- [ ] `ci.yml` comment says "56-test suite"; README/ROADMAP say 80 tests. Update the comment.
- [ ] README says "Written; device build/run reported by the owner" while ROADMAP says "compiles and runs on a real device" — pick one wording.
- [ ] README "Not yet built" list still mentions "history in Room" under Not started in the table.
- [ ] `libs.versions.toml` header still says versions could not be resolved.
- [ ] Tests use a custom harness (no JUnit); fine for now, but consider JUnit 5 so IDE/CI report per-test results.
- [ ] Add a live Gemini contract test (opt-in, needs key) so API-shape changes are caught.
- [ ] Add a README note on **privacy risk of `file.read`** until task #12 ships (already listed under "Known limits" — keep until fixed).

---

## 7. Suggested next order

1. **P0 #1** — verify real run, confirmation, undo, trash on the phone; fix what breaks
2. **P0 #4** — measure real request count on device
3. **P1 #12** — privacy toggles (closes the `file.read` leak risk)
4. **P1 #11** — hard per-task token/request budget with `BLOCKED`
5. **P1 #9 → #10** — foreground service, then checkpoint/resume
6. README/CI doc fixes (section 6)
7. Then P2: archive tools → All-files access → media (FFmpeg) → more providers/routing
