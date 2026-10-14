# Synapse Phase 1 — UI/UX audit (2026-10-14)

**Scope:** 27 numbered UI screens plus Task Completed result state. **Status:** visual coverage complete; functional compliance unverified.

**Source of truth:** `SYNAPSE_UI_UX_SPEC.md` and verified live capabilities take precedence over generated mockups. `SYNAPSE_UI_IMPLEMENTATION.md` reports interactive Compose demos for all 27 screens but explicitly excludes several live backends. This review did not build or run the app.

## Priority findings

| ID | Severity | Finding | Correction |
|---|---|---|---|
| F01 | Critical | Privacy artwork promises local models despite cloud-AI-only architecture | Remove local-AI fallback; distinguish offline non-AI operations |
| F02 | Critical | Package, terminal, APK and root screens imply operational backends | Explicit unavailable states in live mode; persistent Demo data labeling |
| F03 | High | Security screenshot asserts protected/sandbox state without runtime evidence | Bind to verified status; show unknown/unavailable |
| F04 | High | Checkpoint imagery suggests device-level restore | Use task-specific resume/rollback scope |
| F05 | High | Privacy image omits sanitized outbound payload inspector | Add data-category review and explicit consent |
| F06 | High | Memory screen implies working persistent learned memory | Mark demo-only until live backend exists |
| F07 | High | Usage chart and budgets appear authoritative | Separate session measurements from demo budget fixtures |
| F08 | Medium | Settings navigation differs across mockups | Normalize hierarchy, active state and Back behavior |
| F09 | Medium | Static model/device examples appear as actual observations | Use provider metadata and live device readings |
| F10 | High | Dense layouts not verified at 360dp, RTL or 200% font | Responsive/accessibility testing |
| F11 | High | Recovery/monitor show optimistic ETA and unverified repair | Use measured evidence, bounded retries and reapproval |
| F12 | Medium | Extra Task Completed image lacks numbered route | Treat as task result state, not 28th top-level route |

## Screen disposition

| Screens | Audit action |
|---|---|
| 01–02 | Verify onboarding, live/demo status, stable task identity |
| 03 | SAF grants and outbound data review |
| 04–05 | Plan editing, scope approval, revision invalidation |
| 06–07 | Measured progress, failure evidence, bounded recovery |
| 08 | Task checkpoint semantics |
| 09–11 | History, audit provenance, undo conflicts |
| 12–14 | Provider metadata, credential safety, model routing |
| 15–16 | Payload inspector, measured usage vs demo quotas |
| 17–18 | Actual device info and SAF scope |
| 19–20 | Unavailable live Linux/package backend |
| 21–22 | Root/security claims and unavailable memory backend |
| 23 | Scheduling constraints and timing claims |
| 24–25 | Unavailable live terminal and APK backend |
| 26–27 | Demo tool catalog and unified Settings IA |
| Extra result | Verify artifacts and task audit links |

## Next implementation gates

1. Every control has a route, dialog, real action, or explicit unavailable state.
2. Demo fixtures are never presented as live measurements or enforced security.
3. Cloud inference, attachments and consent are explained accurately.
4. Privileged/destructive actions have scoped approval and revision checks.
5. Test Compose at 360dp/412dp, landscape, RTL, large text and TalkBack.
6. Build/run and compare actual screens before claiming implementation compliance.

**Limit:** This is a document/image audit, not a code execution or device security assessment. A follow-up should inspect Compose source and run tests.
