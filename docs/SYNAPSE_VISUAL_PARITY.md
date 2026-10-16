# Synapse v3 visual parity

31/31 reference screens have real, navigable Compose implementations in the Demo workspace. Build and interaction verification passed. **Visual parity remains incomplete:** all 31 rendered screens were compared, and the differences below remain. “Build verified” is deliberately separate from visual acceptance.

Implementation snapshot: `ded803b`. Work began on clean, fetched `main` at `a60f05c`. All `assets/synapse/v3/references/screen_01.png` through `screen_31.png` exist. The mapping below follows PDF page order, independently of legacy enum IDs. The All screens directory now uses this reference order.

[Interactive comparison viewer](visual-parity/index.html) · [Captured renders](visual-parity/actual/) · [Validation record](visual-parity/validation.json) · [Asset audit](SYNAPSE_ASSET_INTEGRATION.md)

## Capture and review

Native Robolectric/Android canvas captures: API 34, 412 × 700 dp app window, mdpi, font scale 1, Light theme, Demo state. Page 08 selects verified task SYN-2044; page 20 starts the Python transaction review. References are normalized to a 412 × 746 dp display, excluding the 24 dp status and 22 dp gesture areas to compare app windows at the same size. Real Android owns system bars; the app does not paint a mock status bar. Side-by-side and 50% overlay images were inspected for every screen; this is not a pixel-perfect claim or an automated similarity pass.

The second refinement pass reduces shared tab, field, row, switch and navigation geometry to the reference scale. Text remains scalable and content scrolls on smaller displays. Android font metrics, status bars, spacing, borders and component geometry still differ. Exact illustrations, provider/app/package logos, and media thumbnails are absent as standalone assets. See the asset audit; no reference image is used as a Compose interface.

## Screen record

Each row has passed compilation and a native capture. Interaction states remain Demo-only except persisted appearance choices and ordinary documentation links. Existing Live code and approval mechanisms remain separate.

| Reference | Compose route | Implemented components / official assets | Functional state | Build | Screenshot comparison | Remaining mismatches | Completion status |
|---|---|---|---|---|---|---|---|

