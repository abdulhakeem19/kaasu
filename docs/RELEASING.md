# Releasing

Kaasu ships as a signed APK attached to a [GitHub Release](../../releases). The release workflow
builds it from a tag, so the binary people download corresponds to a commit anyone can read.

## One-time setup: signing secrets

The release APK has to be signed, and the signing key must never be committed. The workflow reads
it from four repository secrets instead.

> **Why this matters more than usual:** Android identifies an app by its signing key. If the key
> changes, every existing install has to be uninstalled before the new version will go on —
> upgrades break. **Back up `kaasu-release.jks` and its passwords somewhere you will still have
> them in five years.** Losing them means no existing install can ever be updated again.

### Create a keystore (skip if you already have one)

```bash
keytool -genkey -v -keystore kaasu-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias kaasu
```

### Add the secrets

Settings → Secrets and variables → Actions → **New repository secret**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | The keystore file, base64-encoded — see below |
| `KEYSTORE_PASSWORD` | The keystore password |
| `KEY_ALIAS` | The key alias (`kaasu` if you used the command above) |
| `KEY_PASSWORD` | The key password |

To produce `KEYSTORE_BASE64`:

```bash
base64 -i kaasu-release.jks | pbcopy     # macOS
base64 -w0 kaasu-release.jks             # Linux
```

The workflow decodes this to a file, writes a temporary `keystore.properties`, and deletes both
afterwards — the `Remove signing material` step runs even when the build fails.

## Cutting a release

1. **Land everything** you want in the release on `main`, with CI green.
2. **Set the version** in `kaasu-android/app/build.gradle.kts`:
   ```kotlin
   versionCode = 2          // must increase on every release, no exceptions
   versionName = "1.1.0"
   ```
   Android refuses to install an APK whose `versionCode` is not higher than the installed one, so a
   forgotten bump shows up as "app not installed" on a user's phone rather than as a build error.
3. **Update `CHANGELOG.md`** — the release notes point at it.
4. **Tag and push:**
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```
5. The **Release** workflow builds, tests, signs and publishes automatically, attaching the APK and
   its SHA-256 checksum.

To rebuild a release without moving the tag, use **Actions → Release → Run workflow** and pass the
tag name.

## Local release build

You will rarely need this — CI is the source of truth — but to reproduce one locally, create
`kaasu-android/keystore.properties` (git-ignored):

```properties
storeFile=kaasu-release.jks
storePassword=…
keyAlias=kaasu
keyPassword=…
```

Then:

```bash
cd kaasu-android
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Without that file the build still succeeds but produces an **unsigned** APK, which no device will
install.

## Version numbering

Kaasu follows [semantic versioning](https://semver.org) loosely:

- **Patch** (`1.0.1`) — bug fixes, new bank parser patterns
- **Minor** (`1.1.0`) — new capability that doesn't change existing behaviour
- **Major** (`2.0.0`) — anything requiring users to change how they use the app, or a schema change
  that can't be migrated automatically

`versionCode` is a plain integer that only ever goes up, independent of `versionName`.
