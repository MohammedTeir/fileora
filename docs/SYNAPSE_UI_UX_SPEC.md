# Synapse — complete UI/UX specification

Native Android · Jetpack Compose · Material 3 · Cloud AI only

Design basis: the supplied Android AI Device Agent v4 specification and the existing Android project. This is a product design and implementation handoff, not a claim that the proposed runtimes, root tools, or device integrations already exist.

## 1. Product assumptions and design direction

- Synapse is the official visible name. Use it in app bars, onboarding, notifications, dialogs, exports, and accessibility descriptions. A display-name redesign does not require changing the existing application ID, database names, or credential aliases.
- Phones are the primary surface: design at 412 × 915 dp, verify at 360 dp width, and support landscape, tablets, foldables, RTL, and 200% font scaling. Measurements below use Android dp/sp, not literal display pixels.
- Gemini is the first provider. Model names, capabilities, prices, and availability come from provider metadata when available. The example model aliases below are fictional fixture labels, not a current model catalog.
- AI planning and diagnosis run in the cloud. Approved local operations, local verification, file browsing, and stored history can work offline. No local AI fallback is implied.
- The only retained artwork from the repository's assets archive is the existing launcher icon. UI icons use Material vectors; illustrations use independently authored geometry or typography. The current launcher may appear in the launch screen; no new branding artwork is derived from the archive.
- Present concise action explanations, observations, policy decisions, and verification evidence. Never design an interface for exposing private model chain-of-thought. The expandable label is “Why this action?” or “Tool details,” not “Internal thoughts.”
- Default mode is Normal. Root is off. File contents and media sharing are off. Destructive and privileged operations require operation-specific consent. Autonomous mode only grants explicitly chosen scopes.
- All examples in this document are demo fixtures. A prototype has a persistent “Demo data” badge and a scenario switcher outside the product layout. Mock buttons change demo state only. Production screens must never present fixture values as device measurements, real execution, or verified security status.

Visual personality: precise, calm, and active. Neutral surfaces, cobalt actions, generous grouping, restrained motion, and strong typography. Favor readable progress and meaningful status over decorative graphs.

## 2. Information architecture and navigation

Four primary destinations:

| Destination | Primary purpose | Child routes |
|---|---|---|
| Agent | Ask, inspect a proposal, and act | Chat, attachments, plan, approval, recovery |
| Activity | Understand ongoing and completed work | Active tasks, checkpoints, history, audit, undo, schedules |
| Device | Inspect and manage capabilities | Overview, granted folders, Linux, packages, terminal, Android apps, tool catalog |
| Settings | Configure AI, privacy, and policy | Providers, credentials, models, usage, security, memory, appearance |

Use a Material 3 NavigationBar on compact widths and NavigationRail on expanded widths. Agent is the initial destination. A compact “2 tasks running · 1 needs attention” strip appears above navigation when relevant; it opens Activity. Badges count required decisions, not every log event.

Each task keeps one identity across chat, plan, execution, recovery, and audit. Back returns to the caller with scroll position, draft text, and selected item preserved. Notifications deep-link into the exact task; an expired approval opens its recorded result rather than a reusable approval control. Nested editing screens use a back/up app bar. Leaving unsaved edits offers Save draft or Discard. System Back never cancels an executing job.

At widths below 600 dp, use one column. At 600–839 dp, use a rail where practical and cap reading width at 720 dp. At 840 dp and above, use list-detail panes for Activity, Packages, and Memory; keep chat and confirmation text readable rather than stretching edge to edge.

Primary journey:

    Ask → Inspect → Plan preview → Scope/approval → Execute → Verify → Result
                                      ↑               ↓
                                      └── Recovery ← Failure

Configuration, diagnosis, or permission changes return the user to the interrupted task. Any changed plan is re-evaluated before execution.

## 3. Design system

### Typography, space, and shape

Use Android's system sans-serif family; use system monospace only for paths, versions, commands, and raw payloads. Never rely on a downloaded font for startup.

| Token | Size / line height | Use |
|---|---|---|
| headlineLarge | 32 / 40 sp, medium | Welcome and major result |
| headlineMedium | 28 / 36 sp, medium | Dashboard summary |
| titleLarge | 22 / 28 sp, medium | Screen and panel titles |
| titleMedium | 16 / 24 sp, semibold | Card headings and task names |
| bodyLarge | 16 / 24 sp | Chat, explanation, editable text |
| bodyMedium | 14 / 20 sp | Metadata and secondary descriptions |
| labelLarge | 14 / 20 sp, medium | Buttons and status labels |
| labelSmall | 12 / 16 sp, medium | Timestamps and secondary chips |
| code | 13 / 20 sp, monospace | Expandable terminal and tool details |

Spacing tokens: 4, 8, 12, 16, 24, 32, 48 dp. Phone gutters 16 dp; expanded gutters 24–32 dp. Card padding 16 dp; section gap 24 dp; related rows 8–12 dp. Minimum interactive target 48 × 48 dp. List rows have a 64 dp minimum but grow with text. Critical descriptions never truncate. Show a shortened path with a full-path detail action, preserving filename and target location.

Shapes: 8 dp badges, 12 dp fields, 16 dp ordinary cards, 24 dp prominent task cards and bottom-sheet corners. Use 1 dp outlines and 0–2 dp elevation for content; 6 dp for modal surfaces. Avoid stacking many elevated cards.

### Material and semantic colors

Use fixed Synapse colors by default; offer dynamic color as an explicit appearance preference. Status and risk semantics remain consistent. Validate final text/background contrast in both themes; the pairs below are the design starting tokens.

| Role | Light | Dark |
|---|---|---|
| primary / onPrimary | #3157D5 / #FFFFFF | #B9C5FF / #102878 |
| primaryContainer / onPrimaryContainer | #E0E6FF / #132B7A | #253E91 / #E0E6FF |
| background / onBackground | #F7F8FC / #191C25 | #111318 / #E4E6EF |
| surfaceContainerLow | #FFFFFF | #191C23 |
| surfaceContainerHigh | #E9ECF5 | #272B35 |
| onSurfaceVariant | #535B6E | #BDC4D4 |
| outlineVariant | #D9DDE8 | #404653 |
| errorContainer / onErrorContainer | #FFDAD6 / #410002 | #73332D / #FFDAD6 |
| successContainer / onSuccessContainer | #D9F3E4 / #145235 | #173D2B / #B9ECCD |
| warningContainer / onWarningContainer | #FFF0CC / #654900 | #49370D / #FFE2A0 |
| privilegedContainer / onPrivilegedContainer | #EDE1FF / #4B2570 | #43265B / #EDDCFF |

