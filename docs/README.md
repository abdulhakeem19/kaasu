# Kaasu

> **Note (2026-08-30):** Kaasu has pivoted to a personal-use-only build for the repo owner — it is not being published to the Play Store. This document still describes the original public-MVP scope; see `docs/MVP_SCOPE.md` and the root `README.md` for the current, accurate picture (SMS capture and statement import are now implemented; Accessibility Service capture is in progress).

**Kaasu: Private UPI Expense Tracker**

Kaasu is a privacy-first Android expense tracker for Indian users. It reads UPI, bank, wallet, and card transaction notifications locally on the user’s phone, organizes spending automatically, and shows clear monthly money insights without ads, loans, signup, bank login, or forced cloud sync.

## Core Promise

> Your money. Your phone. No cloud.

## Product Positioning

Kaasu is not a loan app, credit score app, banking app, investment app, or payment app. It is a local-first spending clarity tool that helps users understand where their money goes every month.

## MVP Goal

The MVP should answer one question clearly:

> Where did my money go this month?

## MVP Features

- Privacy-first onboarding
- Notification access education and permission flow
- UPI/bank/card/wallet transaction notification capture
- Local transaction parsing
- Rule-based categorization
- Dashboard with monthly spend, today spend, budget remaining, and category breakdown
- Transaction list with search and filters
- Manual expense entry
- Transaction edit and correction flow
- Custom categories
- User-defined rules
- Monthly reports
- CSV export
- Local data deletion

## Not Included in MVP

- Bank login
- Account Aggregator integration
- Cloud sync
- AI chatbot
- Loans
- Ads
- Credit score
- Investment advice
- Family sharing
- iOS app
- Web dashboard

## Recommended Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Coroutines + Flow
- Hilt
- WorkManager
- NotificationListenerService
- Local CSV export

## Suggested Project Structure

```text
kaasu-android/
  README.md
  docs/
    PRODUCT_BLUEPRINT.md
    MVP_SCOPE.md
    ROADMAP.md
    TECHNICAL_ARCHITECTURE.md
    DATABASE_SCHEMA.md
    PERMISSION_STRATEGY.md
    PRIVACY_POLICY_DRAFT.md
    PLAY_STORE_CHECKLIST.md
    TESTING_CHECKLIST.md
  app/
    src/
      main/
        java/com/kaasu/app/
          core/
          data/
          domain/
          feature/
          notification/
          ui/
```

## Development Order

1. Create Kotlin Compose project
2. Build static UI screens
3. Add Room database
4. Add NotificationListenerService prototype
5. Build parser engine
6. Build categorization rules
7. Build transaction list and detail screens
8. Build dashboard
9. Build reports and CSV export
10. Add privacy and data deletion flow
11. Run private beta
12. Prepare Play Store launch

## References

- Android NotificationListenerService: https://developer.android.com/reference/kotlin/android/service/notification/NotificationListenerService
- Android Room: https://developer.android.com/jetpack/androidx/releases/room
- Android DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Google Play SMS/Call Log permissions: https://support.google.com/googleplay/android-developer/answer/10208820
- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
