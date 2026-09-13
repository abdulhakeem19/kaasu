# Kaasu — Release notes

> **Note (2026-09-08):** Historical Play Store submission artifact from before Kaasu's personal-use pivot. Kaasu is not being published; this file is not actively maintained.

## v1.0.0 (versionCode 1) — first closed test

### Play Console "What's new" field (en-US, ≤500 chars — paste as-is)

```
Welcome to the first Kaasu test build 🎉

Kaasu turns your UPI & bank notifications into a spending log — fully on-device. No login, no cloud, no ads.

In this build:
• Auto-capture from GPay/PhonePe/Paytm + bank apps
• Dashboard, budgets (overall + per-category)
• Subscriptions with renewal dates
• App lock (PIN + optional fingerprint)
• Backup, restore & CSV export
• Mark/restore duplicate transactions

Spotted a bug? Settings → Help & support. Thanks for testing!
```

> If the Console rejects it for length, drop the bullet sub-items — the field caps at 500 characters per language.

### Tester brief (put this in the testing instructions / feedback email, not the 500-char field)

**What Kaasu is:** a privacy-first expense tracker that reads UPI/bank *notifications* on your phone and logs them locally. Nothing leaves your device.

**One-time setup:**
1. Open the app → onboarding → grant **Notification access** when prompted (Android Settings → Notification access → enable Kaasu).
2. (Optional) set a budget and an app-lock PIN.

**Please test and report on:**
- Capture accuracy — are real debits/credits logged with the right amount, merchant and direction? Are promos/offers correctly ignored?
- Any **duplicate** entries (and try Settings → Duplicates → mark/restore).
- Budgets (overall + per-category), Subscriptions (renewal dates, add manually).
- App lock unlock on launch; Backup → wipe → Restore round-trip.

**Known limitations in this build:**
- Notification capture must be enabled manually (Android requirement).
- Some bank/app notification formats may parse imperfectly — please share the app + a description (no need to send the raw text).

**Feedback:** Settings → Help & support (or reply to the tester invite).