Error uses native Material 3 color roles. Success, warning, and privileged colors are semantic extensions exposed through a SynapseStatusColors composition local; Material 3 does not define built-in success/warning roles. Recovery uses warning while attention is needed, primary while a fix is executing, and success only after verification.

### Risk semantics

Risk and execution state are separate chips: “L1 · Low impact” can appear beside “Verification failed.” Never color an operation safe merely because its process exited successfully.

| Risk | Label and icon | Treatment | Default authorization |
|---|---|---|---|
| L0 | Read · Visibility | Neutral surface, outlined chip | Within already granted scope |
| L1 | Low impact · AddCircle | Primary container | Within an approved plan; scoped automation may allow |
| L2 | Moderate · DriveFileMove | Warning container, outline | Confirm scope unless explicitly covered by a rule |
| L3 | Destructive · DeleteOutline | Error container, clear affected count | Always review and confirm the specific operation |
| L4 | Privileged · AdminPanelSettings | Privileged container, lock marker | Backend opt-in plus operation-specific approval |
| L5 | Critical · Report | Error surface, heavy outline, “Blocked” label | Block by default; no general override in the proposed UI |

Classification depends on target, scale, overwrite behavior, origin, and reversibility. A copy that overwrites a file is not automatically L1. A user-space package install can be L1 only within an allowed isolated runtime and verified source; other effects may raise its classification. Policy defines the result, not the icon or AI model.

### Motion and accessibility

Use 120–180 ms pressed/state feedback, 200–250 ms navigation and expansion, and short fades for timeline insertion. Animate numeric changes without shifting adjacent labels. Respect system animation settings and reduce-motion preferences; never use flashing risk badges or endless decorative pulsing. Show indeterminate progress only when a denominator is unknown.

Use icon + text + semantic color for every state. Announce important stage changes and approval requests through TalkBack, not every streaming token. Expose progress, counts, and button state in semantics. Decorative icons have null content descriptions; icon-only controls have action labels. Keep sticky controls clear of IME and navigation insets. In RTL, mirror navigation and directional actions; isolate paths, IDs, and terminal output in LTR. Support switch access, keyboard navigation, and visible focus. Allow long labels and 200% font scaling without hiding approval targets.

## 4. Shared demo fixtures

All versions, package sizes, prices, task times, and measurements below are illustrative. Keep these fixture identities and counts consistent in every preview.

### Device snapshot D1

- Android 15, ARM64, 8 GiB RAM, 256 GiB storage.
- Storage now: 48.2 GiB free, 207.8 GiB used. Task A began with 64.0 GiB free; it has produced 15.2 GiB and holds 0.6 GiB of temporary data.
- Synapse process: 612 MiB memory, 42% of one CPU core in the sample interval. Device-wide CPU is “Unavailable with current access,” not inferred from the app metric.
- Battery 68%, charging; battery temperature 37.8 °C; Android thermal status Moderate. Battery temperature is not labeled CPU temperature.
- Root binary detected; backend disabled. Linux userspace installed; 1.84 GiB used. Last capability scan 2 minutes ago.
- Granted folders: Downloads, Movies/Trips, Music/Converted. Display labels map to persisted Android document grants; they are not assumed Unix paths.

### Task A: SYN-2048 — Make travel videos smaller

- Goal: “Make these 500 travel videos smaller. Keep originals and put the results in Compressed.”
- Source label Movies/Trips; destination Movies/Trips/Compressed. Candidate set: 500 files, 87 GiB. Estimated final output 26 GiB. H.264 video, AAC audio, 720p ceiling, no upscale, preserve originals, skip existing outputs only after verification.
- Current snapshot: 320 verified, 4 intentionally skipped, 2 blocked, 1 running, 173 queued = 500. Headline is “320 of 500 verified · 64%,” not “64% successful task.”
- Current item 327: Lisbon_0327.mp4. Elapsed 42m 18s; estimated remaining 24–31m. Checkpoint revision 326 saved 8 seconds ago; 326 items have recorded terminal outcomes (including blocked/skipped).
- Earlier recovery: Porto_0087.mp4 used AAC stream copy after the preferred audio conversion failed; output duration and playback probe passed.
- Sample live events: 14:31:02 destination grant checked; 14:31:03 item 327 probed; 14:31:04 conversion started; 14:31:12 temporary output 34 MiB; 14:31:18 checkpoint metadata persisted for completed items.

### Task B: SYN-2049 — Extract lesson audio

- 156 inputs; 0 verified. Waiting for a recovery decision on Lesson_01_Speaking.mp4.
- Observed error: Unknown encoder 'libmp3lame'; exit code 1. Source readable; destination writable; no complete output created.
- Proposed fix: install a verified MP3-enabled FFmpeg package profile in Linux userspace. Illustrative download 38.4 MiB; additional installed footprint 112 MiB; no root. Attempt 1 of 3.
- Alternative: use available AAC output, which changes the requested format and therefore requires user approval. A later success preview is a separate fixture snapshot, not an increment applied to the waiting screen.

### Task C: SYN-2050 — Organize Downloads, dry run

- 2,481 files: 812 images, 203 videos, 631 documents, 127 archives, 184 APKs, 524 other.
- Proposed disjoint groups: move 604 files (184 APKs, 322 images, 98 videos), send 27 duplicate candidates to recoverable trash, compress 14 source archives, leave 1,836 untouched. Estimated recoverable space 1.2 GiB after trash is eventually emptied, not immediately guaranteed.
- No writes have occurred. Deletion candidates show matching hashes and retained copies. Destructive authorization is separate from approval for moves/compression.

## 5. Screen specifications

State notation in each screen: Loading, Empty, Error, and Success/ready are separate preview variants. Never fill a production empty or error screen with mock values.

### 01 · Welcome, onboarding, and first-run permissions

