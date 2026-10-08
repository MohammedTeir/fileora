# Device Agent

An Android AI agent that operates the device through **typed, policy-checked, verified tools**. You describe a goal; the agent inspects, plans, acts, checks the real result, and recovers from failures within strict limits. Cloud AI only (Gemini first, bring-your-own API key). Spec: [`docs/SPEC.md`](docs/SPEC.md).

## Status: what is real and what is not

| Part | State |
|---|---|
| `agent-core` (loop, tools, policy, verification, recovery, undo, Gemini adapter) | **Built and tested**: 89 tests pass; 8 deliberate bugs were injected and all were caught |
| Gemini adapter against the **live** API | **Works live** (reported in `ROADMAP.md`: model `gemini-3.5-flash-lite`, 0 errors). Unit tests still use a fake transport shaped from the public docs, so there is no automated live-API test yet |
| `app` module (Compose UI, Keystore, SAF filesystem, Room audit/undo history) | **Compiles and runs on a real device** (Redmi Note 8 Pro; reported by the owner). Includes SAF write support, redacted audit history persisted in Room, restart-safe undo storage in Room, and task request/token usage reporting. **Dry run is verified on device; a real run, the confirmation screen, undo and delete-to-trash are not yet verified on device** (`ROADMAP.md` P0 #1) |
| Gradle build files / dependency versions | **Configured**: Android/WorkManager dependencies are declared; full Android compilation still needs GitHub CI or an Android SDK |
| GitHub Actions (`ci.yml`, `release.yml`) | **CI is green and the release workflow builds signed APKs** (reported by the owner; `apksigner verify` output not yet seen). See [`docs/RELEASING.md`](docs/RELEASING.md) for the signing-key and secrets setup |
| Media (FFmpeg), Linux runtime, packages, root, scheduling | Media probe/convert contracts and a JVM FFmpeg backend are implemented; Android native/SAF media integration remains |

## What the MVP does (spec phases 1 and 3)

Tools: `file.list`, `file.search`, `file.search_content`, `file.stat`, `file.hash`, `file.read`, `file.write`, `file.append`, `file.mkdir`, `file.copy`, `file.move`, `file.rename`, `file.delete` (to a restorable trash), `file.compress`, `file.extract`, `media.probe`, `media.convert`, `system.storage`, plus `agent.ask_user`.

The loop for every model-proposed call:

```
validate arguments → plan (no side effects) → policy → [user confirmation] → execute
  → verify the REAL state → on failure: diagnose, remember, bound the retries → back to the model
```

Guarantees, each covered by a test:

- **The AI proposes, the engine authorizes.** The model never touches storage directly.
- **Sandbox.** Only user-granted folders; `..` and symlink escapes are rejected.
- **Verification is mandatory.** A copy that "succeeds" but leaves an empty file is caught, rolled back and reported.
- **Honest final state.** `SUCCESS`, `PLAN_ONLY` (dry run: nothing changed), `SUCCESS_AFTER_RECOVERY`, `PARTIAL_SUCCESS`, `WAITING_FOR_*`, `BLOCKED`, `UNSAFE_TO_CONTINUE`, `FAILED_AFTER_INVESTIGATION`, `CANCELLED` are computed from what actually happened, not from what the model says ("All done!" with one failed item is `PARTIAL_SUCCESS`).
- **Bounded recovery.** At most 3 retries per item; an identical failing call is refused unless something changed; step, failure and file limits per task.
- **Risk-based confirmation.** Reads and low-risk copies run; moves ask; deletes always ask and can never be "allowed for the whole task"; protected locations are refused outright.
- **Dry run and manual mode.** Plan the whole job, change nothing.
- **Undo.** Each task's changes can be reverted; deletes go to a trash.
- **Prompt-injection defense, in layers.** Device data is quarantined under `untrusted_data`, the system prompt says to treat it as data, and destructive actions need human approval that shows the real target. This reduces risk; it does not make a manipulated model harmless (it could still propose harmless-looking but unwanted moves).
- **Secrets.** API key in Android Keystore (AES-GCM), sent in a header (never the URL), redacted from logs, the audit trail and tool results.
- **AI-API failure handling.** Rate limit (honours Retry-After), network, model-unavailable (optional fallback model), context-too-large (history shrunk once). Bad key and safety blocks are not retried.

## Layout

```
agent-core/   pure Kotlin/JVM, no Android imports (enforced by review; checked by grep)
  agent/      AgentEngine, statuses, system prompt
  tools/      typed tools + registry + argument validation
  policy/     PolicyEngine, risk levels, confirmation types
  verify/     post-action verifiers
  recovery/   ErrorAnalyzer (diagnosis + suggested next steps)
  ai/         AIProvider interface, ResilientProvider, gemini/ adapter
  fs/         FileSystem interface + JvmFileSystem (sandboxed)
  security/   Redactor, SecretStore
  history/    AuditLog, UndoManager
app/          Android shell: Compose UI, KeystoreSecretStore, SafFileSystem
scripts/      test-core.sh (offline build+test), kotlin-env.sh
```

## Run the tests

```bash
./scripts/test-core.sh        # no network needed; uses the Kotlin compiler bundled in Gradle's lib dir
gradle :agent-core:check      # same suite via Gradle, once dependencies can be resolved
```

## CI and releases

- `ci.yml`: tests + debug APKs on every push/PR (no secrets).
- `release.yml`: push a tag `vX.Y.Z` to build **signed** APKs for arm64-v8a, armeabi-v7a, x86, x86_64 + universal and publish a GitHub Release with checksums. One-time key setup: [`docs/RELEASING.md`](docs/RELEASING.md).

## First steps on your machine

1. Open the project in Android Studio; let it sync; bump versions in `gradle/libs.versions.toml` and fix any compile errors in `app/` (start with `SafFileSystem.kt`, the largest untested piece).
2. Run the app, add your Gemini API key, tap **List models**, pick a model, add one folder, and try a **dry run** first.
3. Verify the Gemini adapter on a real key before trusting it (a failure there will show up as a clear provider error, not a crash).

## Known limits and decisions to make

- **Privacy default:** file contents are blocked from cloud responses by default. The user can explicitly opt in to sending text requested through `file.read`; the task screen shows a "what will be sent" preview.
- **Result size.** `file.read` defaults to 8,000 bytes (hard cap 100 KB), but the engine cuts any tool result above `maxResultChars` (12,000), so larger reads reach the model as a truncated preview.

- **Distribution.** Play Store vs direct APK decides how far Linux/root can go (spec phases 4 and 7). The MVP needs neither.
- **SAF limits.** No free-space query (reported as "unknown"), linear folder lookups (slow in huge folders), folder moves need provider support.
- **Media/FFmpeg status:** the core has tested `media.probe` and `media.convert` contracts plus a no-shell JVM `JvmFfmpegBackend` with playable-output verification. The Android/SAF native FFmpeg backend, ABI packaging, and the remaining media tools are not yet built or device-verified.
- **Not yet built:** model routing and the Android/SAF media backend. Background execution and task checkpoints are implemented, but still need physical-device validation.
- **Usage limits:** each task now has hard request, input-token, and output-token budgets; reaching one stops the task with `BLOCKED`. Provider-reported usage is shown for the current task; usage history is still in memory only.
- **History is persisted in Room** (`device-agent.db`, `RoomAuditLog.kt`): the redacted audit log and the undo actions (move, delete-created, restore) survive an app restart, and the latest persisted task is offered for undo after restart. Limits: the database is not encrypted beyond Android's app-private storage, undo rows for `file.write`/`file.append` hold the previous file contents (up to 100 KiB each), and AI usage totals (`UsageTracker`) are still in memory only.
- **Checkpointing:** an interrupted foreground-service task persists its request, exact provider-neutral conversation, and latest progress, then offers Resume or Discard on the next app launch. Physical-device validation remains.
- **Scheduled tasks:** `ScheduledTasks.schedule` stores periodic WorkManager input and relaunches the foreground service. Scheduled runs default to manual mode, so they produce proposals instead of silently changing files; a schedule-management UI and device validation remain.
- **Exact conversation checkpoints:** the core now persists provider-neutral messages after each tool-result turn and the Android service restores them after process death. Device validation remains required.
- **WorkManager recovery:** a unique delayed watchdog relaunches a checkpointed task only when the foreground service is no longer alive, preventing duplicate execution during normal runs. Android background-start behavior still needs validation on a physical device.
