# Synapse design asset masters

The full-resolution Synapse v3 master is a **263-file** source archive (approximately 44 MB uncompressed), with **31** navigation-ordered screen references.

To import the master without changing Android runtime resources:

```bash
git checkout main
git pull --ff-only origin main
python3 scripts/import_synapse_v3_assets.py ~/Downloads/Synapse_Production_Candidate_Assets_v3_Full_Resolution.zip
git add assets/synapse/v3
git commit -m "assets: import Synapse v3 full-resolution master"
git push origin main
```

This copies the archive's contents into `assets/synapse/v3/`, including `android/`, `brand/`, `icons/`, `references/`, `tokens/`, `manifest.json`, and QA documents. It preserves original file bytes and does not install launcher candidates into `app/src/main/res`.

The importer checks archive integrity and requires exactly 31 reference images. The reference PNGs are design handoff files, **not** assets that should be packaged in the APK.

For a large Git history, consider Git LFS for the 31 PNG references; use a repository policy appropriate to your team before adding LFS pointers.