**Layout and hierarchy:** Synapse wordmark, one-line promise “Ask. Act. Verify.”, then a four-step progress indicator: AI provider, allowed folders, safety preferences, ready. Use a large headline and one decision per page. The launcher may appear at first launch; other illustrations use Material symbols. Keep “Explore demo” available before credentials are entered.

**Mock content:** Gemini configured; “Fast planner” selected; Downloads granted; “Normal · ask before changes”; file contents/media sharing off. Final summary reads “Synapse can inspect Downloads and propose a plan.”

**Interaction and components:** HorizontalPager or explicit routes, StepProgress, ProviderCard, FolderGrantRow, privacy switches, sticky Continue. Request Android access only after a user chooses the relevant capability. Explain the system picker handoff and return destination. Notifications have their own purpose explanation and optional deferral. Do not require root or Linux installation to finish setup.

**States:** Loading restores setup progress; Empty means no provider/folder yet and offers demo or local browsing; Error keeps completed steps and identifies the failed credential/grant; Success opens Agent with task suggestions. This sequence introduces capabilities without a wall of permissions.

### 02 · Agent chat and operational dashboard

**Layout and hierarchy:** Top app bar “Synapse” with a small “Gemini · Fast planner” model chip and mode pill. Below: an attention card when action is required, then a compact active-task summary. The conversation is the main scrolling surface. Bottom composer has a multiline goal field, attach control, optional voice input, mode affordance, and send/stop-generation control.

**Mock content:** User asks Task A. Synapse replies “I found 500 videos. I’ll keep the originals and verify each compressed copy.” A plan card shows 87 GiB input, estimated 26 GiB output, L1, and “Review plan.” An executing variant shows 320/500 verified and current item 327. Suggested goals: organize Downloads, inspect storage, extract lesson audio, inspect a failed command.

**Interaction and components:** SynapseTopBar, ModelChip, ModeSelector, ChatMessage, TaskSummaryCard, ApprovalCard, AgentComposer. Streaming summaries group under the task rather than creating one bubble per tool event. Stop generation stops the provider response; Pause task affects execution and has a separate label. Tapping a phase opens execution evidence.

**States:** Loading streams a bounded response placeholder; Empty offers examples and current accessible scope; Error distinguishes provider failure from tool failure with a contextual retry/diagnosis action; Success displays verified outcomes and Open results / View audit / Undo when available. Chat remains approachable while task detail is one tap away.

### 03 · Attachment picker and outbound-data review

**Layout:** Attachment bottom sheet with Files, Folder, Image, and Camera when supported. Selection becomes readable chips above the composer. “What will be shared?” opens a full-height review showing Local processing and Sent to provider sections.

**Mock content:** Movies/Trips, 500 local videos; sample image itinerary.jpg, 2.3 MiB. Outbound preview: user request, 500 filenames, codec summaries; video bytes excluded. Selecting the image does not upload it until send/consent.

**Components and behavior:** DocumentPickerLauncher, SelectedItemChip, CloudPayloadPreview, per-attachment remove, redaction preview, Send with selected data. Show provider identity and effective privacy settings. A folder label represents a document grant, not a promise of recursive filesystem access. If the selected model lacks vision, offer an eligible model or remove the image.

**States:** Loading reads metadata; Empty shows choose source; Error explains revoked access, unsupported modality, or size limit; Success shows final count and exact outbound categories. Preserve the draft on cancellation or denial.

### 04 · Interactive dry run and editable plan

**Layout:** App bar “Review plan,” prominent “Dry run · no changes made,” goal, four impact tiles, then step cards. Sticky footer: Approve & Run, Edit Plan, Cancel. Below the action label show the approved scope and highest risk.

**Mock content:** Task C: 604 moves, 27 proposed trash operations, 14 archive jobs, 1,836 unchanged. Step 1 inspect and hash (L0); step 2 create folders (L1); step 3 move selected files (L2); step 4 trash approved duplicates (L3); step 5 verify destinations and retained copies.

**Components and behavior:** ImpactSummary, PlanStepAccordion, RiskChip, file selection sheet, before/after path rows, storage estimate, conflict policy selector. Default each step to a one-line action and count; expand into affected files and typed tool calls. Edit destination, exclusions, and output quality through forms or natural-language revisions. Approve & Run opens destructive review if any L3+ scope remains; it cannot silently authorize it.

**States:** Loading inspects with no writes; Empty reports no matching work and allows editing; Error identifies missing grant, inadequate storage, or stale plan; Success offers an executable reviewed plan. Any material target/parameter change invalidates affected approvals and shows the delta before reapproval.

### 05 · Scoped approval and destructive-action review

**Layout:** Low-risk requests are compact inline cards. Destructive/privileged actions use a dedicated screen: consequence heading, exact targets and count, reversibility, exclusions, risk, policy basis, then confirmation controls. Cancel remains clear and accessible.

**Mock content:** Low risk: “Create 500 compressed copies in Movies/Trips/Compressed · originals retained.” Destructive: “Move 27 duplicate files to trash · 1.2 GiB · retained copies verified.” Show three sample filenames with “Review all 27.” An irreversible variant states that no restore is available.

**Components and behavior:** ScopeSummary, reversible/irreversible banner, selectable TargetReviewList, ConfirmActionBar. Offer Allow once and Allow this approved task only where policy permits. Delete/root never offer blanket future consent. Require explicit acknowledgement for irreversible scope; avoid long-press-only confirmation. Label the final action “Move 27 files to trash” rather than “OK.” A root approval includes the exact typed operation and target.

**States:** Loading resolves target metadata; Empty means targets no longer exist and approval cannot execute; Error shows changed/expired scope or policy denial; Success locks the card with approver/time and opens progress. Approval is bound to the current plan version and rechecked just before execution.

### 06 · Live execution monitor

**Layout:** Goal and current phase first, then verified progress, outcome counts, current-item card, collapsible timeline, and sticky Pause / Cancel controls. Expanded width adds a right-hand evidence pane. A phase rail shows Inspect, Plan, Policy, Execute, Observe, Verify; Recovery appears when needed.

**Mock content:** Task A, “320 of 500 verified · 64%”; 4 skipped, 2 blocked, 1 running, 173 queued. Current item Lisbon_0327.mp4. Remaining estimate 24–31m, output 15.2 GiB. Timeline includes source probe, destination grant, tool start, output written, verification, and checkpoint events.

