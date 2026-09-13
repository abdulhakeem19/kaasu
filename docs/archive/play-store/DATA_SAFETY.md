# Kaasu — Play Console Data Safety form answers

> **⚠️ Historical document — not accurate for the current build (as of 2026-08-30).** This document was prepared for a planned Google Play Store submission. Kaasu is no longer being published — it is a personal-use build for the owner only. This document is kept for historical reference and is **not** an accurate description of the current build's permissions or data access (it predates SMS capture and in-progress Accessibility Service access, and its "no data collected" / "no SMS-reading permissions" answers no longer hold). See the root `README.md` and `docs/PERMISSION_STRATEGY.md` for the current, accurate picture.

Fill the Data Safety section exactly as below. Kaasu collects nothing, which keeps this simple.

## Data collection & sharing
- **Does your app collect or share any of the required user data types?** → **No.**
  - Kaasu stores everything locally and transmits nothing off the device. There is no data "collected" in the Play sense (collected = sent off-device).

If the Console still asks per-category, answer **No / Not collected** for every category (Location, Personal info, Financial info, Messages, Contacts, App activity, etc.).

## Security practices
- **Is all user data encrypted in transit?** → Not applicable (no data leaves the device). If a yes/no is forced, answer **No data is transferred**, and use the "no data collected" declaration.
- **Do you provide a way for users to request data deletion?** → **Yes** — Settings → Delete all data performs a local wipe; uninstalling also removes all data.

## Notification access declaration (Play "Notification Listener" / sensitive access)
Because Kaasu uses `BIND_NOTIFICATION_LISTENER_SERVICE`, Play may require a justification. Use:

> Kaasu is an on-device expense tracker. It uses notification access solely to read payment/transaction alerts from the bank and UPI apps the user explicitly enables, to record those transactions locally. Notification content is parsed on-device and never transmitted, sold, or shared. The app reads no notifications from non-enabled apps and does not access the SMS inbox.

- Core functionality: **Yes** — reading transaction notifications is the app's primary purpose.
- Data handled from notifications: stored **on-device only**, not shared.

## Permissions justification (for the listing / review notes)
- `BIND_NOTIFICATION_LISTENER_SERVICE` — capture transaction notifications on-device (core feature).
- `POST_NOTIFICATIONS` — optional budget/renewal reminders.
- No SMS / Call Log / Location / Contacts permissions are requested (Kaasu deliberately avoids the high-risk SMS/Call-Log policy path).

## Ads
- Contains ads? → **No.**
