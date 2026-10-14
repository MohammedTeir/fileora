# Synapse Compose workspace

The [27-screen specification](SYNAPSE_UI_UX_SPEC.md) now has an executable
Compose implementation. Open **Try demo** in the app bar to explore it, and
use **All screens** (the grid icon) to reach any screen. **Go live** returns to
actual device operations. The four primary destinations are Agent, Activity,
Device and Settings.

## Boundaries

The demo banner remains visible throughout the sample workspace. Its reducer
has no executor, network client, file access or permission API. Installing a
sample package, running a sample command, approving a plan, or enabling sample
root changes only demo state. Demo credentials are fixed placeholders; there
is no API-key entry field in the simulation. Existing live settings, provider
credentials, files, task service, policy enforcement and audit storage remain
separate. Live approval dialogs explicitly say **Live task approval**, including
when a background task requests approval while the demo is open.

All 27 specified screens are populated and interactive in the demo. This does
**not** implement the missing Linux, package, root, shell, APK or memory
backends. Live routes show an explicit availability message for those features.
Supported live workflows reuse the existing validated components; some related
routes share a live screen, as listed below.

## Screen coverage

| # | Screen | Demo interactions | Live integration |
|---|---|---|---|
| 01 | Welcome | Provider, folders, safety and agent entry | Existing Android setup flow |
| 02 | Agent | Goal composer, suggestions, context, recovery and task links | Existing agent, preview mode, scheduling and results |
| 03 | Attachments | Select files, image and folder; inspect outbound categories | Granted-file browser and use-in-task |
| 04 | Plan preview | Edit destination, optional trash, step/tool disclosures | Existing dry-run setting and agent entry |
| 05 | Approval | Exact selection, destructive acknowledgement, revision checks | Real policy approval/review dialogs above the workspace |
| 06 | Monitor | Verified counts, pause/resume, advance a sample item, cancel | Actual session log and stop control |
| 07 | Recovery | Inspect evidence, repair package, approve alternate format, clarify, abort | Task activity; dedicated recovery screen is demo-only |
| 08 | Checkpoints | Progress/resource review, pause/resume and rollback entry | Persisted task resume and current evidence |
| 09 | Activity | Search active/history tasks, inspect outcomes and schedules | Room history and existing task activity |
| 10 | Audit | Summary, changes, timeline and raw tool evidence | Existing persisted task details in Activity |
| 11 | Undo | Fixed Downloads transaction, conflict choice and restore confirmation | Existing per-task undo in Activity |
| 12 | Providers | Provider switch, connection check and configuration links | Secure provider setup and actual test connection |
| 13 | Credentials | Add/remove/replace placeholder, clear connection state | Existing encrypted key manager |
| 14 | Models | Primary profile, fallback and routing | Actual provider model discovery and routing |
| 15 | Privacy | Five outbound categories and sanitized request preview | Existing file-content sharing controls |
| 16 | Usage | Per-task sample usage and daily request budget | Current session usage; daily budget UI is demo-only |
| 17 | Device | Storage, RAM, CPU, thermal/battery and capability samples | Actual public device/RAM/data-volume observations |
| 18 | Sandbox | Grant/revoke sample folders with confirmation | Android SAF and optional All Files Access |
| 19 | Packages | Search/filter, install/manage, health and repair entry | Backend unavailable |
| 20 | Package transaction | Dependencies, four verified steps, cancel and guarded removal | Backend unavailable |
| 21 | Security | Modes, risk rules, detected/unavailable/denied root states | Existing execution settings; optional modes/root are demo-only |
| 22 | Memory | Observed/preference filters, edit/add and forget | Backend unavailable |
| 23 | Schedules | Goal/time validation, enabled state, charging constraint | Existing periodic schedules in Activity/agent composer |
| 24 | Terminal | Command preview, explicit simulated result, bounded metadata | Backend unavailable |
| 25 | Apps/APKs | Search/select sample apps, identity and downgrade review | Backend unavailable |
| 26 | Tools | Search contracts, inspect risk/schema/verification | Full catalog UI is demo-only; existing agent tools remain live |
| 27 | Settings | Appearance, reduced motion, links and confirmed demo reset | Appearance, notifications, backup/import and actual settings |