**Components and behavior:** StageRail, SegmentedTaskProgress, OutcomeCountRow, CurrentItemCard, EventAccordion, persistent-control bar. Distinguish bytes processed from verified item progress. Group repetitive successes; keep errors and approval requests visible. Auto-follow only while the user is at the bottom, with a “12 new events” jump chip otherwise.

**States:** Loading reconnects to persisted task state; Empty shows no active work and links to history; Error preserves progress and opens Recovery; Success becomes a verified summary only when result checks finish. Exit code 0 alone never produces the green result card.

### 07 · Live diagnosis and adaptive recovery

**Layout:** Warning header “Audio conversion needs attention,” task/item context, then four vertical cards: Observed error → Diagnosis → Proposed fix → Safety check. Raw stderr and inspection evidence are collapsed. A comparison section shows alternatives. Footer actions: Approve Fix & Retry, Use Alternative Tool, Ask User Input, Abort.

**Mock content:** Task B, missing libmp3lame. Evidence: input readable, output folder writable, encoder absent, no complete output. Fix: MP3-enabled FFmpeg profile; 38.4 MiB download, +112 MiB installed, L1 within the selected runtime, source verification required. “Attempt 1 of 3; this fix has not been tried.” Alternative: AAC via available tools, explicitly labeled “Changes output format.”

**Components and behavior:** DiagnosisStageCard, EvidenceList, StrategyComparison, SafetyDecisionCard, RecoveryAttemptList. State the basis for the diagnosis without inventing confidence percentages. Ask User Input opens a concrete prompt about format, destination, or missing permission. Abort offers current-task or affected-item scope where safe. Applying a fix does not automatically authorize broader file or root access.

**States:** Loading gathers diagnostics; Empty evidence says “Cause not identified” and requests a precise next input; Error records the failed fix and next distinct strategy; Success says “Retry verified” and returns to the task. After three attempts, stop automatic retries and preserve the diagnosis; do not loop the same strategy without new evidence.

### 08 · Checkpoints and long-running work

**Layout:** Large progress card, current item, outcome breakdown, checkpoint timeline, resource condition strip, and Pause / Resume from Checkpoint / Cancel & Rollback Transaction controls. State-specific buttons replace one another rather than showing contradictory actions.

**Mock content:** Task A at item 327; checkpoint revision 326 saved 8s ago; 320 verified, 4 skipped, 2 blocked. Battery 68% charging, thermal Moderate, 48.2 GiB free. “Pause after current safe step” explains that an item may finish before the job pauses.

**Components and behavior:** CheckpointCard, ResourceConditionChips, ItemQueue, ResumeReview, rollback entry. Resume rechecks grants, inputs, outputs, model access, and available space; verified outputs are reused only after reconciliation. A crash during item 327 produces “Checking partial output” before restart. No partial media file is presented as completed. Thermal/resource policies may pause at a safe boundary; show their actual source and availability.

**States:** Loading reconciles checkpoint and filesystem; Empty says no saved checkpoint exists; Error shows revoked destination or changed inputs with repair choices; Success confirms resumed execution or verified completion. Foreground-service and Android scheduling constraints remain visible; do not promise execution will survive every system stop.

### 09 · Activity history and search

**Layout:** Active / History / Scheduled tabs, search field, compact status/date filters, then date-grouped task rows. Active rows show progress; completed rows show verification outcome and duration. Search matches goals, filenames, tools, and task IDs.

**Mock content:** Task A running; Task B waiting for recovery; “Organize receipts” verified, 84 files; “Install Python” verified after recovery; “Build notes app” failed after investigation; “Clean old exports” cancelled with 12 changes retained. Each row has the actual outcome, not a generic checkmark.

**Components and behavior:** SearchBar, FilterChipRow, TaskHistoryRow, date headers, selection actions. Tap opens the audit summary; overflow offers Rename label, Export redacted log, and Delete history. Deleting a history record never implies undoing file operations; state effects on restore metadata before removal.

**States:** Loading uses rows without fake counts; Empty distinguishes no history from no matches; Error offers retry while retaining cached rows labeled with freshness; Success shows searchable results with clear partial, blocked, cancelled, and recovered states.

### 10 · Task detail, audit, and verification report

**Layout:** Outcome hero, original goal, key totals, and tabs Summary / Changes / Timeline / Tools. Pin one relevant action such as Open outputs or Resume. Secondary actions include Export and Undo when supported.

**Mock content:** A completed preview of Task A: 494 verified, 4 skipped, 2 failed = 500; final state Partial success. Six earlier items succeeded after recovery. Verification examples: file exists, nonzero bytes, expected video/audio streams, duration within tolerance, decodable output. Show failures and exact paths together.

**Components and behavior:** ResultSummary, ChangeDiffList, grouped AuditTimeline, ToolInvocationPanel, VerificationChecklist. Timeline entries include timestamp, tool ID, redacted arguments, policy result, duration, exit status, observed result, and verifier output. Export uses redaction preview and preserves timestamps/timezone. “Succeeded after recovery” appears only for successful tasks with recovery, not this partial outcome.

**States:** Loading reads local history; Empty says record contains no execution and may be plan-only; Error distinguishes incomplete/corrupt evidence from a failed tool; Success shows verified evidence. Missing evidence displays “Not verified,” never an inferred pass.

### 11 · Undo, trash, and rollback review

**Layout:** Transaction title, reversibility summary, affected-file groups, conflicts, and a clear action footer. Separate Cancel execution from Undo changes already made.

**Mock content:** Task C rollback: 604 moved files restorable, 27 trash entries available, 14 archive outputs removable if unmodified. Example conflict: Downloads/Images/ticket.png changed after the task; offer Keep current, Restore to another name, or review overwrite. Trash shows a stated retention period only if enforced by the selected backend.

**Components and behavior:** ReversibilityBadge, RestoreItemList, conflict-resolution sheet, RollbackProgress. Revalidate files and record rollback as a new audited transaction. Show “621 of 631 operations reversible” in a mixed-support fixture, not a generic Undo available flag. Never promise recovery for permanent deletion or system changes.

