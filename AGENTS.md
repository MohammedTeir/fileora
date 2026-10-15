# Repository instructions

## App identity

Use **Synapse** as the app name in UI, prompts, documentation, and new export
filenames. Preserve the existing application ID, database, preference keys,
and credential aliases for upgrade compatibility. Settings import must continue
to accept legacy backup files.

## Asset policy

The approved `assets/synapse/v3` package is the visual authority for the 31-screen UI.
Use its supplied branding and SVG icon concepts through Android runtime resources;
keep original source assets intact. `scripts/import-v3-icons.py` reproduces the
VectorDrawables. Never package design-reference screenshots in the APK or use a
screenshot as an interface. The raster references and reconstructed icon concepts
have different provenance; do not claim pixel-perfect original vectors.