Browse and Search remain reachable through Device and All screens. No existing
installation identifiers, databases, credential aliases or legacy import keys
were renamed. Only the retained launcher uses archived artwork; all new UI uses
Compose and Material vectors.

## Design and state implementation

- `ui/synapse/SynapseWorkspace.kt`: four destinations, saved navigation stack,
  screen directory, Live/Demo boundary and state preview selector. Bottom
  navigation below 600 dp, navigation rail above it, supplementary context panel
  at 1000 dp. Main content caps at 840 dp.
- `SynapseComponents.kt`: cards, semantic status/risk badges, progress counts,
  confirmation dialogs, accessible switches, wrapping actions, collapsible
  evidence and left-to-right selectable code blocks.
- `SynapseAgentScreens.kt`, `SynapseSettingsScreens.kt`,
  `SynapseDeviceScreens.kt`: complete demo presentations and event dispatch.
- `SynapseLiveScreens.kt`: adapters to current functionality, real device
  observations, and explicit unavailable states.
- `SynapseModels.kt`: pure demo reducer and bounded, versioned snapshot codec.
  Plan revisions invalidate consent; destructive targets require acknowledgement;
  output counts are conserved; recovery is bounded; package capability is
  declared only after the final check.
- `SynapseWorkspaceViewModel.kt`: demo state survives saved-instance/process
  recreation; appearance and reduced-motion preferences also persist across
  fresh launches. Reset affects only demo data.
- `ui/Theme.kt`: light/dark/system appearance, 32/40, 28/36, 22/28 typography,
  16/24 and 14/20 body text, 4/8 dp spacing, blue primary, labeled success/warning/
  error states, neutral read and purple privileged risk. Actions target 48 dp.

Use **States** in demo mode to inspect Ready, Loading, Empty, Error, Success and
Offline on each route. These are presentation previews, not assertions about a
live backend. Error states include a recovery action; offline previews explain
which work can continue. Operational disclosures show plans, policy decisions
and tool evidence, not private model reasoning.

The demo is deterministic: progress advances with **Simulate next verified
item**, package verification advances one step per action, and terminal output
is a clearly labeled fixed fixture. A typed goal uses the fixed Downloads plan
to demonstrate scope review; it does not invoke an AI model.

## Rendered examples

These are native Robolectric renders of the implemented Compose UI, not concept art:
[Agent](screenshots/synapse/agent.png),
[Device](screenshots/synapse/device.png), and
[expanded dark RTL / 150% text](screenshots/synapse/device-dark-rtl.png).

## Verification

Local validation: **109 core tests and 59 app tests pass**; debug APK builds;
Android lint reports **0 errors and 5 existing warnings**. The 59 app tests include
18 new reducer tests, 5 Compose screen/interaction tests and a fresh-install
Activity integration test. CI now runs the app tests and lint and retains reports,
rendered screenshots and debug APKs as artifacts.

Run with JDK 17 and Android SDK 35:

```sh
gradle :agent-core:check :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

`SynapseStateTests` covers plan/consent invalidation, exact scope counts,
conserved progress, pause/resume, completed-task protection, package verification,
recovery limits, root availability, credential state, memory editing, time
validation and snapshot restoration/corruption.

`SynapseScreenTests` uses Robolectric's native renderer at API 34. It exercises
all 27 routes plus Browse/Search in six presentation states, destructive approval,
package verification, saved navigation and Live/Demo switching. It also renders
an expanded dark RTL layout at 150% text scaling. `SynapseActivityTests` launches
the actual Activity and ViewModels, explores the demo, and returns to live device
inspection without credentials or device mutations. Gradle resolves the pinned
Robolectric runtimes into the build directory so a writable home directory is
not required. Rendered examples are written to
`app/build/outputs/synapse-screenshots/`.

These tests do not replace physical-device checks for TalkBack traversal,
keyboard/IME behavior, Android system pickers, OEM background execution,
root/package backends, or actual cloud-provider responses. Those remain device
and backend integration work.
