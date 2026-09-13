# Kaasu — Play Store release checklist

> **Note (2026-08-30):** Historical Play Store submission artifact from before Kaasu's personal-use pivot. Kaasu is not being published; this file is not actively maintained.

Status legend: ✅ done in-repo · ⬜ you must do

## 1. App config (✅ done)
- ✅ `versionName = "1.0.0"`, `versionCode = 1`
- ✅ Launcher icon set from the Kaasu logo (adaptive + legacy densities)
- ✅ Release build type: signing wired via `keystore.properties`; minify/shrink off for v1
- ✅ No SMS/Location/Contacts permissions; notification-listener justification written

## 2. Signing key (⬜ you — do once, keep forever)
```bash
cd kaasu-android
keytool -genkey -v -keystore kaasu-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kaasu
cp keystore.properties.template keystore.properties   # then fill in your passwords
```
- ⬜ Store `kaasu-release.jks` + passwords in a safe place (a password manager). **If lost, you can never update the app.**
- ⬜ (Recommended) Enrol in **Play App Signing** so Google holds the app signing key and your upload key is recoverable.
- `kaasu-release.jks` and `keystore.properties` are git-ignored — never commit them.

## 3. Build the release bundle (after signing is set up)
```bash
cd kaasu-android
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab  (upload this .aab to Play)
```
- ⬜ Test the release build on a device (`./gradlew installRelease` or install the AAB via bundletool) — especially capture, app lock, backup/restore, since release ≠ debug.

## 4. Play Console listing (⬜ you)
- ⬜ Create the app in Play Console (Finance category).
- ⬜ Store listing copy → `docs/archive/play-store/STORE_LISTING.md`.
- ⬜ App icon (512×512) → `docs/archive/play-store/listing/play_icon_512.png`.
- ⬜ Feature graphic (1024×500) — **needs to be created** (e.g. wordmark `kaasu/logo_full.png` on a forest/cream background).
- ⬜ Phone screenshots (min 2, 1080p+) — capture: Dashboard, Transactions, Budgets, Subscriptions, Reports, App lock. (We have QA screenshots to reuse.)
- ⬜ Privacy Policy URL → host `docs/PRIVACY_POLICY.md` and paste the URL.

## 5. Data safety & content rating (⬜ you)
- ⬜ Data Safety form → `docs/archive/play-store/DATA_SAFETY.md` ("No data collected").
- ⬜ Content rating questionnaire (Finance; expected "Everyone").
- ⬜ Target audience: 18+ / not directed at children.
- ⬜ Notification-listener access declaration → justification in `DATA_SAFETY.md`.

## 6. Release
- ⬜ Upload `.aab` to a **Closed/Internal testing** track first; verify install + core flows.
- ⬜ Promote to Production.

## Notes
- Target SDK 35, min SDK 26 — current and compliant.
- Re-verify the v1.0.0 release build on-device once (app lock PIN, backup round-trip) before promoting.
