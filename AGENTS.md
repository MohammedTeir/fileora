# Repository instructions

## App identity

Use **Synapse** as the app name in UI, prompts, documentation, and new export
filenames. Preserve the existing application ID, database, preference keys,
and credential aliases for upgrade compatibility. Settings import must continue
to accept legacy backup files.

## Asset policy

Only the existing Synapse launcher icon may use artwork from `assets/`.
Do not copy, convert, trace, or derive other application resources from that
folder. Use Compose Material icons or independently authored Android resources
for UI changes. The launcher artwork may also appear on the app launch screen.
Keep the `assets/` archive intact; it is not a runtime resource directory.
