# Kaasu Permission Strategy

## Personal-use pivot (2026-08-30)

This document was originally written for a Play Store-bound app, where the driving constraint on every permission decision was Play policy risk. Kaasu is now a **personal-use build for the repo owner only** — it will not be published, so that constraint no longer applies. SMS access has since been added (implemented on `feature/sms-capture`: live `SmsReceiver` + one-time historical `SmsBackfillWorker` inbox scan, both tested against the owner's real device and SMS inbox), and Accessibility Service access is being added (in progress on a separate branch, not yet complete). See "Future SMS Permission Decision" at the bottom of this doc, which anticipated exactly this scenario.

The permission **philosophy** below — ask for the minimum needed, be transparent, make it easy to revoke — still holds. What changed is which permissions are "the minimum needed": for a single, consenting owner, direct SMS reading and on-screen transaction history are legitimate, deliberate choices, not policy violations to avoid.

## Permission Philosophy

Kaasu should ask for the minimum possible access needed to deliver automatic expense tracking.

The product promise is privacy. Permission handling must therefore be clear, minimal, and respectful.

## Permission Plan (current, personal build)

## Required

### 1. Notification Listener Access

Used to detect transaction alerts from UPI, bank, wallet, and card apps.

This is not a normal runtime permission dialog. The user must manually enable Kaasu in Android notification access settings.

### 2. SMS Access (`READ_SMS`, `RECEIVE_SMS`) — added post-pivot

Used for live SMS transaction capture (`SmsReceiver`) and a one-time historical inbox backfill (`SmsBackfillWorker`), on `feature/sms-capture`. Previously listed under "Avoid" below when this was a public-app document; that constraint was Play-policy-driven and no longer applies to a personal build. The same on-device-only, no-upload handling rules apply to SMS content as to notification content.

### 3. Accessibility Service — in progress, not yet complete

Planned to read UPI apps' (GPay/PhonePe) own on-screen transaction history directly, for cases notifications and SMS both miss. Being implemented on a separate branch as of this writing; treat as underway, not finished.

## Optional

### 4. POST_NOTIFICATIONS

Use only if Kaasu sends reminders, monthly summaries, or budget alerts on Android versions that require notification runtime permission.

## Still Avoided

These remain avoided regardless of the pivot — the pivot changes the calculus for SMS/Accessibility specifically (they now serve the app's core purpose for a consenting owner), it is not a blanket relaxation:

```text
SEND_SMS
READ_CALL_LOG
READ_CONTACTS
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
CAMERA
RECORD_AUDIO
READ_MEDIA_IMAGES
```

## Why SMS Was Originally Avoided (historical)

Google Play treats SMS and Call Log permissions as high-risk or sensitive. Apps that do not qualify must remove them from the manifest. This was the reasoning for keeping Kaasu notification-first in the original MVP scope, when a Play Store submission was still the plan. It no longer applies — see "Personal-use pivot" above.

Official reference:
https://support.google.com/googleplay/android-developer/answer/10208820

## Notification Access Onboarding

Never send users directly to Android settings without explaining why.

## Permission Explanation Copy

```text
Kaasu needs notification access to detect transaction alerts from UPI, bank, card and wallet apps.

Your notifications are processed on your phone.
Kaasu does not upload your transaction data.
Kaasu ignores non-transaction notifications whenever possible.
You can disable access anytime from Android Settings.
```

## Permission Screen Requirements

The permission education screen must explain:

- What access is needed
- Why it is needed
- What data is processed
- Where data is stored
- What Kaasu does not do
- How to disable access

## Permission Flow

```text
Welcome
→ Privacy promise
→ Notification access explanation
→ Open Android Settings
→ User enables Kaasu
→ User returns to app
→ App checks permission status
→ Continue to budget setup/dashboard
```

## Disabled Permission State

If notification access is disabled after onboarding:

Show:

```text
Automatic tracking is paused.
Enable notification access to continue detecting transaction alerts.
Manual expense entry still works.
```

Actions:

```text
Open Settings
Continue Manually
```

## Permission Status Checks

Check notification access:

- During onboarding
- On app resume
- Before showing auto-tracking dashboard state
- From settings screen

## Data Handling Rules

When notification is received:

- Read only notification title/text/subtext if available.
- Filter for finance/payment-related alerts.
- Avoid saving unrelated notification content.
- Avoid logging raw notification text in release builds.
- Never send notification content to external servers in MVP.
- Give the user delete/export controls.

## Sensitive Data Handling

Kaasu may process financial information locally. The privacy policy and Play Store Data Safety section must be accurate and consistent.

Official references:

- Google Play User Data Policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469

## Manifest Rules

MVP manifest should include only necessary service declarations and permissions.

Avoid adding unused permissions because Play Store review and user trust can both be affected.

## NotificationListenerService Declaration

Example direction:

```xml
<service
    android:name=".notification.listener.KaasuNotificationListenerService"
    android:label="@string/app_name"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

Confirm current Android requirements during implementation.

## Trust Rules

- Do not surprise the user.
- Do not hide permission purpose.
- Do not overclaim privacy.
- Do not say “end-to-end encrypted” unless cloud sync exists and is implemented correctly.
- Say “stored locally on your device” for MVP.
- Make data deletion easy.

## Future SMS Permission Decision — RESOLVED (2026-08-30)

This section originally conditioned SMS permission on "Google Play eligibility researched carefully" and "the core app function clearly qualifies." That condition is moot: Kaasu pivoted to personal-use only and will not be submitted to Google Play at all, so there is no eligibility to research and no rejection risk to plan a fallback for. SMS access has accordingly been added, on `feature/sms-capture`. The original conditions are kept below for historical record, not because they still gate anything.

Original conditions (superseded):

- Notification-based capture is not enough.
- Google Play eligibility is researched carefully.
- The core app function clearly qualifies.
- A proper permission declaration is prepared.
- There is a fallback plan if rejected.
