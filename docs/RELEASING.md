# Releasing (direct distribution)

Two workflows live in `.github/workflows/`:

| Workflow | When it runs | What it does |
|---|---|---|
| `ci.yml` | every push to `main` and every pull request | runs the 89 core tests, builds debug APKs, uploads them as a 7-day artifact. Uses no secrets. |
| `release.yml` | you push a tag like `v0.1.0`, or run it by hand | runs the tests again, builds **signed** release APKs for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` plus a universal APK, verifies the signatures, writes `SHA256SUMS.txt`, and (for tags) publishes a GitHub Release |

> The app is pure Kotlin right now, so the per-architecture APKs are functionally identical. They start to differ when native libraries (FFmpeg, proot) are added. Most phones need `arm64-v8a`; the universal APK works everywhere but is larger.

## One-time setup

### 1. Create your signing key (do this once, on your own machine)

```bash
keytool -genkeypair -v -keystore release.jks -alias deviceagent \
  -keyalg RSA -keysize 4096 -validity 10000
```

You will choose a store password and a key password. **Back up `release.jks` and both passwords somewhere safe and offline.** If you lose the key you can never update an installed app, and anyone who gets it can publish "updates" that Android will accept. Never commit it (`.gitignore` already blocks `*.jks`).

### 2. Add the secrets in GitHub

Repository → Settings → Secrets and variables → Actions → New repository secret. Or with the GitHub CLI:

```bash
base64 -w0 release.jks | gh secret set KEYSTORE_BASE64        # macOS: base64 -i release.jks | tr -d '\n' | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD      # the store password
gh secret set KEY_ALIAS              # deviceagent
gh secret set KEY_PASSWORD           # the key password
```

### 3. Pin your certificate (recommended)

Get the certificate fingerprint and store it as a fourth secret. The release job then **fails** if the APK is ever signed with a different key:

```bash
keytool -list -v -keystore release.jks -alias deviceagent | grep "SHA256:"
gh secret set EXPECTED_CERT_SHA256   # paste the hex value (colons are fine)
```

Publish the same fingerprint on your download page so users can check it.

## Making a release

```bash
git tag v0.1.0
git push origin v0.1.0
```

The workflow publishes a Release containing the APKs and `SHA256SUMS.txt`. `versionCode` is set automatically to the workflow run number, so it always increases (Android refuses to install an update with a lower one). Users can verify a download with `sha256sum -c SHA256SUMS.txt`.

## Hardening worth doing

- **Protect the `main` branch and tags**: require pull-request review, and restrict who can push `v*` tags. Anyone who can push a tag can trigger a signed release.
- **Use an environment with required approval**: put the four signing secrets in a GitHub Environment named `release` with "required reviewers", then add `environment: release` to the `build-and-sign` job. Releases then need a human click.
- **Action pinning:** CI and release workflow actions are pinned to immutable commit SHAs with version comments. Review and update them deliberately when Dependabot proposes a newer trusted release.
- The workflow decodes the keystore to the runner's temp folder (outside the workspace) and deletes it at the end of the job, even on failure.

## What has and has not been verified

The workflow files are valid YAML and actions are pinned, but the Android workflow has **not run in this sandbox** because no Android SDK is installed. Run `ci.yml` on GitHub before creating a release tag; only create a tag once CI is green.
