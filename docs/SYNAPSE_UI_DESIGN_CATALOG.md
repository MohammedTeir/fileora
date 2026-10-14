# Synapse UI design catalog — 31 visual designs

Updated October 2026. This catalog supplements [UI/UX specification](SYNAPSE_UI_UX_SPEC.md) and [Compose implementation](SYNAPSE_UI_IMPLEMENTATION.md).

**Inventory:** 27 originally numbered screen designs + 3 additional Settings designs (Permissions, Appearance, About) + 1 additional Task Completed/Result state = **31 illustrated pages**. All artwork was provided/generated during the design conversation and the final PDF has no placeholder pages.

## Visual design collection extension (October 2026)

The visual design collection contains **31 illustrated pages** organized by primary navigation destination. This extends the original **27 numbered screen specifications** without renumbering their existing routes or implying that every visual design is a distinct implemented Compose route.

| Primary destination | Visual pages, in navigation order |
|---|---|
| Agent (8) | 01 Welcome, 02 Agent, 03 Attachments, 04 Plan Preview, 05 Approval, 06 Monitor, 07 Recovery, **Task Completed / Result** |
| Activity (5) | 08 Checkpoints, 09 History, 23 Schedules, 10 Audit / Verification, 11 Undo / Rollback |
| Device (7) | 17 Overview, 18 Files / Sandbox, 25 Apps / APK, 24 Terminal, 26 Tools, 19 Packages, 20 Package Transaction |
| Settings (11) | 27 Settings Overview, 12 Providers, 13 Credentials, 14 Models, 15 Privacy, 16 Usage, 21 Security, 22 Memory, **Permissions**, **Appearance**, **About** |

### Additional Settings page designs

- **Permissions:** Review actual Android runtime grants and special app accesses, explain why each is needed, and link to the relevant OS permission flow. Treat the screenshot's counts and granted states as demo data. Do not silently request Accessibility, root, broad storage, or other privileged access.
- **Appearance:** Light/dark/system themes, accent preference, font scaling, layout density, reduced motion, high contrast, and screen-reader accessibility. Only display controls that are actually supported; preserve existing theme behavior.
- **About:** App name and branding, version/build read from the installed app, updates where supported, documentation/support, open-source licenses, privacy/legal links, and build metadata. Screenshot version/build/date are illustrative, not release facts.
- **Task Completed / Result:** A task outcome presentation state linked to Agent execution and task detail/audit; do not count it as a new standalone route unless implemented as such.

### Fidelity and implementation boundary

The 31 pages are **visual references**, not 31 verified Compose routes. Screenshots may show fictional providers, models, metrics, permission grants, package states, or device details. Verify these against runtime capabilities and real data. Task checkpoints are resumable work checkpoints, not full-device snapshots. Do not infer backend capabilities from mockups.

The companion inventory is [SYNAPSE_UI_DESIGN_CATALOG.md](SYNAPSE_UI_DESIGN_CATALOG.md).


## PDF collection

The conversation artifact is **Synapse_Complete_31_Screens_By_Navigation.pdf**, ordered by navigation destination, with PDF bookmarks. The PDF is not committed to this repository; the filename documents the exported deliverable, not a repository link.

## Implementation checklist

- [ ] Verify whether Permissions, Appearance, and About require new routes or can reuse Settings navigation.
- [ ] Verify Task Completed is a state of the task detail/execution workflow rather than a new route.
- [ ] Compare all mockup labels, provider/model availability, sample data, and permissions against actual capabilities.
- [ ] Validate contrast, TalkBack, font scaling, responsive layouts, RTL, and Android permission flows.
- [ ] Verify package, sandbox, terminal, root, and checkpoint functionality without treating design fixtures as live data.
- [ ] Finalize brand vectors from authoritative design sources; screenshot-derived reconstructions are not exact original vectors.
