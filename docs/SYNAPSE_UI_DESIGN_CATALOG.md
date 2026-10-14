# Synapse UI design catalog (October 2026)

This document supplements `SYNAPSE_UI_UX_SPEC.md` and `SYNAPSE_UI_IMPLEMENTATION.md`. It records visual mockups produced during the UI exploration. **Mockups are concepts, not evidence of implemented or tested Android functionality.**

## Numbered screens

| # | Screen | Visual artwork available in exported catalog |
|---|---|---|
| 01 | Welcome / onboarding | Yes |
| 02 | Agent dashboard | Yes |
| 03 | Attachment picker / outbound-data review | Missing original image |
| 04 | Interactive plan preview | Yes |
| 05 | Scoped approval | Yes |
| 06 | Execution monitor | Missing original image |
| 07 | Diagnosis / recovery | Missing original image |
| 08 | Checkpoints | Yes |
| 09 | Activity history | Yes |
| 10 | Audit / verification report | Yes |
| 11 | Undo / rollback review | Yes |
| 12 | AI providers | Yes |
| 13 | Credentials | Yes |
| 14 | Models | Yes |
| 15 | Privacy | Yes |
| 16 | Usage | Yes |
| 17 | Device overview | Yes |
| 18 | Files / sandbox | Yes |
| 19 | Packages | Yes |
| 20 | Package transaction | Yes |
| 21 | Security | Yes |
| 22 | Memory | Yes |
| 23 | Schedules | Missing original image |
| 24 | Terminal | Yes |
| 25 | Android apps / APKs | Missing original image |
| 26 | Tools | Yes |
| 27 | App settings | Yes |

## Additional illustrated screen / state

- **Task Completed / Result** — an extra visual concept illustrating a successful execution result. It is not a new numbered route in the 27-screen specification; it is a state/detail presentation associated with Agent task results and Audit (10).

## Export conventions

- The complete catalog PDF has 27 numbered positions plus the additional Task Completed concept.
- Unavailable artwork is represented by a plainly labeled placeholder, not a fabricated screenshot.
- A separate PDF includes only available artworks.
- Generated mockups contain fictional example data; model IDs, capabilities, package availability, filesystem access, and restore behavior must be verified against implementation.
- The Checkpoints concept currently implies whole-device restore; the canonical spec describes **task-level resumable checkpoints**. The concept should be corrected before implementation.
- Do not infer that the shell, root, package manager, or full agent memory backends are implemented merely because their UI appears in the mockups.

## Follow-up

Recover or regenerate the five missing screen images (03, 06, 07, 23, 25), reconcile designs with the UI spec, and re-export a fully illustrated PDF.