**States:** Loading checks preconditions; Empty means nothing reversible remains; Error preserves completed restores and lists unresolved conflicts; Success reports verified restores and any exceptions. Backing out does not apply partial unconfirmed choices.

### 12 · AI provider hub

**Layout:** Default-provider card, configured-provider list, Add provider action, then links to Models, Privacy, and Usage. Every row shows connection status and last checked time.

**Mock content:** Gemini default, credential stored, test succeeded 8m ago, “Fast planner” active. OpenAI-compatible demo provider configured but not selected. Another provider row shows “Needs credentials.” Use neutral provider text/vector marks rather than copied logos.

**Components and behavior:** ProviderCard, ConnectionStatusChip, DefaultProviderSelector, AddProviderSheet. Add/edit requires adapter type, display name, optional supported endpoint, and credential entry. Custom endpoints show the destination host and HTTPS validation; do not offer to bypass certificate verification. Provider changes affect new tasks; changing an active task opens a scoped review of privacy and capability implications.

**States:** Loading restores saved configurations; Empty offers Set up Gemini and Explore demo; Error separates invalid credentials, rate limiting, network outage, and incompatible API; Success shows test method and timestamp. A stored key is not labeled “Connected” until a successful test.

### 13 · Secure credential manager and connection test

**Layout:** Provider and endpoint header, masked credential status, Replace key / Remove key controls, Test connection button, concise privacy explanation. Initial entry uses a password field and explicit paste visibility during entry only.

**Mock content:** “Key stored securely · ending 7F2A”; “Last tested 8 minutes ago · model listing available.” Test result details distinguish authentication, model availability, and tool-calling support; a single API success does not prove all three.

**Components and behavior:** CredentialStatusCard, SecretEntryField, TestProgressSteps, RedactedErrorCard. Never reveal the full saved key, include it in preview fixtures, or place it in logs. Removing a provider key explains which pending AI-dependent tasks will pause. Keep local task results accessible.

**States:** Loading checks storage status; Empty offers Add key; Error labels missing key, unreadable Keystore entry, invalid credential, or test timeout with specific actions; Success shows stored/tested status independently. No “saved successfully” state before secure storage confirms completion.

### 14 · Model selection, routing, and fallback

**Layout:** Default model selector, capability chips, optional routing toggle, task-role rows, and fallback order. Use a detail sheet for model capabilities rather than a wide comparison grid on phones.

**Mock content:** Gemini “Fast planner” default: tools, structured output, streaming. “Recovery model” for diagnosis; “Vision model” for selected images. Fallback: Recovery model, same provider. Cross-provider fallback off. A disabled fixture model reads “Missing tool-calling capability.”

**Components and behavior:** ModelSearchList, CapabilityChip, RoutingRuleRow, reorderable FallbackList, privacy-impact sheet. Routing off forces the chosen model and shows unsupported tasks honestly. Fallback respects current sharing rules and user-enabled destinations. Unknown capabilities display Unknown; they are not assumed supported. Provider catalog refresh preserves an existing selection until validation resolves it.

**States:** Loading fetches/catalogs; Empty explains no eligible models and retains credentials; Error offers refresh or another provider without clearing preferences; Success shows effective task routing. Unavailable models cannot silently expand data sharing or switch providers.

### 15 · Privacy controls and cloud payload inspector

**Layout:** Plain-language summary “Processing stays on your device; selected context goes to your AI provider,” global sharing controls, provider-specific exceptions, and a preview of the next outbound request.

**Mock defaults:** Filenames on within selected folders; metadata on; redacted command output on; file contents off; media off; selected image sharing requires per-task approval. Preview: 500 filenames + codec summaries, no video bytes, Gemini destination. Show raw byte/token estimates as estimates.

**Components and behavior:** PrivacyToggleRow, ScopeChip, RedactionPreview, outbound-category accordion, Reset defaults. Distinguish persistent preference from one-task consent. Tightening privacy pauses future requests that cannot comply; already sent data cannot be recalled. Logs and tool output remain untrusted data, visually separate from user instructions.

**States:** Loading computes the payload preview; Empty means no outbound request is pending; Error blocks send if data cannot be classified/redacted; Success shows effective allowed/excluded categories. A preview must match the actual send path, not merely document an intended policy.

### 16 · AI usage, budgets, and limits

**Layout:** Today / Week / Month control, request and token totals, optional cost estimate, provider breakdown, task usage list, and budget editor.

**Mock content:** Today 42 requests, 128k input tokens, 18k output tokens; illustrative estimate $0.14 for requests with a configured price table; two requests have unknown usage and are excluded from cost totals. Task A 18/40 requests; task token budget 60k; recovery limit 3 attempts. Cost warning at 80% and hard task stop at the configured limit.

**Components and behavior:** UsageSummary, simple accessible bar chart with table alternative, BudgetProgress, LimitEditor, task drill-down. Distinguish local estimates from provider billing; unknown usage is not zero. A hard local budget blocks the next AI request rather than promising exact enforcement of a remote bill. Editing budgets is explicit, with an audit entry for active tasks.

**States:** Loading restores usage; Empty shows “No AI requests yet”; Error retains last available totals with unknown intervals marked; Success shows sourced totals and current limits. Offline local work has no inferred AI usage.

### 17 · Device and environment inspector

**Layout:** Device identity row, storage and memory cards, battery/thermal strip, then capabilities grouped as Android, granted storage, Linux tools, and root. Include Last inspected and Refresh.

**Mock content:** Snapshot D1. Storage 48.2/256 GiB free; Synapse memory 612 MiB; app CPU 42% of one core; system CPU unavailable. Android 15/ARM64; battery 68% charging; thermal Moderate. Root detected, disabled. Tools: FFmpeg, Python, Git; missing ImageMagick capability.

**Components and behavior:** MetricCard, storage distribution with text labels, CapabilityRow, FreshnessLabel. Tap any metric for source, scope, refresh time, and permission limitation. CPU/temperature charts show only measured values. Scoped Android app/package visibility and protected directories are explained as actual platform limits.

**States:** Loading inspects each section independently; Empty means a capability has not been scanned; Error is localized to a metric with retry/settings links; Success shows measured, unavailable, and stale values distinctly. Refresh never invokes a root authorization prompt implicitly.

### 18 · Granted folders, file inspector, and sandbox policy

