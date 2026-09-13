# Kaasu — Account & Data Deletion

> **⚠️ Historical document — not accurate for the current build (as of 2026-09-08).** This document was prepared for a planned Google Play Store submission. Kaasu is no longer being published — it is a personal-use build for the owner only, with no public users. It is kept for historical reference and does **not** accurately describe the current build's permissions or data access (it predates SMS capture, statement import, and Accessibility Service capture). For the current, accurate picture, see the root `README.md` and `docs/PERMISSION_STRATEGY.md`.

_Last updated: 23 June 2026_

This page explains how to delete your data in **Kaasu**, a privacy-first expense tracker for Android.

## There is no account to delete

Kaasu has **no sign-up, no login, and no servers**. You never create an account, and nothing about you is ever uploaded — every transaction, category, budget, and setting is stored **only in a local database on your device**.

Because there is **no cloud account and no server-side data**, there is nothing for us to hold or delete on our side, and no request or email is required. You are always in full control, and deletion is instant and self-service.

## How to delete your data (in the app)

Everything Kaasu stores can be permanently erased from inside the app:

1. **Delete everything** — Open Kaasu → **Settings → Danger Zone → "Delete account & wipe device"** → confirm. This permanently deletes **all** transactions, categories, accounts, budgets, rules, merchant labels, and personalization settings from your device.
2. **Delete transactions only** — **Settings → Danger Zone → "Clear all transactions"** removes your transaction history while keeping your default categories.
3. **Uninstall the app** — Removing Kaasu from your device deletes its entire local database along with it.

All of the above are **immediate and permanent** (hard delete) — there is no soft delete and no recovery.

## What gets deleted

A full wipe removes everything Kaasu has stored on your device:

- Transactions (captured and manually added)
- Categories, rules, and merchant labels
- Accounts and budgets
- Your profile name, currency, and app settings
- The app lock PIN

## Backups you created

If you used **Settings → Back up data** to export a backup file (JSON) or **Export transactions** (CSV), those files were saved by you, to a location you chose (e.g. your Files or Drive). They live outside the app — to delete them, remove those files yourself. Kaasu has no access to them.

## Data retention

Kaasu retains **no data on any server** because it never stores any there. On-device data is kept only until you delete it or uninstall the app.

## Contact

Questions about data deletion? Email **support@kaasu.app**.
