<div align="center">

# Kaasu

**A privacy-first expense tracker for India that never asks for your bank login.**

Kaasu reads the payment notifications and SMS your phone already receives, turns them into a
spending log, and keeps every rupee of it on your device. No account. No cloud. No ads.

[![License: MIT](https://img.shields.io/badge/License-MIT-2ea44f.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3ddc84.svg)](#requirements)
[![Language](https://img.shields.io/badge/kotlin-100%25-7f52ff.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg)](https://developer.android.com/jetpack/compose)

</div>

---

## Why this exists

Every expense tracker in India asks you to do one of two things: type each purchase in by hand, or
hand over your bank credentials to an aggregator. The first is work you will stop doing by week
three. The second means your entire financial history lives on somebody else's server.

Your phone already knows. Every UPI payment, card swipe and bank debit arrives as a notification or
an SMS. Kaasu reads those, on the device, and builds the log for you.

**Nothing leaves your phone.** There is no server to leave it to.

## What it does

| | |
|---|---|
| 🔔 **Four capture channels** | Notification listener, direct SMS, bank statement import (CSV/PDF/XLSX), and an optional passive accessibility reader — all feeding one pipeline |
| 🏷️ **Learns your categories** | Categorise one "SWIGGY" by hand and every later one follows. Most-frequent wins, so a single misfiling can't re-teach the wrong category |
| 🧾 **Reads the note you typed** | The "Bike repair" you typed while paying in GPay lands in the transaction |
| 🔁 **Catches duplicates** | The same payment arriving by notification *and* SMS is stored once |
| 🏦 **Knows your banks** | IDFC, SBI, Union Bank and friends get their own colour and monogram, consistently |
| 📊 **Budgets and insights** | Monthly budgets per category, subscription detection, spending heatmap, CSV export |
| 🔒 **App lock** | Biometric or PIN, with amounts hidden on the lock screen |

## Privacy, concretely

This is the part most apps are vague about, so here it is in specifics:

- **No network permission is used for your data.** Transactions are never uploaded, because there is
  nowhere to upload them to.
- **No analytics touch transaction content.** No spending data, merchant names or amounts are
  reported anywhere.
- **Raw notification and SMS text is never logged in release builds.**
- **Deletion is real deletion** — a hard delete, not a flag, and you can wipe everything from
  Settings.
- **Money is stored as integer paise**, never floating point, so your totals don't drift.

### About the permissions

Kaasu asks for more than most trackers, and you should know exactly why before you grant anything:

| Permission | What it's for | Required? |
|---|---|---|
| Notification access | Reads payment notifications — the primary capture channel | Yes |
| `RECEIVE_SMS` / `READ_SMS` | Catches bank SMS for payments that send no notification | Optional |
| Accessibility service | Passively reads GPay/PhonePe's *own* transaction-history screen to catch payments nothing else recorded | Optional, off by default |
| `POST_NOTIFICATIONS` | Budget alerts | Optional |

The accessibility channel is the most powerful permission class on Android, so it is deliberately
the most conservative thing in the codebase: **purely passive** — it never taps, navigates or
automates anything — scoped statically to payment-app packages and to window-change events only, so
it cannot observe PIN entry or arbitrary keystrokes. See
[`PERMISSION_STRATEGY.md`](docs/PERMISSION_STRATEGY.md) for the full reasoning.

> **Note:** Kaasu is not distributed on Google Play. Google Play restricts SMS and Accessibility
> permissions to apps whose *core function* requires them, and an expense tracker does not qualify.
> Kaasu is built to be installed on your own device from source.

## Screenshots

> _Add screenshots to `docs/screenshots/` and link them here._

| Dashboard | Transactions | Insights |
|---|---|---|
| _coming soon_ | _coming soon_ | _coming soon_ |

## Getting started

### Requirements

- Android Studio (Ladybug or newer)
- JDK 17
- An Android device running **8.0 (API 26)** or later — a physical device is strongly recommended,
  since notification and SMS capture cannot be exercised on an emulator

### Build and install

```bash
git clone https://github.com/abdulhakeem19/kaasu.git
cd kaasu/kaasu-android
./gradlew installDebug
```

Then, on the device:

1. Open Kaasu and complete onboarding
2. Grant **notification access** when prompted (this opens Android Settings — it is not a normal
   runtime dialog)
3. Optionally grant SMS access, and run **Settings → Re-scan SMS inbox** to import past messages
4. Optionally enable the accessibility service

### Common commands

```bash
./gradlew test              # unit tests
./gradlew lint              # Android lint
./gradlew assembleDebug     # build a debug APK
./gradlew connectedAndroidTest   # instrumented tests (device required)
```

## How it works

Every capture channel converges on one pipeline, so the parser, duplicate checker and categoriser
exist in exactly one place:

```
Notification ─┐
SMS ──────────┤
Statement ────┼─→ TransactionCapturePipeline
Screen read ──┘         │
                        ├─ TransactionParser      amount · type · merchant · note · confidence
                        ├─ DuplicateChecker       same payment from two channels → stored once
                        ├─ CategoryRuleEngine     explicit rules, then learned from your history
                        └─ TransactionRepository  → Room (kaasu.db)
                                  │
                                  └─→ UseCase → ViewModel → Compose UI
```

**Stack:** Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Hilt · Coroutines/Flow ·
WorkManager

Deeper detail lives in [`docs/`](docs/):

- [`TECHNICAL_ARCHITECTURE.md`](docs/TECHNICAL_ARCHITECTURE.md) — layer design and the parser pipeline
- [`DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) — Room entities, field by field
- [`PERMISSION_STRATEGY.md`](docs/PERMISSION_STRATEGY.md) — why each permission exists
- [`ROADMAP.md`](docs/ROADMAP.md) — where this is going
- [`CHANGELOG.md`](CHANGELOG.md) — every meaningful change since the first commit

## Contributing

Contributions are welcome — especially **parser coverage for banks Kaasu doesn't handle yet**, which
is the single highest-value thing you can add.

⚠️ **Never commit a real bank message.** Change the names, account tails and reference numbers
before adding a test. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute a parser and what
else is expected.

## Security

Found a security or privacy issue? Please **don't** open a public issue — see
[SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE) © 2026 Abdul Hakeem

Kaasu is not affiliated with, endorsed by, or connected to any bank or payment provider. Bank names
and colours are used only to identify your own accounts inside the app.
