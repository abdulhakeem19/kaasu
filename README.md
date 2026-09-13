<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/banner-dark.png">
  <img src="docs/assets/banner.png" alt="Kaasu — UPI Expense Tracker" width="420">
</picture>

### Your phone already knows what you spent. Kaasu just writes it down.

A privacy-first expense tracker for India that reads the payment notifications and SMS your phone
already receives — and keeps every rupee of it on your device.

**No account. No cloud. No bank login. No ads.**

<br>

[![License: MIT](https://img.shields.io/badge/License-MIT-2ea44f?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=flat-square&logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7f52ff?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285f4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![CI](https://img.shields.io/github/actions/workflow/status/abdulhakeem19/kaasu/android.yml?branch=main&style=flat-square&label=CI)](../../actions)

[Quick start](#-quick-start) · [Features](#-what-it-does) · [Privacy](#-privacy-concretely) · [How it works](#️-how-it-works) · [Contributing](#-contributing)

</div>

---

## 🤔 Why this exists

Every expense tracker in India asks you to do one of two things:

1. **Type every purchase in by hand** — work you will quietly stop doing by week three.
2. **Hand over your bank credentials** to an aggregator — your entire financial history, on someone
   else's server, forever.

There's a third option nobody builds. Every UPI payment, card swipe and bank debit already arrives
on your phone as a notification or an SMS. **Kaasu reads those, on the device, and builds the log
for you.**

Nothing leaves your phone — there is no server for it to leave to.

---

## 🚀 Quick start

```bash
git clone https://github.com/abdulhakeem19/kaasu.git
cd kaasu/kaasu-android
./gradlew installDebug
```

Then on the phone:

| Step | What to do |
|:---:|---|
| **1** | Open Kaasu and finish onboarding |
| **2** | Grant **notification access** when asked — this opens Android Settings, it isn't a normal popup |
| **3** | *Optional:* grant SMS access, then **Settings → Re-scan SMS inbox** to pull in past messages |
| **4** | *Optional:* enable the accessibility service to catch anything the first two missed |

> **Heads up:** notification and SMS capture can't be exercised on an emulator. Use a real device.

<details>
<summary><b>Other useful commands</b></summary>

<br>

```bash
./gradlew test                 # unit tests
./gradlew lint                 # Android lint
./gradlew assembleDebug        # build a debug APK
./gradlew connectedAndroidTest # instrumented tests (device required)
```

</details>

---

## ✨ What it does

|   | Feature | |
|:---:|---|---|
| 🔔 | **Four ways to capture** | Notification listener, direct SMS, statement import (CSV/PDF/XLSX), and an optional passive screen reader — all feeding one pipeline |
| 🏷️ | **Learns your categories** | Tag one "SWIGGY" by hand and every later one follows. Most-frequent wins, so one misfiling can't re-teach the wrong category |
| 🧾 | **Keeps your note** | The "Bike repair" you typed while paying in GPay lands on the transaction |
| 🔁 | **Kills duplicates** | The same payment arriving by notification *and* SMS is stored once |
| 🏦 | **Knows your banks** | IDFC, SBI, Union Bank and friends get a consistent colour and monogram |
| 📊 | **Budgets & insights** | Per-category budgets, subscription detection, a spending heatmap, CSV export |
| 🌙 | **Light and dark** | A proper near-black dark theme, not an inverted light one |
| 🔒 | **App lock** | Biometric or PIN, with amounts hidden on the lock screen |

---

## 📱 Screenshots

> _Not published yet._ Screenshots of a real install show real merchants, real amounts and the names
> of real people who sent money — so they can't simply be pasted in. Add sanitised captures to
> `docs/screenshots/` and link them here.

| Dashboard | Transactions | Insights |
|:---:|:---:|:---:|
| _coming soon_ | _coming soon_ | _coming soon_ |

---

## 🔐 Privacy, concretely

Most apps are vague here, so this is specific:

- 🚫 **Your data is never uploaded** — there is nowhere to upload it to.
- 🚫 **No analytics touch transaction content.** No merchants, amounts or notes are reported anywhere.
- 🚫 **Raw notification and SMS text is never logged in release builds.**
- ✅ **Deletion is real deletion** — a hard delete, not a hidden flag. Wipe everything from Settings.
- ✅ **Money is stored as integer paise**, never floating point, so totals don't drift.

### The permissions, and why

Kaasu asks for more than most trackers. You should know exactly why before granting anything:

| Permission | Why | Required |
|---|---|:---:|
| **Notification access** | Reads payment notifications — the main capture channel | ✅ Yes |
| **`RECEIVE_SMS` / `READ_SMS`** | Catches bank SMS for payments that send no notification | Optional |
| **Accessibility service** | Passively reads GPay/PhonePe's *own* history screen to catch what nothing else saw | Optional, off by default |
| **`POST_NOTIFICATIONS`** | Budget alerts | Optional |

The accessibility channel is the most powerful permission class on Android, so it is deliberately
the most conservative code in the project: **purely passive** — it never taps, navigates or
automates anything — and scoped statically to payment-app packages and window-change events only,
so it cannot observe PIN entry or arbitrary keystrokes.
Full reasoning in [`PERMISSION_STRATEGY.md`](docs/PERMISSION_STRATEGY.md).

> ### ℹ️ Kaasu is not on Google Play, and won't be
> Google Play restricts SMS and Accessibility permissions to apps whose *core function* requires
> them, and an expense tracker doesn't qualify. Kaasu is built to be installed on your own device
> from source. If someone offers you a prebuilt Kaasu APK, don't trust it — see [SECURITY.md](SECURITY.md).

---

## 🏗️ How it works

Every capture channel converges on one pipeline, so the parser, duplicate checker and categoriser
exist in exactly one place instead of once per channel:

```
  Notification ─┐
  SMS ──────────┤
  Statement ────┼──▶  TransactionCapturePipeline
  Screen read ──┘              │
                               ├─▶ TransactionParser      amount · type · merchant · note · confidence
                               ├─▶ DuplicateChecker       one payment, two channels → stored once
                               ├─▶ CategoryRuleEngine     explicit rules, then learned from history
                               └─▶ TransactionRepository  ──▶ Room (kaasu.db)
                                              │
                                              └──▶ UseCase ──▶ ViewModel ──▶ Compose UI
```

**Built with** Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Hilt · Coroutines & Flow · WorkManager

<details>
<summary><b>Where the details live</b></summary>

<br>

| Document | What's in it |
|---|---|
| [`TECHNICAL_ARCHITECTURE.md`](docs/TECHNICAL_ARCHITECTURE.md) | Layer design and the parser pipeline |
| [`DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) | Room entities, field by field |
| [`PERMISSION_STRATEGY.md`](docs/PERMISSION_STRATEGY.md) | Why each permission exists |
| [`ROADMAP.md`](docs/ROADMAP.md) | Where this is going |
| [`CHANGELOG.md`](CHANGELOG.md) | Every meaningful change since the first commit |

</details>

---

## 🤝 Contributing

Contributions are welcome — **parser coverage for banks Kaasu doesn't handle yet** is the single
highest-value thing you can add. Every bank words its messages differently, and Kaasu can only
categorise what it can read.

> ### ⚠️ Never commit a real bank message
> A real SMS carries your account tail, a reference number, and often **another person's name**.
> Change those before adding a test — keep only the bank's sentence structure, which is the only
> part the parser reads.

Start with [CONTRIBUTING.md](CONTRIBUTING.md) — it walks through adding a new bank step by step.

Found a security or privacy issue? Please **don't** open a public issue — see [SECURITY.md](SECURITY.md).

---

## 📄 License

[MIT](LICENSE) © 2026 Abdul Hakeem

<div align="center">
<br>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/logo-dark.png">
  <img src="docs/assets/logo.png" alt="" width="54">
</picture>

<sub>Kaasu is not affiliated with, endorsed by, or connected to any bank or payment provider.<br>
Bank names and colours are used only to identify your own accounts inside the app.</sub>

</div>
