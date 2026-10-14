# Fileora asset usage

This document tracks the supplied design assets that are currently used by the Android app and the assets intentionally kept available for later UI work.

## Integrated now

| App location | Supplied asset | How it is used |
|---|---|---|
| `app/src/main/res/drawable-nodpi/fileora_launcher.png` | `assets/brand/official/launcher_icon/android_sizes/ic_launcher_512.png` | Approved Fileora launcher artwork, copied into an Android `drawable-nodpi` resource so Android does not rescale it by density. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | `assets/brand/official/launcher_icon/android_sizes/ic_launcher_512.png` | The standard adaptive launcher icon uses the approved artwork as its foreground and monochrome source. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | `assets/brand/official/launcher_icon/android_sizes/ic_launcher_512.png` | The round launcher icon uses the same approved artwork. |
| `app/src/main/res/drawable-nodpi/fileora_icon_*.png` | 20 selected 48 px exports from `assets/icons/sized/` | Phase 1 runtime catalog for Home, navigation, Settings, approval, privacy, and task-status UI. |
| `app/src/main/res/drawable-night-nodpi/fileora_icon_{home,history,settings,undo}.png` | Matching files from `assets/icons/sized-dark/48px/` | Automatic dark-theme contrast variants for the four supplied icons that have dedicated dark exports. |

The reusable Compose API is `FileoraIcon` with `FileoraIconName` in `app/src/main/kotlin/dev/deviceagent/app/ui/FileoraIcons.kt`. Phase 1 now uses the catalog for bottom navigation, Home notifications/status/privacy/start controls, Settings folder actions, and the change-approval risk indicator. Important actions still retain visible text labels.

The existing application ID, package namespace, Room database name, and secret aliases were not changed. This keeps the app upgrade-compatible for existing installations.

## Policy for future changes

Do not use files from `assets/` for future implementation work. Do not copy,
convert, trace, or derive new application resources from that folder. Use the
existing Compose Material icon dependency or independently authored Android
resources instead. This rule is also recorded in the root `AGENTS.md`.

The resources already integrated above may remain in use. No replacement of
existing icons or launcher artwork is required. Keep the asset archive intact.

## Brand assets kept for design/reference use

- `assets/brand/official/` contains the approved logo and launcher variants.
- `assets/brand/concepts/` contains six alternative logo concepts. The archive README does not mark these as official, so they are not used in the app.
- `assets/originals/official/` contains byte-identical supplied originals and must remain unedited.
- `assets/originals/reference-sheets/` contains design references, not runtime resources.
- `assets/originals/previous-package/` contains the earlier asset package for history only.

## Validation

The launcher resource is a valid 512 × 512 RGBA PNG. After changing launcher resources, validate with:

```bash
./gradlew :agent-core:check
./gradlew :app:assembleDebug   # requires Android SDK API 35
```

The core suite does not render Android UI. A real-device check is still recommended for adaptive-icon masking and the launcher’s monochrome rendering.