**Layout:** Granted folders list, Add folder, per-folder access chips, then expandable sandbox limits. Selecting a folder opens a searchable file list with breadcrumbs, metadata detail, and Use in task. The sandbox editor uses grouped controls, not a single advanced text form.

**Mock content:** Downloads: read/write; Movies/Trips: read/write; Music/Converted: write via grant; Android/data: restricted. File row Lisbon_0327.mp4, 182 MiB, H.264/AAC, 1080p, 3m 42s. Sandbox: only selected document grants, 5 GiB temporary-storage cap, two concurrent jobs, network policy restricted to chosen provider/package sources where the runtime can enforce it.

**Components and behavior:** FolderGrantRow, FileBrowserList, MediaMetadataCard, ScopePicker, SandboxLimitField, enforcement-status badge. A setting unsupported by a backend is “Unavailable” or “Advisory,” never “Enforced.” Revoking access explains affected jobs and pauses them safely. Protected-path access is not solved with an ordinary toggle.

**States:** Loading reads document metadata; Empty invites a folder grant or shows an empty folder; Error handles revoked permissions, moved documents, and unsupported enforcement; Success reflects the actual grant and validator status. File previews obey privacy settings and do not upload content automatically.

### 19 · Linux environment and package dashboard

**Layout:** Runtime health card with architecture and storage, Installed / Available / Updates tabs, search, and a task queue. Primary actions: Install Package, Verify Runtime, Repair Environment.

**Mock content:** “Linux userspace · ARM64 · healthy · 1.84 GiB.” Installed: FFmpeg 7.1, 286 MiB; Python 3.12, 142 MiB; Git 2.47, 34 MiB; Curl 8.11, 8 MiB. Available: ImageMagick 7.1, 94 MiB estimated. FFmpeg has one demo profile update; repository indexes last refreshed 4h ago. Versions are fixtures, not update recommendations.

**Components and behavior:** RuntimeHealthCard, package SearchBar, PackageRow, UpdateBadge, InstallQueue. Runtime bootstrap gets its own preview: source, architecture, download, expanded size, verification, accessible paths. Package counts exclude unavailable entries. Updates warn about running jobs using the affected binary and can wait until the job finishes.

**States:** Loading reads package metadata; Empty offers runtime bootstrap or no search results; Error distinguishes repository offline, checksum failure, architecture mismatch, and broken binary; Success means the runtime/binaries have passed actual checks. Repair proposes changes and risk before acting.

### 20 · Package detail, dependencies, installation, and repair

**Layout:** Package identity, Installed/Available status, version and source, size impact, dependency tree, used-by tasks, then install/update/remove actions. Installation becomes a stepper: Download → Verify artifact → Install → Probe binary → Verify capability.

**Mock content:** Task B's MP3-enabled FFmpeg profile, 38.4 MiB transfer, +112 MiB footprint. Dependency rows include codec libraries and certificate data; shared dependencies are counted once. A Python task example shows Python → requests → urllib3/certifi. Sample output: “Artifact verified,” “ffmpeg -version: exit 0,” “Encoder check: libmp3lame present.”

**Components and behavior:** DependencyTree with accessible indented-list alternative, StorageImpact, SourceVerification, TransactionStepper, bounded log viewer. Install Package previews package identity and sandbox scope. Removal shows dependent packages and tasks. Retry resumes only verified downloads; failed signatures/checksums block use. Repair does not silently disable verification or fall back to an unknown mirror.

**States:** Loading resolves dependencies; Empty dependency list states None; Error preserves the failing stage and rollback availability; Success verifies both binary execution and the requested capability, then returns to the interrupted task for retry.

### 21 · Root and security policy hub

**Layout:** Mode segmented control Normal / Autonomous / Manual; root availability card; per-risk rule rows; task/resource limits; recent policy decisions. Root enablement is visually isolated from routine preferences.

**Mock content:** Normal selected. L0 reads allowed in granted folders; L1 copies allowed in approved scope; L2 moves ask; L3 delete/overwrite always ask; L4 root/system always ask; L5 critical blocked. Root: “Detected · disabled.” Alternative previews: Unavailable, Permission denied, Manager unavailable.

**Components and behavior:** ModeSelector, RootStatusCard, CapabilityGrantEditor, RiskRuleRow, PolicyDecisionLog. Autonomous mode grants named operations, targets, and limits; it does not lower mandatory destructive/root confirmations. Manual mode produces proposals and runs nothing automatically. Enabling root explains detection versus authorization and invokes the device's root manager only on deliberate user action. It never approves future operations by itself. Typed root operations are preferred; exceptional command execution has separate explicit consent and policy validation.

**States:** Loading reads policy/detection; Empty means no optional grants; Error keeps the backend off and explains denial; Success lists effective privileges with scope and expiry. Never provide a universal “allow all” switch.

### 22 · Agent memory and learned environment

**Layout:** Facts / Preferences / Task context tabs, search, scope filters, and a memory detail editor. Facts show source and freshness; preferences show owner and effective scope.

**Mock content:** Observed Android 15, ARM64, 48.2 GiB free, FFmpeg available; “observed 2m ago.” Preferences: Never delete automatically, put audio in Music/Converted, ask before package installs. Task fact: preferred audio strategy failed once; AAC stream copy verified for Porto_0087.mp4.

**Components and behavior:** MemoryRow, SourceBadge, ScopeSelector, expiry/refresh indicator, EditPreferenceSheet, DeleteMemoryDialog. Device facts are refreshed from evidence; user corrections are stored as explicit preferences/annotations rather than falsifying telemetry. Preferences conflicting with policy explain precedence. Deleting memory does not undo actions or delete independent audit records. Task context expires according to a visible retention setting; credentials are never memories.

**States:** Loading reads scoped records; Empty invites a preference or device scan; Error marks stale/conflicting facts and offers revalidation; Success confirms a changed preference and identifies which future tasks use it. New grants are never inferred solely from a remembered preference.

### 23 · Scheduler and unattended task review

**Layout:** Scheduled list and Create schedule; editor has goal/approved plan, destination, frequency, local timezone, charging/network/storage constraints, and notification/approval behavior. Show next eligible window, not an exact execution guarantee.