| [screen_01.png](../assets/synapse/v3/references/screen_01.png) — Welcome | `WELCOME` | Setup cards and entry actions; brand, provider/folder/security icons | Setup links, skip, scroll | Passed | Native render + overlay reviewed | Standalone phone illustration absent; larger text and targets | Build verified |
| [screen_02.png](../assets/synapse/v3/references/screen_02.png) — Agent Dashboard | `AGENT` | Goal composer, quick-action grid, recent tasks; agent/file/tool icons | Input, suggestions, plan and task links | Passed | Native render + overlay reviewed | Row density, composer text, sample task metadata | Build verified |
| [screen_03.png](../assets/synapse/v3/references/screen_03.png) — Attachments | `ATTACHMENTS` | Picker sheet, attached-item rows, preview; image/file/folder/upload icons | Select, remove, preview, continue | Passed | Native render + overlay reviewed | Thumbnail assets absent; accessible picker rows scroll | Build verified |
| [screen_04.png](../assets/synapse/v3/references/screen_04.png) — Plan Preview | `PLAN` | Task summary, six steps, scope and tool cards; folder/file/approval icons | Destination/exclusions editing; revision invalidates approval | Passed | Native render + overlay reviewed | Existing Downloads scenario differs; lower sections scroll | Build verified |
| [screen_05.png](../assets/synapse/v3/references/screen_05.png) — Approval | `APPROVAL` | Scope/risk review and guarded consent; security/approval/delete icons | Destructive acknowledgement and exact-plan approval | Passed | Native render + overlay reviewed | Existing safe scope and consent copy differ; density | Build verified |
| [screen_06.png](../assets/synapse/v3/references/screen_06.png) — Execution Monitor | `MONITOR` | Progress, task stages, resources, log/evidence; play/pause/activity icons | Pause/resume, simulate verification, cancel | Passed | Native render + overlay reviewed | Existing verified counts and task fixture; log density | Build verified |
| [screen_07.png](../assets/synapse/v3/references/screen_07.png) — Recovery | `RECOVERY` | Failure/evidence, choices, prerequisites; recovery/warning/package icons | Select remedy, clarify, confirm alternate format, bounded retry | Passed | Native render + overlay reviewed | Recovery fixture and mandatory safety copy differ | Build verified |
| [screen_08.png](../assets/synapse/v3/references/screen_08.png) — Task Completed / Result | `TASK_RESULT` | Summary, result/folder/log tabs, output and next actions; file/audit/undo icons | Tabs, file/audit/undo links; complete only when verified | Passed | Native render + overlay reviewed | Thumbnail assets absent; retained verified-task fixture | Build verified |
| [screen_09.png](../assets/synapse/v3/references/screen_09.png) — Checkpoints | `CHECKPOINT` | Activity tabs, checkpoint list and settings; checkpoint/history icons | Create sample checkpoint, retention, restore review | Passed | Native render + overlay reviewed | Task-only checkpoint scope; readable list rows scroll | Build verified |
| [screen_10.png](../assets/synapse/v3/references/screen_10.png) — Activity History | `ACTIVITY` | Tabs, search/status filters, task rows; history/activity icons | Search, filter, task/audit navigation | Passed | Native render + overlay reviewed | Existing task list and outcome counts differ | Build verified |
| [screen_11.png](../assets/synapse/v3/references/screen_11.png) — Schedules | `SCHEDULE` | Schedule list, filters and editor; schedule/agent icons | Search/filter, enable, validated time/goal editing | Passed | Native render + overlay reviewed | Schedule fixtures and accessible row heights differ | Build verified |
| [screen_12.png](../assets/synapse/v3/references/screen_12.png) — Audit | `AUDIT` | Task summary, changes/timeline/tool-log tabs; audit/file/tool icons | Tabs, evidence disclosure, result/undo links | Passed | Native render + overlay reviewed | Existing evidence scenario; no media thumbnails | Build verified |
| [screen_13.png](../assets/synapse/v3/references/screen_13.png) — Undo | `UNDO` | Change selection, conflicts, confirmation; undo/folder/delete icons | Select, conflict policy, confirm simulated restore | Passed | Native render + overlay reviewed | Existing safe rollback fixture; larger rows and consent copy | Build verified |
| [screen_14.png](../assets/synapse/v3/references/screen_14.png) — Device Overview | `DEVICE` | Device identity and resource grid; device/network/security icons | Capability and tool/package links | Passed | Native render + overlay reviewed | Standalone device photo absent; sample metadata differs | Build verified |
| [screen_15.png](../assets/synapse/v3/references/screen_15.png) — Files | `SANDBOX` | Storage sidebar, search, selected rows and action dialogs; folder/file icons | Select, create/rename validation, delete confirmation, copy/move preview | Passed | Native render + overlay reviewed | Dense table scrolls; simplified file metadata and hierarchy | Build verified |
| [screen_16.png](../assets/synapse/v3/references/screen_16.png) — Apps / APKs | `APPS` | Search, filters, sample app inventory and detail dialog; apps icon | Search, status selection, details/actions preview | Passed | Native render + overlay reviewed | Third-party app logos absent; sample inventory differs | Build verified |
| [screen_17.png](../assets/synapse/v3/references/screen_17.png) — Terminal | `TERMINAL` | Dark log, command input, quick/recent commands; terminal/play icons | Session tabs, editable command, simulated output | Passed | Native render + overlay reviewed | Command/session fixtures and log typography differ | Build verified |
| [screen_18.png](../assets/synapse/v3/references/screen_18.png) — Tools | `TOOLS` | Grouped tool catalog and configuration; tool-specific official icons | Search, details, enable/disable sample tool | Passed | Native render + overlay reviewed | Reference density and contract details differ | Build verified |
| [screen_19.png](../assets/synapse/v3/references/screen_19.png) — Packages | `PACKAGES` | Search/filter, package rows and install links; package/download icons | Select, installed filter, transaction navigation | Passed | Native render + overlay reviewed | Third-party package logos absent; existing package fixture | Build verified |
| [screen_20.png](../assets/synapse/v3/references/screen_20.png) — Package Transaction | `PACKAGE_DETAIL` | Package summary, stages, log and actions; package/download/approval icons | Approval, staged verification, cancel/remove guards | Passed | Native render + overlay reviewed | Existing dependency/probe stages preserved; fixture differs | Build verified |
| [screen_21.png](../assets/synapse/v3/references/screen_21.png) — App Settings | `SETTINGS` | Provider, model settings, usage panels; provider/model/usage icons | Provider links, temperature, token validation, reset confirmation | Passed | Native render + overlay reviewed | Provider logos absent; dense controls need more scrolling | Build verified |
| [screen_22.png](../assets/synapse/v3/references/screen_22.png) — AI Providers | `PROVIDERS` | Provider catalog, defaults/fallback and preferences; provider/settings icons | Configure, selection, routing, simulated connection check | Passed | Native render + overlay reviewed | Provider logos/model chips absent; fixture and density differ | Build verified |
| [screen_23.png](../assets/synapse/v3/references/screen_23.png) — Credentials | `CREDENTIALS` | Sample credentials and storage settings; key/security/delete icons | Alias validation, reveal placeholder, test, clear confirmation | Passed | Native render + overlay reviewed | Provider logos absent; sample-only credential metadata | Build verified |
| [screen_24.png](../assets/synapse/v3/references/screen_24.png) — Models | `MODELS` | Searchable grouped catalog, model detail and fallback; model/provider icons | Enable/disable and select default sample model | Passed | Native render + overlay reviewed | Provider logos absent; reference rows/chips are much denser | Build verified |
| [screen_25.png](../assets/synapse/v3/references/screen_25.png) — Privacy | `PRIVACY` | Overview, sharing, retention, controls; privacy/upload/memory icons | Sharing switches, retention selection, confirmed reset | Passed | Native render + overlay reviewed | Safer existing sharing defaults; lower panels scroll | Build verified |
| [screen_26.png](../assets/synapse/v3/references/screen_26.png) — Usage | `USAGE` | Overview, native donut and bar charts, request list; usage/history icons | Metric and period selection, request detail, budget slider | Passed | Native render + overlay reviewed | Illustrative series; axis/detail density and lower panels differ | Build verified |
| [screen_27.png](../assets/synapse/v3/references/screen_27.png) — Security | `SECURITY` | Overview, execution/risk/root/sandbox/events; shield/root/warning icons | Mode, root detection, guarded demo root enabling | Passed | Native render + overlay reviewed | Existing immutable safety rules; dense reference panels scroll | Build verified |
| [screen_28.png](../assets/synapse/v3/references/screen_28.png) — Memory | `MEMORY` | Overview, switches/storage, search/filter/editor; memory/history icons | Add/edit/forget preferences; observed facts protected | Passed | Native render + overlay reviewed | Existing six-item fixture; switches and lower panels scroll | Build verified |
| [screen_29.png](../assets/synapse/v3/references/screen_29.png) — Permissions | `PERMISSIONS` | Overview, collapsible required/optional lists; permission/folder/device icons | Expand, review, grant/revoke sample status, security link | Passed | Native render + overlay reviewed | Actual permissions are separate; counts corrected; density | Build verified |
| [screen_30.png](../assets/synapse/v3/references/screen_30.png) — Appearance | `APPEARANCE` | Theme previews, accent/typography/layout/accessibility; appearance icons | Persisted theme/accent/font/size/density/contrast/icons; reduced motion | Passed | Native render + overlay reviewed | Theme-preview geometry; lower settings scroll; TalkBack owned by Android | Build verified |
| [screen_31.png](../assets/synapse/v3/references/screen_31.png) — About | `ABOUT` | Brand, installed version, updates/help/legal/info; brand/about/file icons | Release/docs/issue links, information dialogs, privacy link | Passed | Native render + overlay reviewed | Installed version and project facts replace fictional reference data; density | Build verified |

