# Native visual evidence

Open [index.html](index.html) in a browser to compare each of the 31 approved references with its actual Compose capture and adjustable overlay. If opening local files is restricted, serve the repository with `python -m http.server 8000 --bind 127.0.0.1`, then open `/docs/visual-parity/index.html`.

`actual/` contains lossless native Android canvas captures. The viewer reuses the original reference assets without copying them into the application. `metrics.json` contains RGB difference diagnostics; these are not perceptual scores or acceptance thresholds.

Run `python scripts/compare-v3-screens.py --publish` after the native screenshot tests to refresh these captures. Generated PNG triptychs also appear under `app/build/outputs/visual-parity/comparisons/`. [The parity report](../SYNAPSE_VISUAL_PARITY.md) lists remaining mismatches.
