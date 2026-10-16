# Isolated v3 artwork

The user explicitly approved extraction of isolated artwork pixels from the approved references on 2026-10-16. `scripts/extract-v3-artwork.py` creates these PNGs and byte-identical Android resources. `manifest.json` records source paths, source hashes, exact crop coordinates, dimensions and output hashes.

These are cropped raster design assets, not original third-party vector masters. The original references and supplied branding/icons are unchanged. Only illustrations, photos, logos, and the embedded screenshot thumbnails depicted in the Monitor image-content panel are extracted. No screen, card, toolbar, form, or interactive control is used as a runtime image. All surrounding text and UI remain Compose.