## Verification

- `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:lintDebug`: passed; five debug APK variants, zero lint errors, 16 warnings.
- `:agent-core:runAgentTests`: 109 passed. The module's plain Gradle `test` task is disabled; its custom harness is the relevant check.
- Full `:app:testDebugUnitTest`: 76 passed, zero failed/skipped. Includes every route/state, all 31 directory links and back navigation, Demo/Live switching, restoration, destructive approval, staged package verification, root confirmation, credential validation, memory editing, file-name validation, appearance persistence, and expanded dark/RTL/large text.
- `SynapseVisualParityTests`: 31 screen captures plus the 38-icon catalog; refreshed after exact SVG rounded-arc conversion. Captures are actual Compose/Android output, not reconstructed HTML or design screenshots.
- Every SVG concept is used in screen code. Branding copies match the supplied bytes; adaptive foreground fits the safe circle. All five APK ZIP inventories contain zero design-reference images or source SVGs.
- Lint warnings: existing storage/API compatibility and dependency declarations; supplied legacy launcher shape/mipmap configuration; retained unused earlier launcher/background resources. No lint error was suppressed.

Android SDK and native screenshot tooling are available. No emulator binary or KVM is available in this environment. Physical-device system-bar behavior, launcher variants and TalkBack have not been visually verified. Existing live device operations were not invoked by these UI tests.

