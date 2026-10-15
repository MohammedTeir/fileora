# Synapse v3 visual parity

Reference authority: `assets/synapse/v3/manifest.json`, PDF page order (independent of legacy route IDs). Work starts at `a60f05c` on clean, fetched `main`.

Native Robolectric screenshots use API 34, 412 × 746 dp, mdpi, font scale 1, Light theme, Demo state. Run `:app:testDebugUnitTest --tests '*SynapseVisualParityTests'`, then `python scripts/compare-v3-screens.py`. Actual renders, side-by-side comparisons and 50% overlays are under `app/build/outputs/visual-parity/` (generated, not shipped). Visual review means an actual render was inspected beside the reference; it does not mean pixel-perfect parity.

All sample actions remain Demo-only. Live routes, executor, credentials and approval policy remain separate. Body text and interactive targets retain accessible minimum sizes, so dense reference tables may scroll on phones. Android owns status/gesture bars; reference mock status bars are not painted into the app.

The supplied package calls its SVGs reconstructed concepts. All 38 are converted by `scripts/import-v3-icons.py`; original SVGs and references stay intact. The isolated Welcome phone illustration, media thumbnails, third-party app/provider logos and device photograph are absent from the package; no screenshot fragments or invented replacement artwork are used. This limits exact artwork parity.

| Page / reference | Route | Components / official assets | Functional state | Build | Screenshot comparison | Remaining mismatch | Status |
|---|---|---|---|---|---|---|---|
| 01 `references/screen_01.png` | `WELCOME` | Setup cards, actions; brand mark, provider/folder/security/settings icons | Navigation and setup links | Passed | Native render + overlay inspected; corrected card spacing and removed Welcome bottom bar | Missing standalone phone illustration; larger accessible text/targets; system bars | Visually verified |
| 02 `references/screen_02.png` | `AGENT` | Composer, 6 quick actions, recent tasks; official role icons | Input, submit, quick links, task navigation | Passed | Native render + overlay inspected; corrected composer and grid heights | Sample typography/spacing; last task scrolls; artwork unavailable | Visually verified |
| 03 `references/screen_03.png` | `ATTACHMENTS` | Attachment picker/preview/removal, plan steps, scoped approval; official file/folder/upload/security icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Demo fixture differs from reference; accessible rows require scrolling; no photo thumbnails | Visually verified |
| 04 `references/screen_04.png` | `PLAN` | Attachment picker/preview/removal, plan steps, scoped approval; official file/folder/upload/security icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Demo fixture differs from reference; accessible rows require scrolling; no photo thumbnails | Visually verified |
| 05 `references/screen_05.png` | `APPROVAL` | Attachment picker/preview/removal, plan steps, scoped approval; official file/folder/upload/security icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Demo fixture differs from reference; accessible rows require scrolling; no photo thumbnails | Visually verified |
| 06 `references/screen_06.png` | `MONITOR` | Progress/log/evidence panels, recovery choices, result tabs; official play/recovery/audit/file icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Existing task fixtures retained; missing thumbnail assets; controls scroll on compact viewport | Visually verified |
| 07 `references/screen_07.png` | `RECOVERY` | Progress/log/evidence panels, recovery choices, result tabs; official play/recovery/audit/file icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Existing task fixtures retained; missing thumbnail assets; controls scroll on compact viewport | Visually verified |
| 08 `references/screen_08.png` | `TASK_RESULT` | Progress/log/evidence panels, recovery choices, result tabs; official play/recovery/audit/file icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Existing task fixtures retained; missing thumbnail assets; controls scroll on compact viewport | Visually verified |
| 09 `references/screen_09.png` | `CHECKPOINT` | Activity tabs, searchable history, checkpoints, schedule editor; official activity/history/checkpoint/schedule icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Task-only checkpoints and existing demo tasks; larger accessible rows scroll | Visually verified |
| 10 `references/screen_10.png` | `ACTIVITY` | Activity tabs, searchable history, checkpoints, schedule editor; official activity/history/checkpoint/schedule icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Task-only checkpoints and existing demo tasks; larger accessible rows scroll | Visually verified |
| 11 `references/screen_11.png` | `SCHEDULE` | Activity tabs, searchable history, checkpoints, schedule editor; official activity/history/checkpoint/schedule icons | Interactive Demo; live actions isolated | Passed | Native render and overlay inspected | Task-only checkpoints and existing demo tasks; larger accessible rows scroll | Visually verified |
| 12 `references/screen_12.png` | `AUDIT` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 13 `references/screen_13.png` | `UNDO` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 14 `references/screen_14.png` | `DEVICE` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 15 `references/screen_15.png` | `SANDBOX` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 16 `references/screen_16.png` | `APPS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 17 `references/screen_17.png` | `TERMINAL` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 18 `references/screen_18.png` | `TOOLS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 19 `references/screen_19.png` | `PACKAGES` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 20 `references/screen_20.png` | `PACKAGE_DETAIL` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 21 `references/screen_21.png` | `SETTINGS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 22 `references/screen_22.png` | `PROVIDERS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 23 `references/screen_23.png` | `CREDENTIALS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 24 `references/screen_24.png` | `MODELS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 25 `references/screen_25.png` | `PRIVACY` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 26 `references/screen_26.png` | `USAGE` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 27 `references/screen_27.png` | `SECURITY` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 28 `references/screen_28.png` | `MEMORY` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 29 `references/screen_29.png` | `PERMISSIONS` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 30 `references/screen_30.png` | `APPEARANCE` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |
| 31 `references/screen_31.png` | `ABOUT` | Existing Compose; v3 shared frame/icons pending | Demo / Live separation retained | Pending | Pending | Legacy geometry/content | In progress |

## Resume / validation record

- Shared palette and compact geometry derive from v3 tokens and normalized references. Runtime branding and all 38 vector resources imported.
- Completed batch 01–02: native screenshot tests passed; overlays inspected. Next: 03–05 attachments, plan, approval.
- Build/test results and visual findings are recorded here before each batch commit.
- Batch 03–05: compiled and native screenshot tests passed; reference/actual/overlay reviewed. Demo fixture differs from reference; accessible rows require scrolling; no photo thumbnails.
- Batch 06–08: compiled and native screenshot tests passed; reference/actual/overlay reviewed. Existing task fixtures retained; missing thumbnail assets; controls scroll on compact viewport.
- Batch 09–11: compiled and native screenshot tests passed; reference/actual/overlay reviewed. Task-only checkpoints and existing demo tasks; larger accessible rows scroll.