**Mock content:** “Compress new travel videos,” daily after 22:00 Asia/Gaza, only while charging, battery above 40%, at least 8 GiB free, Wi-Fi for AI requests. Next window reads “After 22:00 when conditions permit.” An offline approved-plan task can continue local work; AI planning waits for connectivity.

**Components and behavior:** ScheduleRow, TimeWindowPicker, ConditionChip, MissedRunCard, ApprovalRequirementSummary. A scheduled run cannot reuse expired or broader destructive approval. Ask-required operations pause and notify. Editing a plan invalidates dependent consent. Cancellation disables future runs and separately asks what to do with an already running instance.

**States:** Loading restores schedules; Empty offers Create schedule; Error identifies missing grant, credential, or unavailable background permission; Success shows enabled rules and last run. Android battery restrictions and deferred scheduling are represented honestly.

### 24 · Command preview, terminal, and process inspector

**Layout:** Runtime/path header, read-only command preview by default, environment delta, risk/scope summary, Run / Edit / Cancel; running view adds exit status, elapsed time, stdout/stderr tabs, and expandable resource metrics.

**Mock content:** “python scripts/catalog.py --source /workspace/input”; working directory in Linux userspace; no secret values in the environment preview. Output: “Indexed 156 media files,” “3 unsupported formats skipped,” “manifest.json written.” A failure variant shows ModuleNotFoundError: requests and Diagnose action.

**Components and behavior:** CommandPreviewCard, MonospaceLogPanel, ProcessStatusRow, scroll lock, copy-redacted-output. Shell execution remains governed by policy and sandbox, including user-edited commands. Output is untrusted text, never an instruction source or active link/script by default. No root prompt is implied by opening the terminal. Process controls identify the selected runtime process and safe termination scope.

**States:** Loading attaches to a process; Empty has a command draft/examples; Error separates command failure from session loss; Success shows exit 0 plus task-specific output verification, not automatic task success. Large output is bounded with a downloadable redacted log rather than unbounded Compose text.

### 25 · Android apps and APK inspector

**Layout:** Visible apps list with search; APK detail screen with app name, package identifier, version comparison, architecture, permissions, signer comparison, storage impact, and Install via Android action.

**Mock content:** Sample Notes APK: installed 2.4, candidate 2.3, ARM64 supported, signature matches, no newly requested permissions, 38 MiB. Warning: “Candidate is older than installed version.” Available choices: Select newer APK, Keep installed version, or inspect details. Do not default to forced downgrade.

**Components and behavior:** AppRow, VersionComparison, PermissionDiff, SignatureStatus, SystemInstallerHandoff. Install/uninstall routes through supported Android consent flows; the AI does not dismiss system dialogs. APK signature validity, signer match, and source trust are distinct facts. List visibility limitations are stated without inventing missing installed apps.

**States:** Loading inspects archive/signature; Empty invites APK selection or reports no visible matches; Error handles malformed APK, mismatch, unsupported ABI, denied installer, and user cancellation separately; Success rechecks installed version before reporting installation verified.

### 26 · Tool and plugin capability catalog

**Layout:** Searchable groups Files / Media / Android / Linux / Development / Root; filter Available / Needs setup / Disabled. Tool detail shows parameters, scope, backend, risk conditions, verification method, and recent uses.

**Mock content:** file.copy available via Android document grants; media.extract_audio available; media.resize needs a runtime capability; python.run available in userspace; pdf.merge optional plugin not installed; root.typed_operation disabled. A PDF plugin detail shows package source, version, requested path/network capabilities, and size before installation.

**Components and behavior:** CapabilityCatalogRow, ToolSchemaPanel, RequirementList, PluginPermissionDiff. “Available” comes from probing the actual backend, not catalog existence. Installing or updating a plugin goes through package verification and permission review. Test tool opens a scope-limited preview; do not execute from a catalog tap.

**States:** Loading discovers capabilities; Empty distinguishes none installed from search misses; Error marks failed probes and offers runtime repair; Success explains enabled tools and dependencies. Root capability remains locked until separate authorization.

### 27 · App settings, appearance, and support

**Layout:** Sections for Appearance, Accessibility, Notifications, Local data, Diagnostics, and About Synapse. Keep AI/privacy/security shortcuts in their own clear groups rather than duplicating the underlying controls.

**Mock content:** System theme; fixed Synapse palette; motion follows system; task completion and approval notifications on; log retention 30 days; diagnostics export 2.4 MiB estimated. “Synapse · demo build” in design previews, actual version/build in production.

**Components and behavior:** PreferenceRow, ThemePreview, notification-channel links, DataRetentionEditor, ExportReviewSheet. Exports show which paths/logs remain after redaction. Clearing history explains impact on audit and restore metadata; deleting credentials is a separate action. Theme choices immediately preview and persist after confirmation where appropriate.

**States:** Loading restores preferences; Empty diagnostics means nothing to export; Error preserves settings if storage/export fails; Success reports saved preferences or exported local file. Accessibility defaults follow system preferences and do not require a separate setup path.

## 6. Global state, notification, and interruption contract

Use a real task state machine, independent of screen routes:

| Internal outcome | User-facing wording | Primary next action |
|---|---|---|
| PLAN_ONLY | Plan ready · no changes made | Review plan |
| SUCCESS | Completed and verified | Open results |
| SUCCESS_AFTER_RECOVERY | Completed after recovery · verified | View recovery |
| PARTIAL_SUCCESS | Some work completed · exceptions listed | Review remaining |
| WAITING_FOR_USER | Your decision is needed | Review request |
| WAITING_FOR_PERMISSION | Access is needed | Choose/grant scope |
| WAITING_FOR_RESOURCE | Waiting for network/storage/conditions | View condition |
| BLOCKED | Blocked by policy or task limit | Review policy/limit |
| UNSAFE_TO_CONTINUE | Stopped to protect your data | Review diagnosis |
| FAILED_AFTER_INVESTIGATION | Could not complete after investigation | View evidence |
| CANCELLED | Cancelled · completed changes listed | Review/undo changes |

Pause, pausing, resuming, running, and verification-pending are lifecycle states; none are success outcomes. Each result includes timestamps and evidence freshness.

