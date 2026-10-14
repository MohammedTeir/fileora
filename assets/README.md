# Synapse asset archive

This directory preserves the artwork supplied under the former Fileora name.
Only the existing launcher icon is used by Synapse. UI icons use Material
vectors; do not copy, convert, trace, or derive other runtime resources from
this archive. See [the repository asset policy](../AGENTS.md) and
[the current integration guide](../docs/ASSET_USAGE.md).

## Current app integration

The retained 512 px launcher export is integrated as
`app/src/main/res/drawable-nodpi/synapse_launcher.png`. The standard and round
adaptive launchers and the launch screen use the same unchanged artwork.
The former UI PNGs and dark-theme overrides are no longer packaged in the app.

## Archive layout

- `brand/official/` — original logo and launcher variants; only the retained launcher is integrated.
- `brand/concepts/` — historical branding concepts.
- `icons/source/`, `icons/sized/`, `icons/sized-dark/` — archived UI artwork, not for runtime use.
- `originals/official/` — supplied original artwork, preserved unchanged.
- `originals/reference-sheets/` — historical design references.
- `originals/previous-package/` — earlier supplied packages.

Original filenames and the supplied [archive README](ARCHIVE_README.md) remain
unchanged to preserve provenance. References to the former name in those
historical materials are intentional.