## Reproduce / continue

```bash
source /workspace/synapse-env.sh # this cloud environment only
./gradlew :agent-core:runAgentTests :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
python scripts/compare-v3-screens.py --publish # requires Pillow
```

Batch implementation and comparison proceeded through 01–02, 03–05, 06–08, 09–11, 12–13, 14–16, 17–20, 21–24, 25–28, and 29–31. Final regression work corrected dialog measurement, selected-tab visibility, disabled undo selection, slider tracks, status pills, exact vector arcs, and display persistence.

Remaining acceptance work: obtain missing standalone artwork or approved substitutions; continue tightening each dense layout against the saved overlays, particularly Settings 21–31; validate status bars, launcher masks, and accessibility on a real device. Preserve the working navigation, Demo/Live isolation, safety wording and consent tests. Do not mark these screens visually accepted solely because they render or compile.

## Refinement pass

- 01–02: compact setup cards, numbered steps, progress, accent greeting, four suggestion tiles, six illustrated quick actions with captions, and compact recent-task rows. Compiled and compared native renders twice; the dashboard now shows all three recent tasks. Welcome still lacks the standalone phone illustration.
- Shared: reference-sized tabs, fields, rows, switch tracks and bottom navigation; typography and card spacing tightened. Native captures now align app windows after excluding system bars. The remaining batches are being rechecked with these shared changes.

- 03–05: compared three capture iterations; attachment picker captions and four suggestions added, attachments shown in a three-item review fixture, numbered plan stages and expandable action scope restored, permission summary made compact. Continue/approval rows stay visible as details scroll. Source media thumbnails remain unavailable; the existing Downloads scope and consent behavior remain authoritative.

- 06–08: compact verified progress, persistent stop/pause and recovery action rows, status markers, segmented result tabs, larger output rows, and side-by-side output/next-action panels. Two native capture iterations compared; existing task counts, failed-encoder prerequisites and verified-only completion preserved. Media and device-capture thumbnails remain absent from the source package.

- 09–11: checkpoint controls condensed into the reference summary, all eight checkpoint rows visible, timestamp/size metadata and official row icons restored. History filters and schedule search/create controls compacted; schedule toggles visible. Native captures compared. Existing seven-task history and schedule fixtures retained.

- 12–13: compact audit tabs, side-by-side task status/destination and change/evidence panels; Audit uses its reference action footer without the global bottom tabs. Undo has compact warnings, select/deselect-all, and a fixed preview/restore footer. Native renders compared; retained rollback scope and conflict policy require different text from the reference.

- 14–16: storage/RAM bars, compact quick tools and Apps actions; file sidebar, 17-row metadata table, selection and fixed Copy/Move/Rename/Delete footer restored. Two capture iterations compared. Device photo and third-party app artwork remain unavailable; official icons are used.

- 17–20: compact terminal command/log panels, reusable grouped catalogs, eleven realistic Demo package entries, and persistent transaction controls. Three native capture iterations compared. All tool names fit; final tool description and transaction logs can scroll. Existing artifact/checksum/userspace/probe stages are preserved; no package operation runs on the host.
