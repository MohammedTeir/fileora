# Synapse asset usage

The approved v3 package supplies the branding and all 38 runtime icon concepts. See the [asset integration audit](SYNAPSE_ASSET_INTEGRATION.md) for source-to-resource mappings, mask checks, and limitations.

Original SVGs and full-resolution references remain under `assets/synapse/v3/`. The reference screenshots are review inputs and are excluded from the APK. Runtime icons are real VectorDrawables used by Compose.

Run `python scripts/import-v3-icons.py` to reproduce the vectors. Native captures use `./gradlew :app:testDebugUnitTest --tests "*SynapseVisualParityTests"`; generate comparisons with `python scripts/compare-v3-screens.py` (Pillow required).
