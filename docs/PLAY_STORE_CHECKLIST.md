# Kaasu Play Store Checklist

## 1. Pre-Submission Product Review

- [ ] App name finalized
- [ ] Package name finalized
- [ ] App icon ready
- [ ] Feature graphic ready
- [ ] Screenshots ready
- [ ] Short description ready
- [ ] Full description ready
- [ ] Privacy policy live
- [ ] Support email active
- [ ] App category selected
- [ ] Content rating completed
- [ ] Release AAB signed
- [ ] Internal testing completed

## 2. App Identity

### App Name

```text
Kaasu: Private Expense Tracker
```

### Short Description

```text
Track UPI and bank spends privately. No ads, loans, signup or cloud.
```

### Core Tagline

```text
Your money. Your phone. No cloud.
```

## 3. Full Description Draft

```text
Kaasu is a private expense tracker built for Indian users who want simple spending clarity without ads, loans, bank login or cloud tracking.

Kaasu reads transaction notifications from UPI, bank, wallet and card apps locally on your phone and organizes your spending automatically.

Key features:
• Automatic transaction detection from notifications
• Works with UPI, bank, card and wallet alerts
• Monthly spending dashboard
• Category-wise breakdown
• Manual expense entry
• Custom categories
• Smart rules for merchants
• Monthly reports
• CSV export
• Delete all data anytime

Privacy-first:
• No account required
• No bank login
• No ads
• No loans
• No selling data
• Data stays on your phone in MVP

Kaasu is not a banking, lending, investment or payment app. It is a simple private tracker to help you understand where your money goes.
```

## 4. Permissions Checklist

## Required

- [ ] Notification listener service declared correctly
- [ ] Permission explanation screen exists before settings redirect
- [ ] User can continue manually if permission is disabled

## Avoid

- [ ] No READ_SMS
- [ ] No RECEIVE_SMS
- [ ] No SEND_SMS
- [ ] No READ_CALL_LOG
- [ ] No contacts permission
- [ ] No location permission
- [ ] No camera permission
- [ ] No microphone permission

## 5. Data Safety Checklist

Google Play requires a clear and accurate Data Safety section. Ensure the Play Store form matches the app and privacy policy.

Official reference:
https://support.google.com/googleplay/android-developer/answer/10787469

### Answer Carefully

- [ ] Does app collect financial information?
- [ ] Is data processed locally only?
- [ ] Is any data shared with third parties?
- [ ] Is analytics used?
- [ ] Is crash reporting used?
- [ ] Can users request/delete data?
- [ ] Is data encrypted in transit? Only claim yes if data actually moves over network and is encrypted.
- [ ] Is data encrypted at rest? Only claim if implemented.
- [ ] Is data optional or required?

## 6. Privacy Policy Checklist

- [ ] Explains notification access
- [ ] Explains transaction data processing
- [ ] Says data is local in MVP
- [ ] Does not overclaim end-to-end encryption
- [ ] Explains no bank login
- [ ] Explains no ads/loans in MVP
- [ ] Explains data deletion
- [ ] Includes support email
- [ ] Matches Data Safety form

## 7. Sensitive Permission Checklist

Google Play restricts high-risk permissions such as SMS and Call Log. Kaasu MVP should avoid them.

Official reference:
https://support.google.com/googleplay/android-developer/answer/10208820

- [ ] No SMS permissions in manifest
- [ ] No Call Log permissions in manifest
- [ ] No hidden high-risk permissions from SDKs
- [ ] Verify merged manifest before release

## 8. Store Graphics Checklist

Required:

- [ ] App icon: 512 x 512
- [ ] Feature graphic: 1024 x 500
- [ ] Phone screenshots
- [ ] Tablet screenshots if tablet support claimed
- [ ] No misleading financial claims
- [ ] No loan/credit visuals
- [ ] No fake bank partnership claims
- [ ] No screenshots showing real personal financial data

## 9. Screenshot Suggestions

### Screenshot 1

Headline:

```text
Track spending privately
```

Show: Dashboard.

### Screenshot 2

Headline:

```text
No ads. No loans. No bank login.
```

Show: Privacy screen.

### Screenshot 3

Headline:

```text
Auto-detect UPI and card spends
```

Show: Transaction list.

### Screenshot 4

Headline:

```text
Know where your money goes
```

Show: Category report.

### Screenshot 5

Headline:

```text
Your data stays on your phone
```

Show: Privacy & data controls.

## 10. Release Checklist

- [ ] Version code updated
- [ ] Version name updated
- [ ] Release build tested
- [ ] ProGuard/R8 rules checked
- [ ] No debug logs with raw notifications
- [ ] No test API keys
- [ ] No sample data
- [ ] Crash-free smoke test complete
- [ ] Install/uninstall test complete
- [ ] Data deletion test complete
- [ ] Permission disable/re-enable test complete

## 11. Post-Launch Monitoring

- [ ] Watch Play Console crashes
- [ ] Track user reviews
- [ ] Collect parser failure samples manually with user consent
- [ ] Maintain supported app list
- [ ] Improve categorization rules
- [ ] Fix OEM-specific issues