Interruptions to design: lost connectivity, expired credential, app/process death, grant revoked, storage exhausted, thermal pause, unsupported model, notification permission denied, cancelled system picker, changed source file, changed destination, unavailable runtime, and stale approval. Preserve completed work and drafts. Offline mode disables AI-dependent steps with an explanation while exposing permitted local functions.

Notification examples:

- Running: “Synapse · Compress travel videos · 320/500 verified,” Pause / View.
- Decision: “Synapse needs approval for 27 trash operations,” Review only; no destructive one-tap approval on the lock screen.
- Waiting: “Paused at a safe checkpoint · charging required,” View conditions.
- Result: “494 verified · 2 need attention,” View result. Avoid “All done” for partial success.

## 7. Compose implementation contracts

Screens are stateless renderers of observable UI state. ViewModels combine task repositories, capability discovery, policy decisions, and provider state; composables do not execute tools or store credentials. Use lifecycle-aware collection, immutable models, stable LazyColumn keys, and persisted navigation/task identifiers. A persistent task repository and background coordinator own execution across screen recreation.

Core reusable components:

| Component | Inputs | Events and rules |
|---|---|---|
| SynapseScaffold | title, mode, model, running summary | Navigate; handle safe insets |
| AgentComposer | draft, attachments, model capabilities, sending state | Send, attach, stop response; preserve draft |
| TaskSummaryCard | task ID, verified counts, stage, outcome | Open task or required decision |
| StageRail | current stage, completed stages, recovery state | Open evidence for stage |
| RiskChip | level, policy label | Readable text and icon; no independent authorization logic |
| ApprovalCard | request ID, plan revision, target summary, expiry, policy | Approve exact scope, reject, review; disable during submit |
| PlanStepAccordion | step, impact, targets, risk, validation | Expand, edit; route mutations through plan validation |
| EventAccordion | observation, summary, redacted details, timestamp | Expand/copy redacted details |
| VerificationChecklist | checks, observed values, evidence timestamps | Open artifact; unknown remains unknown |
| CheckpointCard | revision, freshness, completed outcomes, resume eligibility | Resume review or inspect |
| PackageTransaction | source, dependencies, download/install/verification state | Approve, cancel safely, retry specific stage |
| MetricCard | value, unit, source, timestamp, availability | Refresh or explain limitation |
| StatePane | loading/empty/error/ready and contextual action | No generic errors without a recovery destination |

Suggested contract sketch:

~~~kotlin
data class TaskProgressUi(
    val total: Int,
    val verified: Int,
    val skipped: Int,
    val blocked: Int,
    val running: Int,
    val queued: Int,
)

data class ApprovalUi(
    val requestId: String,
    val planRevision: Long,
    val summary: String,
    val targetCount: Int,
    val riskLevel: Int,
    val reversible: Boolean,
    val expiresAt: Instant?,
    val submitting: Boolean,
)

sealed interface TaskUiAction {
    data class Approve(val requestId: String, val planRevision: Long) : TaskUiAction
    data object Pause : TaskUiAction
    data object ReviewRollback : TaskUiAction
}
~~~

This is a UI contract, not an authorization implementation. The executor must revalidate identity, current target state, permission, policy, and plan revision. The UI cannot grant authority by sending a risk number.

Use Material 3 Scaffold, TopAppBar, NavigationBar/NavigationRail, Card, FilterChip, ModalBottomSheet, AlertDialog, OutlinedTextField, LinearProgressIndicator, and adaptive list-detail layouts. Full-screen review is preferred to a cramped dialog for long destructive scopes. Forms expose validation close to the field; technical payloads are behind disclosure controls.

Provide a DemoScenarioRepository and PreviewParameterProvider with deterministic IDs/timestamps. Keep demo state separate from production repositories and executors. Every designed action has a working local preview transition; mocked installation never calls a package manager, changes real root access, or saves a real credential. Simulated success explicitly includes simulated verification.

## 8. Preview matrix and acceptance criteria

For every route: populated ready state, initial loading, meaningful empty state, recoverable error, and relevant final success. Also include dark/light, compact/expanded, 200% text, RTL, TalkBack semantics, reduced motion, and offline variants where applicable. A prototype scenario picker exposes these variants without placing developer controls in the real product flow.

Priority walkthroughs:

1. First-run Gemini setup → selected folder → Task C dry run → edit exclusions → approve moves → separate destructive review → audit → rollback conflict.
2. Task A execution → pause → process stop → restore checkpoint → validate partial output → resume → partial final result.
3. Task B missing encoder → inspect evidence → approve verified package profile → probe capability → retry → verify output.
4. Provider outage → bounded backoff → eligible fallback with unchanged privacy scope; invalid key instead opens credential repair.
5. Root detected but disabled → explicit backend enablement → exact-operation approval; denied/unavailable states never appear as enabled.
6. Scheduled task meets charging/storage conditions → starts approved local steps → pauses for fresh destructive consent.
7. Edit/delete a scoped memory → inspect policy precedence → verify future task preview reflects the change.

Acceptance checks:

- All fixture counts reconcile; Task A active counts sum to 500, and finished Task A fixture counts also sum to 500.
- No task is called successful before the required verifier results exist.
- No approval remains valid after a material scope change.
- Package/source verification failures block installation/use; mock availability is never a runtime capability claim.
- Only launcher artwork from assets is used; UI icons are Material vectors or independently authored resources.
- All controls have useful actions, accessibility labels, safe-area behavior, and loading/disabled feedback; no dead-end mock controls.
- Screens preserve draft, selected task, and context after Back, permission handoff, rotation, and process restoration.
- Credentials, hidden model reasoning, and unredacted secrets are absent from logs, previews, and exported examples.
- Root, network restriction, background survival, telemetry availability, and rollback are labeled according to actual backend support.
- Five-second comprehension target: the user can identify what is happening, what changed, what requires a decision, and the next useful action.

## 9. Implementation sequencing without reducing design coverage

All 27 routes above are part of the design. Build shared tokens/components and deterministic demo state first, then implement Agent → Plan → Approval → Execution → Result. Add provider/privacy and Activity as the next connected journeys, then Device/runtime/security/memory. Bind each designed capability to real backend state as available; retain explicit demo/unsupported states until that binding is validated.

The supplied spec covers the full product vision. This document does not assert that proposed screens, package operations, root support, or mock measurements have been implemented or validated on an Android device.
