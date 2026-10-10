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

## Not integrated as runtime UI yet

The app still uses Compose Material vector icons for navigation and actions such as Home, Activity, Settings, Add, Delete, Warning, Undo, and Expand. That is intentional:

- Material vectors remain crisp at every density and support Compose tinting and accessibility consistently.
- The supplied PNG icon library is raster artwork, so replacing every vector would require choosing density, tint, and dark-theme behavior for each control.
- The supplied library is nevertheless ready for selective use in richer file lists, task cards, empty states, onboarding, or branded illustrations.

Recommended mappings for future selective integration:

| App feature | Asset group | Candidate assets |
|---|---|---|
| File list type indicators | `assets/icons/source/file_types/` | `pdf`, `image` (when added), `video` (when added), `spreadsheet`, `word_document`, `archive`-style types |
| Home/task status illustrations | `assets/icons/source/interface_icon_library/` | `ai_assistant`, `ai_sparkle`, `success`, `error`, `warning`, `history`, `undo` |
| Folder and SAF actions | `assets/icons/source/navigation_actions/` | `folder`, `folder_open`, `folder_tree`, `add`, `refresh`, `checkmark`, `close` |
| Settings and privacy | `assets/icons/source/settings_system/` | `privacy_lock`, `eye`, `eye_off`, `notifications`, `storage_cleanup`, `restore_file` |
| Task progress and recovery | `assets/icons/source/task_actions/` | `review_changes`, `apply_changes`, `retry`, `resume_task`, `pause_task`, `partial_success`, `file_conflict` |
| Search, sort, and view controls | `assets/icons/source/ui_controls/` | `clear_search`, `filter_clear`, `list_view`, `grid_view`, `sort_ascending`, `sort_descending`, `view_details` |

For a raster icon in Compose, use `painterResource()` with the appropriate `drawable-nodpi` or density-specific resource. Use the 48 px or 96 px exports from `assets/icons/sized/` for small UI elements; use `assets/icons/sized-dark/` only on dark surfaces where the contrast-adjusted version is needed.

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
