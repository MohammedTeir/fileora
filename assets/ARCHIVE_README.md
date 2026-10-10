# Extracted Assets

Assets extracted from 7 reference sheets (2 JPG, 5 PNG).

- Source images inspected: 7
- Individual assets identified: 122 (76 icons + 40 library icons + 6 logo concepts)
- Files delivered: 128 PNG files (6 logo concepts delivered as 12 files: full lockup + symbol only)
- Unrecoverable: 0
- Output: transparent PNG, at the pixel size shown in the sheets (raster, not vector)

## Categories
- `file_types/` (12): apk, calendar_file, code_file, database_file, ebook, font_file, json_file, pdf, presentation, spreadsheet, unknown_file, word_document
- `interface_icon_library/` (40): ai_assistant, ai_sparkle, android, archive, audio, cloud, compressed, copy, create_folder, delete, document, downloads, error, favorites, files, filter, help, history, home, image, loading, more_options, move, organize, permissions, recent_files, rename, search, security, select_all, settings, sort, storage, success, tasks, text_file, undo, upload, video, warning
- `navigation_actions/` (16): add, back, cancel, checkmark, close, edit, external_link, folder, folder_open, folder_tree, forward, info, open_external, refresh, share, up_one_level
- `settings_system/` (16): accessibility, app_information, backup, dark_theme, eye, eye_off, light_theme, microphone, network_offline, notifications, permanent_delete, privacy_lock, restore_file, storage_cleanup, sync, trash_bin
- `task_actions/` (16): ai_analyze, ai_suggestions, apply_changes, cancel_task, compare_files, duplicate_files, file_conflict, partial_success, pause_task, replace_file, resume_task, retry, review_changes, skip, stop_task, task_queue
- `ui_controls/` (16): checkbox_checked, checkbox_unchecked, chevron_down, chevron_right, clear_search, collapse, date_filter, expand, filter_clear, grid_view, list_view, preview, select_none, sort_ascending, sort_descending, view_details
- `logos/full_lockups/` (6): 01_ai_folder_primary_lockup, 02_letter_f_lockup, 03_intelligent_folder_lockup, 04_file_stack_lockup, 05_ai_integration_lockup, 06_abstract_symbol_lockup
- `logos/symbols_only/` (6): 01_ai_folder_primary_symbol, 02_letter_f_symbol, 03_intelligent_folder_symbol, 04_file_stack_symbol, 05_ai_integration_symbol, 06_abstract_symbol_symbol

## Notes and limitations
- Names come from the labels printed in the sheets. Concept 01 is labeled "primary" in the logo sheet; nothing is marked official.
- Three PNG icon sheets had a checkerboard baked into the pixels, so backgrounds were removed by edge flood-fill with edge decontamination.
- Library icons (interface_icon_library) came from soft tiles with drop shadows. Icon-only crops were made and the shadow removed; search and settings had their pale interiors cleared.
- The logo wordmark and tagline are dark navy, so they will not show on dark backgrounds. Use light backgrounds.
- JPG-sourced icons (ui_controls, task_actions) have slight compression softness.
- Specks on microphone, open_external, refresh, list_view and grid_view were removed in a cleanup pass.
- Some names overlap with the earlier Fileora_Extracted_Assets.zip (e.g. home, settings, search). That archive was left untouched.

## Sized icons and dark/light handling
- `icons_sized/<category>/<48|96|192|512>px/`: all 116 icons (76 + 40 library) on a consistent square transparent canvas, centered, with about 6% padding per side (largest side = 88% of the canvas). Made from the extracted rasters, so 192 and 512 px versions are upscaled and slightly soft. Use 48 and 96 for crisp results.
- `icons_sized_dark_bg/<size>px/`: 12 dark-background versions. `dark_theme` and `network_offline` get a thin light rim; 10 thin deep-blue library glyphs (history, undo, select_all, loading, search, settings, sort, downloads, upload, home) are brightened. Use these on dark UIs instead of the standard ones.
- Light backgrounds: all icons read clearly on white; no light variants needed.
- Dark backgrounds: all other icons read clearly on near-black.
- `logos/full_lockups_for_dark_bg/`: the six concept lockups with white wordmark and tagline (blue accents lightened for contrast). Symbol-only files work on both.

## Official asset variants (`official_variants/`)
Derived from your supplied official logo and launcher icon. The originals are untouched in `originals/official/`.
- `launcher_icon/launcher_icon_rounded_transparent.png`: checkerboard removed, real transparency, rounded shape.
- `launcher_icon/launcher_icon_full_bleed_square.png` and `play_store_512_full_bleed.png`: square with corners filled, for Play Store and adaptive-icon use (Android applies its own mask).
- `launcher_icon/android_sizes/`: rounded transparent icon at 48 (mdpi), 72 (hdpi), 96 (xhdpi), 144 (xxhdpi), 192 (xxxhdpi) and 512 px.
- `logo/`: full lockup (light bg, trimmed, and white-text version for dark bg), symbol only (native and 512 px wide), and horizontal lockup (light and dark). The horizontal layout is composed from the official artwork without redrawing it.
- Not included: separate adaptive-icon foreground and background layers, because the launcher design cannot be split cleanly without redrawing it.

## Originals (unmodified copies of supplied files)
- `originals/reference_sheets/`: the 7 source sheets.
- `originals/official/`: your official logo and Android launcher icon, byte-identical.
- `originals/previous_package/Fileora_Extracted_Assets.zip`: your earlier archive.
