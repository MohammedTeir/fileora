# UX rework

Problems found and what changed:

- One endless settings-style page -> 3 tabs: Home (task), Activity (history + repeating tasks), Settings.
- Approval and question dialogs never appeared: the background service waited on its own private channel.
  They now go through `TaskBridge`, so the dialog shows on top of any tab, and the notification buttons use the same path.
- No `POST_NOTIFICATIONS` permission (Android 13+), notifications could not be tapped, the "finished" notification
  vanished when the service stopped, and approvals were silent. Now: 3 channels, tap opens the app, separate result notification.
- Messages were a card at the top of a long page, so they were off-screen after tapping Run. Now a snackbar.
- Raw ids and enums (task ids, FAILED_AFTER_INVESTIGATION, file.move, bytes, tokens) -> plain language in `Friendly.kt`.
- Typing model names / hours -> model dropdown (auto-suggested after saving a key), frequency choices.
- Model is now remembered per provider (it used to be shared, which broke after switching provider).
- Light/dark theme, edge-to-edge, app icon, notification icon.
- The most recent finished task can be reconstructed from Room audit history and shown on Home after an app restart.
- Bulk requests such as “move all PDFs” use one bounded, verified `file.move_matching` operation instead of one AI turn per file, reducing token-limit failures and keeping the confirmation understandable.
- AI closing notes are rendered as bounded plain text: common backticks, bold markers, headings, links, and Markdown bullets are cleaned before display, while the engine’s factual result remains primary.

## Verification and remaining work

- `./gradlew :app:assembleDebug` passes locally with Android API 35.
- JVM tests cover the new plain-language formatting, model suggestion, setup checklist, and status translation helpers.
- Screens and notification flows still need visual and interaction validation on a physical Android device.
- Bulk move confirmation and undo still need physical-device validation with a real Download/Documents provider.
- Text remains hardcoded in English; Arabic/localized string resources are not included in this pass.
