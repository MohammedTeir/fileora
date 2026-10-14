# Fileora asset usage

## Launcher artwork

The only supplied artwork integrated into the app is
`app/src/main/res/drawable-nodpi/fileora_launcher.png`, originally copied from
`assets/brand/official/launcher_icon/android_sizes/ic_launcher_512.png`.
The standard and round adaptive launcher icons use this resource for their
foreground and monochrome layers. The app launch screen displays the same icon.

## UI icons

`FileoraIcon` and `FileoraIconName` in
`app/src/main/kotlin/dev/deviceagent/app/ui/FileoraIcons.kt` render Compose
Material vectors, with theme-aware tinting and an automatically mirrored Undo
icon. The former supplied UI PNGs and their dark variants have been removed
from Android resources. Visible labels and accessibility descriptions remain.

## Policy for future changes

Only launcher artwork may come from `assets/`. Do not copy, convert, trace, or
derive other application resources from that folder. Use Compose Material icons
or independently authored Android resources. See the root `AGENTS.md`.
Keep the asset archive intact; none of its other artwork is packaged into the app.

## Validation

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

These checks do not render the UI; visual device validation is still needed.
