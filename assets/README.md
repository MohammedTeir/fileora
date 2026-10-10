# Fileora design assets

This directory contains the complete asset package supplied for Fileora. The files are preserved as raster PNG/JPG assets; no source artwork was redrawn or overwritten.

## Recommended assets

Use these first in product code, documentation, or release materials:

- `brand/official/` — approved Fileora logo and launcher-icon variants.
  - `logo/` contains full lockups, horizontal lockups, dark-background variants, and symbol-only variants.
  - `launcher_icon/` contains the rounded transparent icon, full-bleed store/adaptive-icon variants, Play Store artwork, and Android-size exports.
- `icons/source/` — crisp source-sized icon files grouped by purpose.
- `icons/sized/` — consistent square exports at 48, 96, 192, and 512 px. Prefer 48 or 96 px for UI use; the 192/512 versions are enlarged raster exports.
- `icons/sized-dark/` — dark-background variants for the icons that need additional contrast treatment.

## Asset groups

| Folder | Contents | Intended use |
|---|---|---|
| `brand/official/` | Derived official logo and launcher variants | Product branding and Android store/launcher work |
| `brand/concepts/` | Six alternative logo directions, with full lockups and symbols | Exploration, comparison, or future brand review; not marked official |
| `icons/source/file_types/` | File-type icons | File lists and type indicators |
| `icons/source/interface_icon_library/` | General Fileora interface icons | Home, activity, file operations, and status UI |
| `icons/source/navigation_actions/` | Navigation and folder actions | Back, folder, refresh, share, add, and similar controls |
| `icons/source/settings_system/` | Settings, privacy, storage, and system actions | Settings and permissions UI |
| `icons/source/task_actions/` | Task lifecycle and AI-operation actions | Approval, retry, pause, resume, conflict, and task UI |
| `icons/source/ui_controls/` | Selection, filtering, sorting, and view controls | List/grid/search/filter controls |
| `icons/sized/` | Consistent multi-size versions of the icon groups | Deterministic size selection for UI or mockups |
| `icons/sized-dark/` | Contrast-adjusted dark-background versions | Dark surfaces and dark-theme references |
| `originals/official/` | Byte-identical supplied official logo and launcher originals | Source archive and provenance; do not edit |
| `originals/reference-sheets/` | Supplied reference sheets | Design provenance and visual reference; not runtime assets |
| `originals/previous-package/` | Earlier `Fileora_Extracted_Assets.zip` package | Historical archive; do not use as the current source |

## Official versus exploratory logos

The archive README says that the six logo concepts are not marked official. The approved artwork is therefore kept separately under `brand/official/`; the alternatives remain under `brand/concepts/` so the design history is not lost.

## Important limitations from the supplied package

- All assets are raster files; vector source files were not supplied.
- The full lockup wordmark/tagline is dark navy and is intended for light backgrounds. Use the dark-background lockups on dark surfaces.
- The sized 192 px and 512 px exports are enlarged raster versions. Prefer 48 px or 96 px when a crisp small UI icon is needed.
- Some JPG-sourced control/task icons retain slight compression softness.
- The package README is preserved verbatim as [`ARCHIVE_README.md`](ARCHIVE_README.md).

## Current app integration

The approved 512 px launcher export is now integrated as the Android launcher foreground in `app/src/main/res/drawable-nodpi/fileora_launcher.png`. The standard and round adaptive launcher resources both use it. A Phase 1 catalog of 20 high-value 48 px icons is also packaged under `app/src/main/res/drawable-nodpi/`, with four supplied dark-theme overrides under `drawable-night-nodpi/`. The Android package/application ID remains unchanged so existing Fileora installations can upgrade without losing app data.

See [`docs/ASSET_USAGE.md`](../docs/ASSET_USAGE.md) for the exact source-to-resource mapping and the recommended future uses for the icon library.
