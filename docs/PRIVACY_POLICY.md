# Kaasu — Privacy Policy

> **⚠️ Historical document — not accurate for the current build (as of 2026-08-30).** This policy was prepared for a planned Google Play Store submission. Kaasu is no longer being published — it is a personal-use build for the owner only, with no public users. This document is kept for historical reference and does **not** accurately describe the current build's permissions or data access (it predates SMS capture and in-progress Accessibility Service access). For the current, accurate picture, see the root `README.md` and `docs/PERMISSION_STRATEGY.md`.

_Last updated: 22 June 2026_

Kaasu ("the app", "we") is a privacy-first expense tracker for Android. This policy explains, in plain language, what data Kaasu handles and how.

## The short version

**Kaasu collects nothing.** There are no accounts, no cloud servers, no analytics, and no ads. Everything the app records stays on your device.

## What data Kaasu processes — and where it stays

- **Transactions, categories, accounts, budgets, rules, and settings** you create or that Kaasu captures are stored **only in a local database on your device**. They are never transmitted off the device by Kaasu.
- **Notification content**: With your permission, Kaasu's notification listener reads notifications **only from the payment and bank apps you enable** (Settings → Bank sources) plus your messaging app, to detect transactions. Parsing happens entirely on-device. Kaasu does not read notifications from other apps, and does not read your SMS inbox.
- **No personal profile**: An optional display name and avatar you set are stored locally for personalization only.

## What Kaasu does NOT do

- Does not create an account or require sign-in.
- Does not upload, sync, or back up your data to any server. (Backups you create are plain files saved by **you**, to a location **you** choose.)
- Does not contain advertising or third-party analytics/tracking SDKs.
- Does not request location, contacts, camera, microphone, or SMS-reading permissions.

## Permissions Kaasu uses

- **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`): to read payment alerts from the apps you enable, on-device. You grant this manually in Android Settings and can revoke it any time.
- **Post notifications** (optional, Android 13+): only to show you budget/reminder alerts you turn on.

## Your control over your data

- **Export**: Settings → Back up data (a JSON file) or Export transactions (CSV). These files are created locally and shared only if you choose to.
- **Delete**: Settings → Danger Zone → Clear all transactions / Delete all data performs a permanent local wipe.
- Uninstalling the app removes its local database.

## Children

Kaasu is not directed at children under 13 and collects no data from anyone.

## Changes to this policy

If this policy changes, the "Last updated" date above will change and the new version will ship with the app.

## Contact

Questions about privacy? Email **support@kaasu.app**.
