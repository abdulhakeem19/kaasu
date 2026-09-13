# Kaasu — Development Changelog

This file tracks every meaningful change to the Kaasu app from the first commit to launch and beyond.
Updated with each push-worthy commit. The goal is to always know the path we came from.

---

## Format

```
## [Phase / Version] — YYYY-MM-DD
### Added / Fixed / Changed / Removed
- Short description of what changed and why
```

---

## [Phase 10: Accessibility channel documented as non-functional] — 2026-09-13
### Changed
- **The accessibility capture channel does not work, and the app now says so.** Measured against
  current Google Pay on a real device, it captures nothing for two independent reasons:
  - `isTransactionScreen()` matches `viewIdResourceName` substrings. A dump of GPay's accessibility
    tree returns exactly one resource-id, `android:id/content` — the check can never pass.
  - `collectLeafTexts()` reads `node.text`. GPay exposes zero text nodes; its content is in
    `contentDescription`, which the scraper never reads. Even with (1) fixed it would find nothing.
  GPay is also behind a biometric lock a passive service cannot pass. Corroborated by the database:
  across 1,043 captured transactions, `app_sources` records **zero** scrape attempts ever.
- The onboarding page previously headed "Catch what notifications miss" with a button reading
  "Allow screen reading". Soliciting Android's most powerful permission class for a feature that
  provably does nothing is a worse failure than the README overstating it, so the page now leads
  with "Screen reading (not working yet)", explains why, and recommends skipping.
- README and `PERMISSION_STRATEGY.md` updated to match. The feature count in the README drops from
  four capture channels to three, because three is the number that work.
- The channel is documented rather than deleted: the two-layer scope filter and the parser reuse are
  the expensive parts and remain correct, so a working scraper would be a contained change.

---

## [v1.0.1] — 2026-09-13
### Fixed
- Onboarding was unreadable in dark mode — the first screen a new user saw, and v1.0.0 shipped
  with it. Worth a release on its own.

### Added
- "காசு · Tamil for money" on both About surfaces.

---

## [Phase 10: Onboarding contrast fix + showcase screenshots] — 2026-09-13
### Fixed
- **Onboarding was unreadable in dark mode.** Its root used `KaasuColors.onForest` as the page
  background — that is the cream which sits *on* the brand green, not a background role. In light
  mode it passed for one; in dark mode it resolved near-white while the headings, which use `ink`
  (also near-white in dark), disappeared into it completely. Now uses
  `MaterialTheme.colorScheme.background`. This was the first screen a new user saw, and the
  released v1.0.0 shipped with it.

### Added
- Screenshots in `docs/screenshots/`, shown in the README. Captured from a throwaway `.demo` build
  seeded with invented merchants and amounts — never from a real install, whose every screen shows
  real merchants, real balances and the names of real people who sent money.

---

## [Phase 10: Name and origin] — 2026-09-13
### Added
- The README now says what the name means — *kaasu* · **காசு** · Tamil for money — directly under
  the pitch, since it is the first thing a stranger wonders, and "Built in Chennai" in the footer.
  The app already carried "Made in Chennai" in three places; the README carried neither.
- Both About surfaces (Settings → About Kaasu, and the About dialog) now show "காசு · Tamil for
  money" above the version.

---

## [Phase 10: Releases — downloadable signed APK] — 2026-09-13
### Added
- `.github/workflows/release.yml` — tagging `v*` builds, tests, signs and publishes a release APK
  with its SHA-256 checksum attached. Signing material comes from repository secrets, is written to
  disk only for the build, and is deleted by a step that runs even when the build fails.
- The workflow fails loudly when `KEYSTORE_BASE64` is absent rather than quietly publishing an
  unsigned APK, which no device can install.
- `docs/RELEASING.md` — how to create the keystore, which four secrets to add, and how to cut a
  release. Spells out that the signing key must be kept forever: Android identifies an app by its
  key, so losing it means no existing install can ever be updated.
- README gained an **Install** section leading with the APK download, plus a checksum-verification
  step — worth the extra line for an app that reads bank messages.

### Changed
- README and `SECURITY.md` previously said Kaasu was source-only. Now that a binary exists, both say
  to install *only* from this repository's releases or from source, and how to verify the download.

---

## [Phase 10: Toolchain upgrade — AGP 9, Kotlin 2.4, compileSdk 37] — 2026-09-13
### Changed
- Upgraded the whole toolchain in one commit rather than piecemeal. Dependabot had opened five
  separate PRs that each failed on their own, because the upgrades are interdependent: the new
  androidx libraries need AGP 9.1+, AGP 9.4 needs Gradle 9.6+, and the androidx artifacts need
  `compileSdk` 37. Nothing moves until all of it moves.
  - Gradle 8.10.2 → 9.7.1, AGP 8.7.2 → 9.4.0, Kotlin 2.1.0 → 2.4.20
  - KSP 2.1.0-1.0.29 → 2.3.12 (KSP has moved to standalone versioning, no longer Kotlin-coupled)
  - Hilt 2.56.2 → 2.60.1, Compose BOM 2025.01 → 2026.09, coroutines 1.9.0 → 1.11.0
  - Room 2.7.0 → 2.8.5, navigation 2.8.5 → 2.10.1, and the rest of the androidx group
- **AGP 9 provides Kotlin support itself**, so the `org.jetbrains.kotlin.android` plugin had to be
  removed from both build files, and `android.kotlinOptions` — deleted in AGP 9 — was replaced by
  `kotlin { compilerOptions { jvmTarget } }`.
- `compileSdk` 35 → 37, required by the androidx artifacts. `targetSdk` deliberately stays at 35:
  raising it opts the app into new runtime behaviour, which is a separate change that deserves
  device testing on its own rather than riding along with a dependency bump.
- `vico` is held at 2.0.1. The 3.x release is a breaking API rewrite of the charting layer, which
  is a code migration rather than a version bump and does not belong in this commit.

### Fixed
- Six `Locale.getDefault()` reads inside composables now read `LocalConfiguration.current.locales[0]`.
  The updated Compose lint flags the old form as `NonObservableLocale` — it is genuinely a bug, not
  just a style rule: a composable reading the default locale that way does not recompose when the
  device locale changes, so dates and month names would keep rendering in the previous language.

---

## [Phase 10: Open-source readiness] — 2026-09-13
### Removed
- **Real personal data scrubbed from the repository.** Parser tests and changelog entries carried
  real captured messages: other people's names (the senders of payments), account tails, bank RRN
  and reference numbers, and typed payment notes. Those people never agreed to appear in a public
  repository. 74 occurrences across 14 files replaced with invented equivalents of the same shape —
  the parsers only care about sentence structure, which is preserved exactly.

### Fixed
- **`gradle-wrapper.jar` was gitignored and untracked**, so a fresh clone could not run `./gradlew`
  at all. That breaks CI and blocks every new contributor on their first command. Now committed,
  which is Gradle's own guidance.

### Added
- `LICENSE` — MIT.
- `README.md` — what the app is, why it exists, the privacy position stated concretely, a permission
  table with the reasoning, build instructions, and the capture-pipeline diagram.
- `CONTRIBUTING.md` — leads with the rule that matters most here: never commit a real bank message.
  Parser coverage is called out as the highest-value contribution, with the steps to add a bank.
- `SECURITY.md` — private reporting, and an explicit list of what counts as a security issue for an
  app that holds a complete spending history and is expected never to transmit it.
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1, extended to name financial data as private
  information.
- `.github/workflows/android.yml` — CI running tests, lint and a debug assemble. Debug rather than
  release because signing is local-only; the signing config already tolerates a missing
  `keystore.properties`.
- `.github/ISSUE_TEMPLATE/` — bug, feature, and a dedicated "bank not supported" form. Each one
  requires an explicit confirmation that personal details were removed.
- `.github/PULL_REQUEST_TEMPLATE.md`, `.github/dependabot.yml`, `.gitattributes`.

### Changed
- Repository restructured for a public audience: `kaasu_project_docs/` → `docs/`, and the
  Play-Store-era material to `docs/archive/play-store/` since it describes a submission that will
  never happen. All 20 cross-references updated; no broken links remain.

---

## [Phase 9: Bank identity — brand colours and monograms] — 2026-09-13
### Added
- `BankRegistry` resolves a bank from any identifier a transaction happens to carry: a DLT SMS
  sender header (`sms:JM-IDFCFB-S`), an app package (`com.phonepe.app`) or an account's display
  name ("IDFC FIRST Card"). One fragment table serves all three, matched longest-first.
- Account chips now show the bank's own colour and monogram. Previously the colour came from
  hashing the account's last four digits, so the same bank appeared in a different colour on every
  card — three IDFC cards were green, amber and blue — and unrelated banks could collide on one.
- **No third-party logo images are bundled.** The colours are hand-matched approximations, and a
  monogram on the brand colour gives the same recognition without shipping trademarked artwork. If
  real logos are wanted, only `BankIdentity` gains a field; no call site changes.
- The stored `colorArgb` is now the *fallback*, not the winner. It reads like a user choice but is
  auto-assigned at account creation, and letting it win is what produced the inconsistency.
  Accounts matching no known bank still use it, then fall back to their own initials.

### Fixed
- **The accounts screen was unreachable.** `AccountsScreen`, its view model and its nav route all
  existed, and `SettingsScreen` even declared an `onNavigateToAccounts` parameter — but nothing in
  its body ever called it, so there was no way to open the screen. Added a "Accounts & cards" row
  under Bank Sources.
- Auto-created accounts fall back to the bank resolved from the SMS sender header before the
  literal string "Bank". The sender names the bank even when the message body does not, which is
  how an account ended up called "Bank" on the test device.

---

## [Phase 9: Dark theme — near-black surfaces] — 2026-09-13
### Added
- `background` and `surface` moved into `KaasuPalette`, and both Material color schemes are now
  *derived* from the palette via `schemeFrom()` rather than maintained alongside it. Previously a
  palette change moved half the app and the hand-written `darkColorScheme` moved the other half.
- Two dark palettes, compared on device: `NearBlackKaasuPalette` (#0B0F0D ground, #151A17 cards,
  near-white text) and `CharcoalKaasuPalette` (the warmer grey-green Kaasu already shipped).
  `DarkKaasuPalette` aliases the near-black one — swapping that single line changes every dark
  surface in the app, which is the payoff of routing all color through `KaasuColors`.
- Near-black was chosen for amount legibility: expense red and income green need a deeper ground to
  read as accents, and the branded green card separates from the background instead of merging into
  it. Charcoal stays in the file as a one-line alternative rather than being deleted.

---

## [Phase 9: Refactor — theme-aware brand palette] — 2026-09-13
### Changed
- **Dark mode was defined but unreachable.** A `DarkColorScheme` existed, and Settings → Appearance
  already offered System/Light/Dark, but ~300 call sites across 24 files referenced the raw
  light-mode constants directly (`KaasuInk`, `KaasuBorder`, `KaasuForest`, …). Near-black ink and
  warm cream borders were painted on a dark background regardless of theme, so choosing Dark
  produced an unreadable screen rather than a dark one.
- Colors now resolve through `KaasuColors`, backed by a `KaasuPalette` provided by `KaasuTheme` via
  `LocalKaasuPalette`. `LightKaasuPalette` reproduces the original values exactly — light mode is
  pixel-identical — and `DarkKaasuPalette` supplies the dark counterparts.
- The brand palette deliberately stays *outside* `MaterialTheme.colorScheme` rather than being
  mapped onto it. Folding "forest" into `primary` and "expense" into `error` would erase the
  difference between a branded surface and an error state, and the income/expense pair carries
  meaning no Material role expresses.
- Every accessor is `@Composable`, which is what makes a value follow the theme. Three call sites
  could not compile under that rule and each was a genuine bug: `TransactionCard.resolveColors` was
  a plain helper (now `@Composable`), and the dashboard sparkline and reports donut read colors
  inside a `DrawScope`, which is not composition (now hoisted before the `Canvas`). `ReportsScreen`
  also held top-level `val` colors, initialised once outside composition — now composable getters.
- Two hardcoded chip backgrounds in `TransactionCard` (`0xFFD8E9DF`, `0xFFD3E2ED`) became
  alpha-blended `income`/`transfer`, so they follow the theme instead of staying pale on dark.

---

## [Phase 9: Search and filter — wiring up the dead controls] — 2026-09-13
### Fixed
- **Transaction search never worked.** `TransactionsViewModel.onSearchQueryChange` and the search
  flow inside the `combine` were fully implemented, but the "search bar" was a plain `Box` drawing
  `state.searchQuery` as static text — no `TextField`, no `clickable` — so nothing in the app could
  ever set a query. Replaced with a real `BasicTextField` plus a clear button. Searches merchant,
  note and category name, which the view model already supported.
- **Dashboard search button was decorative.** A `Box` with a `Search` icon and no click modifier at
  all. Now opens the transactions list, which is where search lives.
- **Transactions back arrow was decorative.** Same problem. This screen is reachable both as a
  bottom-nav tab and from the dashboard's "View all", so it now takes a nullable `onBack` and the
  arrow is drawn only when `previousBackStackEntry` exists — no dead control on the tab root.
- **Settings search button was decorative.** Removed rather than wired: searching settings is a
  feature in its own right, and shipping a fake button is worse than shipping no button.

### Added
- **Filter sheet**, opened by the transactions filter icon (which was also a dead control). Filters
  by transaction type and by "needs a category", with a clear-all. The icon's border turns green
  while a filter is active, so a filtered list can never silently look like a complete one. The
  category chips were already functional and stay on screen, so they are deliberately excluded from
  that indicator.

---

## [Phase 8: Notes, learned categories, pending-payment guard, re-parse backfill] — 2026-09-08
### Added
- **Payment notes.** `NoteParser` pulls the free text the payer typed in GPay/PhonePe ("Bike
  repair", "Groceries") out of the notification and stores it in the existing `note` column, which
  the capture pipeline had been hardcoding to null since it was written. These apps append the note
  straight after the amount with no label, so the note is whatever trails the amount — which is
  also where the app's own boilerplate sits, hence the `BOILERPLATE`/`NON_NOTES` filtering that
  keeps "Tap to view." and "Sent using Paytm UPI" from being stored as if the owner had typed them.
- **Learned categories.** When no explicit rule matches, the pipeline now reuses the category this
  merchant has most often been filed under (`TransactionDao.getLearnedCategoryIdByMerchant`), so
  categorising one "SWIGGY" transaction by hand teaches every later one. Most-frequent rather than
  most-recent, so a single misfiling cannot re-teach the wrong category; recency only breaks ties.
  Matching is NOCASE because captured casing differs per channel ("SWIGGY" via SMS, "Swiggy" via
  the notification listener).
- **Re-parse backfill.** `TransactionBackfillManager`, wired to Settings → "Re-scan saved
  transactions", re-runs the current parser over stored transactions using the `rawText` each one
  retained. Parser improvements otherwise only ever help future captures, leaving existing history
  permanently stuck on "Unknown". Strictly enrich-only: a field is written only where it is
  currently empty, and manual entries are excluded entirely, so nothing the owner set by hand is
  ever overwritten.
- Settings → "Exclude non-transactions" marks rows the current parser now rejects as ignored so
  they stop counting toward spend totals. Ignored, never deleted — the row and its raw text
  survive and the owner can reverse it.

### Fixed
- **Double-counted mandate payments.** `PendingPaymentDetector` drops notifications that announce a
  payment instead of confirming one — UPI mandate pre-debit notices ("Your account will be debited
  with Rs 300.00 towards X"), scheduled autopay, and bill reminders. The real confirmation for the
  same mandate arrives days later, far outside `DuplicateChecker`'s window, so it can never pair
  them and both were being stored. On the device sample this was built from, one ₹300 mandate had
  20 pre-debit notices stored against 3 actual debits — about ₹20,000 of phantom spend overall.
  The rule is tense-based, not keyword-based: verified against 959 real captured rows, it matches
  29 of them and none that contain completed-payment wording.

---

## [Phase 7: Fix — merchant extraction for real bank/UPI formats] — 2026-09-08
### Fixed
- Transactions were showing as "Unknown" in the UI because `MerchantParser` returned null: on a
  972-transaction device sample, 430 genuine captures had no merchant name. The parser only
  understood "paid to X" / "at X" / "from X" / a VPA handle, and four very common real formats
  matched none of them. Added, measured against that sample to recover 73% of them:
  - `BENEFICIARY_CREDITED_PATTERN` — IDFC's "A/c XX1234 debited by Rs. 5.00 on 28/11/25; EXAMPLE SECURITIES
    PRIVATE LI credited." puts the beneficiary *before* the verb, so no "to X" pattern could ever
    see it. By far the biggest single gap (249 rows).
  - `FVG_PATTERN` — Union Bank's "Fvg: ARUN TRAD Avl Bal Rs:1391.54" names the beneficiary in a
    "favouring" field terminated by the running balance (32 rows).
  - `TOWARDS_PATTERN` — IDFC UPI mandates: "debited with Rs 300.00 towards EXAMPLE SECURITIES PRIVATE
    LIMITED SI for the UPI Mandate" (28 rows).
  - `TO_OUTCOME_PATTERN` — GPay autopay: "Payment for Autopay of Rs.835.44 to Hostinger was
    successful" has a bare "to X" with no paid/sent verb in front of it (6 rows).
  Each capture is anchored to start at a letter, so a comma inside a thousands-separated amount
  ("Rs. 1,234.00") cannot be mistaken for the clause delimiter and swallow the amount as a name.
- `REFUND`/`CASHBACK` now try `PAID_YOU_PATTERN` first, like `INCOME` already did. A refund is money
  arriving and is named the same way, so GPay's "NAME paid you Rs.300.00 Bike repair Refund"
  previously resolved to no merchant at all.
- Pattern dispatch moved to an ordered `firstMatch(vararg)` helper. Ordering is the precedence rule —
  narrow format-specific patterns must run before `VPA_PATTERN`, which matches a handle anywhere.

### Note
This fixes capture going forward. The 430 existing rows keep their stored "Unknown" until a
re-parse backfill is run over their retained `rawText`.

---

## [Phase 7: Chore — archive Play Store docs, ignore design bundle] — 2026-09-08
### Changed
- `TERMS_OF_SERVICE.md`, `DATA_DELETION.md` and `docs/archive/play-store/RELEASE_NOTES.md` were written for the
  Play Store submission that will never happen. Committed with the same top-of-file historical
  note the personal-use pivot applied to `PRIVACY_POLICY.md` and `DATA_SAFETY.md`, so they stop
  making binding-sounding public promises the current build no longer keeps, without losing the record.
- `.gitignore` now ignores `/kaasu/` and `/Kaasu-handoff.zip` — an unpacked Claude Design handoff
  bundle and its archive, external design-tool scratch rather than repo content. Three of that
  folder's subdirectories were already ignored individually; this replaces them with the whole folder.

---

## [Phase 7: Fix — lint clean after SMS capture] — 2026-09-08
### Fixed
- `AndroidManifest.xml` now declares `<uses-feature android:name="android.hardware.telephony"
  android:required="false" />`. The `RECEIVE_SMS`/`READ_SMS` permissions added for the SMS capture
  channel implicitly require telephony hardware, which failed lint with two
  `PermissionImpliesUnsupportedChromeOsHardware` errors and would have restricted install to
  telephony devices. Marking it not-required keeps the app installable on tablets/ChromeOS, where
  the other three capture channels still work.
- Switched the two `Icons.Default.Message` usages (onboarding SMS step, Settings SMS row) to
  `Icons.AutoMirrored.Filled.Message`, clearing the deprecation warnings introduced with the SMS
  capture UI. AutoMirrored is also the correct choice for a directional icon under RTL.

---

## [Docs: personal-use pivot] — 2026-08-30
### Changed
- **Kaasu is no longer headed for the Play Store — docs updated to reflect a personal-use-only build.** The owner has decided Kaasu will never be published; it's a single-user, full-consent build for the owner's own device. Updated `CLAUDE.md`, `docs/PERMISSION_STRATEGY.md`, `docs/MVP_SCOPE.md`, `docs/PRODUCT_BLUEPRINT.md`, `docs/ROADMAP.md`, and `docs/README.md` to drop the "avoid SMS / avoid Accessibility / Play policy risk" framing that no longer applies, while keeping the underlying privacy principles (local-only, no cloud upload, no analytics on transaction content, no raw text logged in release) explicitly intact and unchanged.
- **Cross-referenced already-implemented work**: direct SMS capture (`feature/sms-capture`) and bank/UPI statement import (`feature/statement-import`, `feature/statement-import-real-formats`) are now documented as implemented capture channels; Accessibility Service capture (reading UPI apps' own on-screen transaction history) is noted as in progress on a separate branch, not yet complete or merged.
- **Marked Play Store submission artifacts as historical**: added prominent top-of-file notes to `docs/PRIVACY_POLICY.md` and `docs/archive/play-store/DATA_SAFETY.md` (whose "collects nothing" / "no SMS-reading permissions" claims no longer hold) and lighter notes to `docs/archive/play-store/STORE_LISTING.md` and `docs/archive/play-store/RELEASE_CHECKLIST.md`, without rewriting their content — they're kept for historical reference, not repurposed as an accurate current description.
- Docs-only change; no app code touched.

---

## [Phase 7: Accessibility screen-read capture (fourth, passive-only channel)] — 2026-08-30

Fourth transaction-capture channel, alongside the notification listener, direct SMS capture, and
statement import — all four still land in the same `TransactionEntity` table via the shared
`TransactionCapturePipeline`. This one is qualitatively different from the other three: it's an
`AccessibilityService` that opportunistically reads GPay/PhonePe's own on-screen
transaction-history text to catch payments none of the other channels ever recorded (a dismissed
notification, a transaction outside the SMS/statement window, an in-app-only balance change).
AccessibilityService is the single most powerful permission class on Android, so this is
deliberately the most conservative implementation possible, and the most optional/lowest-priority
of the four channels in onboarding:

- **Purely passive, never active automation.** The service does nothing unless the owner already
  has GPay or PhonePe open and something changes on screen during their own normal use (scrolling
  their own history). It never calls `performGlobalAction`/`performAction` to open, navigate, or
  tap anything — that's a categorically riskier capability (interrupts the owner's phone use,
  can't pass biometric/PIN prompts, far higher fragility) explicitly left out of this pass.
- **Two-layer scope filter**, the entire safety model for this channel: (a) static —
  `accessibility_service_config.xml` narrows `android:packageNames` to the same
  `SourceApps.PAYMENT_APP_PACKAGES` superset used for notification capture, and
  `android:accessibilityEventTypes` to ONLY `typeWindowStateChanged|typeWindowContentChanged` —
  never a click/text-changed/all-mask type, so this app can never observe PIN-entry keystrokes or
  arbitrary taps; (b) dynamic — `ScraperRegistry` narrows further to only packages that actually
  have a registered `ScreenScraper` (today: GPay, PhonePe — being allow-listed for notifications
  doesn't mean a scraper exists). `KaasuAccessibilityService.onAccessibilityEvent` also redundantly
  guards against its own package, and wraps the entire scrape in `runCatching` — a crash here can
  disable the *entire* system accessibility service until the user manually re-enables it, so a bug
  in one scraper must never be allowed to propagate out of that callback.
- **Reuses the existing parser, not a fork of it.** Each `ScreenScraper` (`GPayScreenScraper`,
  `PhonePeScreenScraper`, sharing a common bounded-depth tree-walk in
  `BaseTransactionScreenScraper`) reconstructs a row's scattered on-screen text fragments into one
  sentence (e.g. "Paid to Swiggy ₹245 28 Aug") via the pure, unit-tested
  `TransactionRowTextBuilder`, then `ScrapedTransactionCandidate.toRawNotification()` wraps that
  into the exact same `RawNotification` model notification/SMS capture already use — so
  `TransactionParser`'s existing amount/merchant/confidence logic runs completely unmodified for
  this fourth channel too. `isTransactionScreen()` matches defensively on resource-id SUBSTRINGS
  ("transaction"/"txn"/"history"/"amount") rather than exact ids, since exact ids churn across app
  version bumps.
- **Coarse-precision dedup.** A history row's date is bare ("28 Aug", "Yesterday") with no
  time-of-day — the new `DateTextResolver` resolves it to a `LocalDate` and anchors `postedAt` at
  **noon** (not midnight — a deliberate mid-day anchor for coarse-dedup comparisons) when no time
  fragment is present. Because that timestamp is coarse, this channel cannot use
  `DuplicateChecker.isDuplicate`'s tight 5-minute window — it would almost never line up with the
  real notification/SMS-captured timestamp for the same payment. Added
  `DuplicateChecker.isDuplicateCoarse`, widening the match to a full calendar day, reusing
  `TransactionRepository.getByDateRangeAmountAndType` (already existed from the statement-import
  Tier 2 dedup — no new DAO query needed) and `MerchantSimilarity` unchanged.
  `TransactionCapturePipeline.process` gained a `useCoarseDedup` parameter (default `false`, so
  every existing caller is unaffected) rather than forking a second orchestration path — only the
  accessibility channel passes `true`. This channel only ever *inserts* transactions no other
  channel caught; it never verifies, corrects, or overwrites an existing entry — a coarse
  duplicate hit means "drop it," full stop.
- **Visible health signal**, because this channel fails silently by design otherwise:
  `AppSourceEntity` gained two nullable columns, `lastAccessibilityScrapeAttemptAt` and
  `lastAccessibilityScrapeSuccessAt` (`Migration_7_8`, schema v7 → v8), updated via two new
  `AppSourceDao` methods from `KaasuAccessibilityService`. `BankSourcesScreen`'s per-source detail
  sheet now shows a "Screen reading" status line for GPay/PhonePe ("last successful read 2 days
  ago" / "attempted, nothing new found" / "not attempted yet") plus a read-only reflection of
  whether the system accessibility toggle is actually on (`AccessibilityServiceStatus`, backed by
  `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`) with a tap-through to
  `Settings.ACTION_ACCESSIBILITY_SETTINGS` — Kaasu cannot flip this toggle itself.
- **Onboarding**: new `AccessibilityPage`, modeled directly on `SmsPage` — same
  "always show a Skip action, degrade gracefully" pattern, since this channel is even more
  optional than SMS was (it only ever catches what the other three missed). Inserted after the SMS
  page; step-progress-bar segment count bumped from 5 to 6. Manifest `<service>` entry follows the
  same style as the existing notification-listener/SMS-receiver entries, with
  `BIND_ACCESSIBILITY_SERVICE` and a `meta-data` pointing at the new config XML. The system
  Settings description string (`accessibility_service_description`) is deliberately specific about
  what it reads and an explicit disclaimer of what it does NOT do (no PIN/password capture, no
  tap/keystroke injection, nothing leaves the device) — matching the onboarding pages' honest-copy
  tone.

**Fragility and maintenance-burden tradeoffs, on the record**: this is by a wide margin the most
fragile of the four capture channels. `isTransactionScreen`'s resource-id substring matching and
the row-reconstruction heuristics in `BaseTransactionScreenScraper` will silently stop finding
anything the moment GPay or PhonePe next reshuffles their transaction-history layout — with no
error, just a health-signal timestamp in Settings that quietly stops advancing. That's an accepted
tradeoff for a personal-use, no-store-review build: this channel is explicitly a supplementary
catch-net, never the primary or only capture path, and every one of its findings is filtered
through the same duplicate-suppression logic as the other three channels before ever reaching the
transaction table. `GPayScreenScraper`/`PhonePeScreenScraper`'s actual `AccessibilityNodeInfo`
tree-walking has no unit test (Robolectric isn't configured in this project, per the
statement-import session's finding, and standing up heavy mocking for a single Android framework
class would be disproportionate to this codebase's existing test density) — but the pure logic
downstream of that walk (`TransactionRowTextBuilder`, `DateTextResolver`,
`ScrapedTransactionCandidate.toRawNotification()`, `DuplicateChecker.isDuplicateCoarse`) is fully
covered.

### Added
- `accessibility/` package: `service/KaasuAccessibilityService.kt`, `scraper/ScreenScraper.kt`
  (interface), `scraper/BaseTransactionScreenScraper.kt` (shared bounded-depth tree-walk),
  `scraper/GPayScreenScraper.kt`, `scraper/PhonePeScreenScraper.kt`, `scraper/ScraperRegistry.kt`,
  `model/ScrapedTransactionCandidate.kt` (+ `toRawNotification()`), `extraction/DateTextResolver.kt`,
  `extraction/TransactionRowTextBuilder.kt`, `AccessibilityServiceStatus.kt`.
- `res/xml/accessibility_service_config.xml`, `accessibility_service_description` string.
- `Migration_7_8` (schema v7 → v8): `app_sources.lastAccessibilityScrapeAttemptAt` /
  `lastAccessibilityScrapeSuccessAt`; two new `AppSourceDao` update methods.
- `DuplicateChecker.isDuplicateCoarse`; `TransactionCapturePipeline.process(useCoarseDedup: Boolean = false)`.
- Onboarding `AccessibilityPage`; `BankSourcesScreen`/`BankSourcesViewModel` screen-reading health
  signal + read-only accessibility-toggle reflection.
- Unit tests: `DateTextResolverTest`, `TransactionRowTextBuilderTest`,
  `ScrapedTransactionCandidateTest`, `DuplicateCheckerTest` (isDuplicateCoarse cases).

---

## [Phase 6: Statement import — real-format parsers (IDFC FIRST XLSX, GPay PDF)] — 2026-08-30

Follow-up to the CSV/PDF statement-import work below. The app owner provided two real sample
statements and confirmed his actual usage — he banks directly with **IDFC FIRST Bank** (statement
export is **XLSX**, a format the prior session's importer didn't support at all) and does most
other spending through **Google Pay**, which itself settles through multiple linked
banks/cards (SBI credit card, Union Bank of India, IDFC FIRST) and exports its own PDF
transaction statement covering all of that. This session builds real, verified parsers for both,
built directly against the real files (unzipped/inspected XML for the XLSX; extracted with
PdfBox for the PDF) rather than guessed — see the "Note" on the entry below, which still applies
unchanged to the four CSV/PDF parsers from that prior session.

### Added
- **`XlsxTextExtractor`** (`statement/parser/xlsx/`): a hand-rolled `.xlsx` (OOXML spreadsheet)
  reader — `java.util.zip` + `javax.xml.parsers` DOM parsing, zero new library dependency (no
  Apache POI). Resolves the "Account Statement" sheet by **name** via `workbook.xml` →
  `workbook.xml.rels` (never a hardcoded `sheet1.xml`), reads the shared-string table (handling
  rich-text `<si><r><t>` runs), and reconstructs each row — including cells that are entirely
  absent from the XML (sparse/blank cells) — into pipe-delimited plain text, so XLSX becomes a
  third "extraction to delimited text" step alongside raw CSV bytes and PdfBox-extracted PDF
  text; the `StatementParser.parse(fullText: String)` contract itself didn't need to change. The
  pure zip/XML logic lives in an internal `XlsxParsing` object with no Android dependency
  (`XlsxTextExtractor` is a thin Context-owning wrapper around it) specifically so it's directly
  unit-testable without Robolectric.
- **`IdfcFirstSavingsXlsxParser`** (verified format — real sample): IDFC FIRST Bank savings
  statement, header `Transaction Date | Value Date | Particulars | Cheque No. | Debit | Credit |
  Balance`, dates as literal `dd-MMM-yyyy` text (not Excel date serials — confirmed directly in
  the raw XML). Footer/summary rows (`Total`, `Total number of Debits/Credits`, `End of the
  Statement`, blank rows) are skipped via date-parse failure rather than text pattern-matching.
  A small per-prefix mapper extracts both a clean description and (for UPI rows) the UPI
  transaction ID from the `Particulars` micro-format: `UPI/DR|CR/<txnId>/<name>/...` → name +
  ID, `BLKIFT/...` → "Salary", `IFT-OPT/...` → "Fund Transfer", `POS-VISA/<merchant>/...` →
  merchant, `*INTEREST*` → "Interest".
- **`GpayStatementPdfParser`** (verified format — real sample): Google Pay "Transaction
  statement" PDF, reusing the existing `PdfTextExtractor`. One repeating 6-line block per
  transaction, anchored on `UPI Transaction ID: <digits>` (present on every real block) with a
  small window-scan to either side for the date/verb/amount lines, rather than assuming fixed
  offsets — deliberately resilient to extraction-spacing variance. Real block order (verified via
  an actual PdfBox extraction of the sample file, run in a throwaway local script) is date, time,
  verb line, UPI-ID line, settlement line, amount — **not** the more "obvious" date/verb/time/
  amount/settlement order a casual read of the PDF's visual columns would suggest. The settlement
  line ("Paid by X" / "Paid to X") is deliberately never used for direction — only the verb line
  right after the date is: "Paid to `<name>`" → expense, "Received from `<name>`" → income. A
  third real verb variant, **"Self transfer to/from `<bank>`"** (the user moving money between
  their own linked accounts, e.g. IDFC FIRST ↔ Union Bank — not discovered until the real-file
  verification step, not something the brief anticipated), is recognized and explicitly skipped
  rather than mis-booked as income or spend, consistent with GPay's own statement note that these
  don't count toward the Sent/Received totals either.
- **`upiTransactionId: String?` field on `StatementLineItem`** (default `null` — every pre-existing
  CSV/PDF parser is unaffected). Populated by both new parsers.
- **Tier 1.5 dedup** in `StatementImportManager.isDuplicate`, between the existing Tier 1
  (exact-hash) and Tier 2 (same-day/amount/merchant-similarity): when an item carries a
  `upiTransactionId`, a new `TransactionDao.findByRawTextContaining` (`rawText LIKE '%'||:pattern
  ||'%'`, exposed through `TransactionRepository`) checks whether any existing transaction's
  stored `rawText` already contains that same reference number. This matters concretely here:
  the same real-world UPI payment can land in **both** the IDFC XLSX statement (its own
  reference embedded in `Particulars`) **and** the GPay PDF statement (`UPI Transaction ID: ...`)
  — and often in the original bank SMS/notification `rawText` too, since Indian bank SMS commonly
  echoes the same reference number. A substring match on that number is far more reliable than
  the fuzzy same-day/merchant-similarity fallback Tier 2 still handles for rows with no UPI ID
  (IDFC's `BLKIFT`/`IFT-OPT`/interest-credit rows).
- Both parsers registered in `StatementParserRegistry`; `.xlsx`'s MIME type
  (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) added to the Settings
  import file picker, plus a magic-byte ("PK" zip signature) sniff in
  `StatementImportManager.extractText` as a fallback for pickers that misreport an `.xlsx`'s
  MIME type as generic `application/octet-stream`.
- Tests: `XlsxTextExtractorTest` (builds one minimal synthetic in-memory `.xlsx` zip),
  `IdfcFirstSavingsXlsxParserTest`, `GpayStatementPdfParserTest` — all against fabricated
  names/amounts/IDs structured to match the verified real formats exactly, never real personal
  data. The two real sample files stay local-only, under a newly gitignored `/Statements/`.

### Note
- **`IdfcFirstSavingsXlsxParser` and `GpayStatementPdfParser` are verified against real sample
  statements** (XML unzipped/inspected directly for the XLSX; PdfBox-extracted and cross-checked
  against `pdftotext` for the PDF) — unlike the four CSV/PDF parsers from the entry directly
  below, which remain first-pass, unvalidated guesses built without a real sample and are left
  as-is pending an actual export from one of those banks.
- Used `javax.xml.parsers` (DOM) rather than `android.util.Xml` for the XLSX reader: the latter is
  stubbed ("not mocked") in this module's local JVM unit tests since Robolectric isn't set up
  here, which would have left the extractor's own test unable to exercise real parsing.
  `javax.xml.parsers` is equally built-in/zero-dependency and runs identically on-device and in a
  plain JUnit test.

---

## [Phase 6: Statement import — user-initiated CSV/PDF bank statement import] — 2026-08-30

Third and last capture channel: a Settings-triggered, one-off import of a bank/card e-statement
(CSV or PDF), for full historical accuracy from before the app was used, or for banks/cards that
don't produce a clear SMS/notification signal. Purely user-initiated — no service, no listener,
no live capture, unlike the notification and SMS channels.

### Added
- **`statement/` package** (sibling to `notification/`): `StatementLineItem`/`ImportResult`
  models, a `StatementParser` interface (per-bank hardcoded matching via `canParse(sample)` —
  mirrors `AccountNotificationParser`'s convention, not a generic column-mapping heuristic),
  `StatementParserRegistry` (ordered, first-match-wins), and `StatementImportManager`
  (`preview()`: parse + two-tier dedup check, no writes; `commit()`: inserts the reviewed "new"
  subset). Deliberately does **not** reuse `TransactionParser`/`DuplicateChecker` — those are
  tuned for narrative-sentence notification/SMS text and near-simultaneous cross-channel echoes;
  a statement line is already structured (date + amount + description) and only needs a
  same-day dedup window. Still lands in the same `transactions` table via the same
  `CategoryRuleEngine` for auto-categorization. Imported rows are tagged
  `sourceAppPackage = "statement_import"`, `sourceAppName = "<Bank> Statement"` — no schema
  migration needed (both are free nullable strings already).
- **Three CSV parsers**: `HdfcSavingsCsvParser` (`Date,Narration,Chq/Ref No.,Value Dt,
  Withdrawal Amt.,Deposit Amt.,Closing Balance`), `SbiSavingsCsvParser` (`Txn Date,Value Date,
  Description,Ref No./Cheque No.,Debit,Credit,Balance`), `GenericCreditCardCsvParser`
  (`Transaction Date,Description,Amount,Type`, representative of SBI Card/HDFC/ICICI/Axis
  credit-card exports). Chosen from the banks this codebase has confirmed real-device-testing
  support for (see `AccountNotificationParser`'s issuer list and the Phase 3 "Parser real-world
  coverage" rounds in this changelog: HDFC, SBI/SBI Card, ICICI, Axis, IDFC FIRST, Union Bank).
- **One PDF parser**: `HdfcSavingsPdfParser`, using `PdfTextExtractor` (thin wrapper around
  **PdfBox-Android**, since Android's built-in `PdfRenderer` only rasterizes — extracts no text).
  Text-based (selectable-text) statements only; scanned/image PDFs are out of scope (no OCR).
  PdfBox's extraction loses the original table's column alignment, so the PDF parser matches
  one transaction per line via regex against collapsed whitespace-separated tokens, rather than
  assuming column positions — same per-bank-hardcoded convention as the CSV parsers.
- **Import UX**: Settings → DATA → "Import bank statement" (new row next to "Export transactions
  (CSV)") → system file picker (CSV/PDF) → `ImportStatementScreen`/`ImportStatementViewModel`
  (new `Screen.ImportStatement` route, no bottom bar) → preview ("N found — M new, K already
  tracked (skipped)") with a scrollable list of the new line items → "Commit import" → success
  state with the final inserted count. An unrecognized format fails cleanly with "Couldn't
  recognize this file's format. Kaasu currently supports: [...]" — no manual column-mapping
  fallback. Runs as a plain suspend call on `viewModelScope` (same pattern as `BackupManager`'s
  bulk JSON restore) — no new `WorkManager` worker; realistic statement sizes don't need one.
- `TransactionDao.getByDateRangeAmountAndType` (+ `TransactionRepository` passthrough): same-day
  + same amount/type lookup for the import's Tier 2 dedup, analogous to the existing
  `getRecentByAmountAndType`.
- `core/util/MerchantSimilarity.kt`: the merchant-similarity substring check extracted out of
  `DuplicateChecker` (previously private/inline) so `StatementImportManager`'s Tier 2 dedup can
  reuse the exact same matching rule without forking the logic. No behavior change to
  `DuplicateChecker`.
- `com.tom-roush:pdfbox-android:2.0.27.0` added to the version catalog.

### Note
- **The three CSV parsers and the one PDF parser are a first-pass best effort built without a
  real sample bank statement from the app owner.** Every parser's assumed header/format, date
  pattern and column order is documented in a comment at the top of its file specifically so
  it's easy to spot-fix once tested against a real export. Expect at least the PDF parser's
  line-shape regex to need adjustment — it's a plausible-but-unverified guess at what PdfBox's
  extracted text looks like for a real HDFC statement.

---

## [Phase 6: SMS capture — direct read + shared capture pipeline] — 2026-08-30

Kaasu is now personal-use only (not published to any app store), which removes the earlier
constraint that kept it away from `READ_SMS`. This adds a second, fully additive capture channel
alongside the existing notification listener — nothing about notification capture changes.

### Added
- **Direct SMS capture**: `SmsReceiver` (live, via `SMS_RECEIVED`) and `SmsBackfillWorker` (one-time
  historical inbox scan, idempotent — safe to re-run via "Re-scan SMS inbox"). Both reuse the
  existing `RawNotification` model (`packageName = "sms:<sender>"`) and the same parser pipeline as
  notifications — no parallel parsing logic.
- **`SmsFilter`**: content is the mandatory gate (`FinancialTextHeuristics`, extracted from
  `NotificationFilter` with no behavior change) — Indian DLT sender-ID formats vary too much by
  telecom circle to be a reliable allowlist, so sender format is only ever a small additive
  confidence nudge in `TransactionParser`, never a hard gate.
- **`sms_senders` table** (`SmsSenderEntity`/`SmsSenderDao`), auto-populated the first time a sender
  is seen (mirrors `AppSourceDao`'s shape, keyed on sender instead of package). Manage per-sender
  on/off from the new **SMS sources** screen (Settings → SMS sources).
- **Onboarding**: new "Also read bank SMS directly" page after the notification-access page,
  requesting `RECEIVE_SMS`/`READ_SMS` — always skippable ("Skip — use notifications only"); granting
  triggers the one-time backfill.
- **Settings**: "SMS access" status row, "SMS sources" row, and "Re-scan SMS inbox" (with live
  progress from `WorkManager`) under a new SMS CAPTURE section.
- First `WorkManager` worker in this codebase: `androidx.hilt:hilt-work` wired via
  `KaasuApp: Configuration.Provider` + `HiltWorkerFactory`.
- Schema **v6 → v7**: `sms_senders` table + `MIGRATION_6_7`.

### Changed
- Extracted `TransactionCapturePipeline` (parse → merchant alias → dedup → classify → account
  resolution → insert) out of `KaasuNotificationListenerService.process()` so both capture channels
  share one orchestration path; the listener now only runs `NotificationFilter` and delegates.
  `Hashing.sha256Prefix()` moved out of the listener into `core/util/Hashing.kt` (same algorithm —
  existing `rawTextHash` values are unaffected).

### Note
- `BackupManager` was not extended to cover `sms_senders` — it doesn't back up the equivalent
  `app_sources` table either today, so there is no existing pattern to mirror; both are left for a
  follow-up backup-coverage pass rather than introduced inconsistently here.

---

## [Phase 5: Duplicate handling — stronger detection + manual mark] — 2026-06-22

### Fixed
- **Auto dedup missed some duplicates.** Widened the detection window from 2 → 5 minutes (bank-SMS echoes of a UPI payment can lag the app notification), and made the "bank echo" match **symmetric** — previously a bank SMS with no merchant only deduped against a named entry when it arrived *second*; now it dedups regardless of order.

### Added
- **Manual "Mark as duplicate"** on the transaction detail screen: hides the entry from lists and totals (new `isDuplicate` flag; also sets `isIgnored`) so a double-counted transaction stops inflating your spend.
- **Duplicates manager** (Settings → Duplicates): lists everything you've marked, with **Restore** to un-mark and count it again.
- Schema **v5 → v6**: `transactions.isDuplicate` column + `MIGRATION_5_6`; included in backup/restore.

---

## [Phase 5: Fix — detail screens reflect edits immediately] — 2026-06-22

### Fixed
- **Transaction detail showed stale data after an edit.** The detail screen loaded a one-shot snapshot in `init`, so editing a transaction (amount/category/mode) and returning showed the old value until you left and re-opened the screen. It now **observes the transaction as a live Room Flow** (`observeById`) and updates instantly. `Mark recurring` likewise relies on the live stream instead of an optimistic patch.
- **Needs-a-tag** now observes the uncategorized queue live (instead of a one-shot snapshot): tagging an item — here or anywhere — removes it immediately, and newly-captured untagged transactions appear without reopening. "Skip for now" is preserved via a local skip set.

### Added
- `TransactionDao.observeById` / `TransactionRepository.observeById` — live single-transaction stream.

---

## [Phase 5: Play Store prep] — 2026-06-22

### Added
- **Launcher icon** from the Kaasu logo: adaptive icon (white background + padded logo foreground) plus legacy PNG icons at all densities.
- **Release signing**: `signingConfigs` driven by a git-ignored `keystore.properties` (template committed); release build type wired to use it.
- **Play assets** under `docs/archive/play-store/`: `STORE_LISTING.md` (name/short/full description, category), `DATA_SAFETY.md` ("no data collected" answers + notification-listener justification), `RELEASE_CHECKLIST.md`, 512×512 store icon, and five full-res device screenshots (dashboard, transactions, reports, budgets, subscriptions).
- **Privacy policy** at `docs/PRIVACY_POLICY.md` (to be hosted; URL goes in the listing).

### Changed
- `versionName` → **1.0.0** (versionCode 1) for the public launch.
- Release build: minify/resource-shrinking **off** for v1 (avoids reflection keep-rule risk with Room/Hilt/JSON; revisit later).

---

## [Phase 4: Polish — subscription renewal day, dedup Help, version footer] — 2026-06-22

### Added
- Manual "Add subscription" now takes a **renewal day (1–28)**; the entry is dated to the most recent occurrence of that day so the detected "next due" lands on the right day next month.

### Fixed
- Removed the duplicate **Help & FAQ** entry — it was both a standalone HELP & LEGAL row and inside the Help & support sheet; now it lives only in the support sheet.
- Settings footer version now uses `BuildConfig.VERSION_NAME` (was hardcoded "v1.0.0", inconsistent with About).

---

## [Phase 4: MVP — Personalization (profile, currency, budget cycle)] — 2026-06-22

### Added
- **Profile screen** (`ProfileScreen` + `ProfileViewModel`, reached from the Settings top card): set your **name** (drives the dashboard greeting "Hi, <name>" and the avatar initial), pick a **currency symbol** (₹/$/€/£/¥/₨/৳), and choose the **budget-cycle start day** (1–28).
- **Currency symbol** is applied app-wide via `CurrencySymbol` (read by `formatRupees`), kept in sync from DataStore in `AppViewModel`.
- **Budget cycle**: `BudgetCycle` computes the current window from the start day (e.g. 15th → 14th). The Budgets screen now runs on this cycle (window, label, and on-pace projection), so budgets align to a mid-month pay cycle.
- DataStore: `displayName`, `currencySymbol`, `monthStartDay`.

### Changed
- Settings top card is now a **Profile** card (name + initial) opening the Profile screen; the dashboard greeting and avatar use the saved name.

### Note
- The dashboard "Spent · <month>" header still uses the calendar month; the cycle currently drives the Budgets screen. Aligning the dashboard window to the cycle can follow once verified on device.

---

## [Phase 4: MVP — Per-category budgets, subscriptions fixes, recurring/bill capture] — 2026-06-22

### Fixed
- **Subscriptions detection bug**: a transaction manually marked recurring (or any non-income spend) now appears in Subscriptions even when its type is `UNKNOWN` or `TRANSFER` — previously the detector only looked at `EXPENSE`, so e.g. "youtubegoogle" marked recurring never showed.

### Added
- **Per-category budget limits** on the Budgets screen: tap a budgeted category to edit/remove its limit, and an "ADD A LIMIT" section lists expense categories without one. Limits feed the existing progress bars (matches mockup #9).
- **Subscription detail screen**: per-charge amount, cadence, charges so far, total spent, first/last charged, next due, and the full charge history; reachable by tapping a subscription.
- **Manual "Add subscription"** from the Subscriptions header — stored as a recurring expense so it flows through detection and the monthly total.
- **Recurring capture** (`RecurringDetector`): autopay / e-mandate executions / standing instructions / SIP / EMI / subscription charges are auto-flagged `isRecurring` at capture, so they surface in Subscriptions automatically.
- **Credit-card bill capture**: e-mandate *setup* reminders are still ignored, but actual auto-debit/e-mandate *executions* are captured; seed rules categorize "<Bank> Credit Card" / card-bill payments to Bills.

---

## [Phase 4: Make dummy features real — Support, About, What's-new] — 2026-06-22

### Added
- **What's-new popup**: shows once per app version after onboarding (version-gated via DataStore `lastSeenVersionCode`), overlaid in `MainActivity`. Re-openable from About.
- **About dialog** (replaces the static About screen): app icon, name, real version, privacy-first tagline, "Made in Chennai", with Close + "What's new" — matching the requested layout.
- **Support sheet** (`SupportSheet`): Report a bug / Request a feature (pre-filled email with a diagnostics footer), Help & FAQ (opens the FAQ), and Copy diagnostics ID (app + device + Android version, no personal data) to clipboard. Reached from a "Help & support" row.

### Notes
- No analytics added — per the privacy-first positioning, the Privacy Policy screen states "Kaasu collects nothing — no analytics, no cloud" instead of an analytics toggle.

---

## [Phase 4: Make dummy features real — Backup / Restore / Export] — 2026-06-22

### Added
- **Full local backup & restore** (`BackupManager`): "Back up data" writes a `.json` snapshot of all transactions, categories, accounts, rules, merchant aliases and the budget setting, shared via the system sheet (save to Drive/Files). "Restore from backup" picks a `.json` via the document picker and faithfully restores it (wipes + re-inserts preserving primary keys so category/account references stay valid). Wires the previously dead DATA rows.
- **Onboarding "Restore from backup"** now works: pick a backup to restore and skip straight into the app — enabling the export → wipe app data → restore flow for testing onboarding.
- DAO support: `getAllForBackup` / `deleteAll*` / `insertAll*` across transaction/category/account/rule/alias DAOs.

### Changed
- Settings DATA section: "Back up data", "Restore from backup", "Export transactions (CSV)" (dropped the unimplemented "Backup (encrypted)" and "Import · Walnut/MM/CSV" placeholders). CSV export now reachable from Settings too.

---

## [Phase 4: Make dummy features real — App lock + Bank sources screen] — 2026-06-22

### Added
- **Real app lock** (was cosmetic): `androidx.biometric` + a 4-digit PIN. New `PinHasher` (salted SHA-256), `BiometricAuthenticator`, DataStore keys (lock enabled, biometric enabled, PIN hash/salt). `AppLockSetupScreen` to set/change PIN, toggle fingerprint, and turn lock off; `AppLockScreen` is now the real unlock gate (auto biometric → PIN fallback, styled keypad). `MainActivity` (now `FragmentActivity`) overlays the gate when locked and re-locks on `onStop`; `AppViewModel` holds lock state. Biometric is **opt-in** (PIN-only by default).
- **Dedicated Bank sources screen**: moved the long bank-sources list out of Settings into its own screen (`BankSourcesScreen` + `BankSourcesViewModel`); Settings now shows a single "Bank & UPI apps · N active" row. Keeps the per-source details sheet and "Add bank" picker.

### Changed
- Settings keypad/PIN UI enlarged and centered (circular keys); "App lock" row reflects real state (Off / PIN / PIN + Fingerprint).

---

## [Phase 4: Make dummy features real — Settings + bank sources] — 2026-06-22

### Changed
- **Bank sources**: each row now shows a real status ("Active · last alert 4d ago" / "Off") instead of the raw package name. Tapping a source opens a **details sheet** with the friendly name, package (monospace), type, status, last-alert time and a capture toggle. "+ Add bank" now opens a picker of known UPI/bank apps not yet added and inserts the chosen one.
- **Persisted toggles**: Hide-amounts-on-lock, Budget alerts, Subscription renewals, Daily nudge and Weekly summary are now backed by DataStore (were local-only/no-op); Daily nudge and Weekly summary became real toggle rows.
- **Merchant rules** screen now lists the actual active rules (merchant → category) from the DB with delete for user rules, replacing the hardcoded "47 auto-tags".
- **About** shows the real `BuildConfig.VERSION_NAME`; **Help & FAQ** rows expand to show real answers.
- **Privacy policy** and **Terms of service** open real in-app content screens (new `Legal` route); **Contact support** opens an email intent. (Backup/Restore/Export/Import rows are handled in the data-portability change.)

### Added
- `SourceApps.DISPLAY_NAMES` + `displayName()`; `MerchantRulesViewModel`; `LegalScreen`; DataStore keys + accessors for the five persisted toggles.

---

## [Phase 3: MVP Build — Merchant display-rename (remembered for future captures)] — 2026-06-22

### Added
- Rename a merchant from the edit screen: when you change the Payee on an existing transaction, a "Rename '<old>' everywhere & remember for future" toggle appears (on by default; uncheck for a one-off fix). When on, it records a merchant alias and bulk-renames existing transactions with that name.
- `merchant_aliases` table (Room schema v4 → v5, `MIGRATION_4_5`) + `MerchantAliasDao` / `MerchantAliasRepository`.
- Capture pipeline applies aliases: `KaasuNotificationListenerService` rewrites a parsed merchant to its alias before categorization, account-memory and storage — so future notifications for the renamed merchant save with the chosen name.
- `RenameMerchantUseCase` (alias + bulk rename) and `TransactionDao.renameMerchant`.

---

## [Phase 3: MVP Build — Split transaction] — 2026-06-22

### Added
- Split a transaction into multiple category slices (detail screen → Split). A bottom sheet lets you set per-slice amounts and categories, add/remove slices, with a live balance indicator; the Split button enables only when slices are balanced to the original and there are ≥2.
- `transactions.parentId` (Room schema v3 → v4, `MIGRATION_3_4`) linking each slice to its parent.
- `TransactionRepository.splitTransaction` + `SplitSlice`: atomically hides the parent (`isIgnored = true`) and inserts the child slices (inheriting merchant/account/time/type). Because every list and aggregation already filters `isIgnored = 0`, the parent drops out of totals/lists and the children roll up into reports, budgets and category breakdowns with no query changes.

---

## [Phase 3: MVP Build — Needs-a-tag review queue + merchant rules] — 2026-06-22

### Added
- `NeedsTagScreen` + `NeedsTagViewModel` + `Screen.NeedsTag` route (mockup #11): a review queue of uncategorized transactions. Each card shows a suggested category plus quick-pick chips, an "Always tag X as this" checkbox, and per-item skip; footer has "Skip all" and "Confirm all suggestions". Reached from a Needs-a-tag row in Settings.
- `SuggestCategoryUseCase`: suggests a category via the existing `CategoryRuleEngine` first, then a keyword fallback resolved to a real category.
- `TransactionDao.getUncategorized` / `setCategory` + repository methods.
- "Always tag X as this" inserts a high-priority (100) user `RuleEntity` so future captures of that merchant auto-tag — the same memory mechanism that makes a renamed/recategorized merchant stick going forward.

---

## [Phase 3: MVP Build — Subscriptions detection + screen] — 2026-06-22

### Added
- `DetectSubscriptionsUseCase` (pure, unit-tested): finds merchants with ≥2 similar-amount expense charges ~a month apart (26–35 day cadence, amounts within 20% of median) OR any transaction manually flagged `isRecurring`. Computes median amount, cadence, next-renewal estimate, and "unused for 60+ days".
- `SubscriptionsScreen` + `SubscriptionsViewModel` + `Screen.Subscriptions` route (mockup #10): dark per-month total header with active/unused pills, and "Renews this week" / "Not used in 60 days" / "Other active" sections. Reachable from a Subscriptions row in Settings.
- `DetectSubscriptionsUseCaseTest`: monthly detection, one-off ignored, irregular/varying-amount ignored, manual-flag, unused-after-60-days, income-never-a-subscription.

---

## [Phase 3: MVP Build — Budgets screen] — 2026-06-22

### Added
- `BudgetsScreen` + `BudgetsViewModel` + `Screen.Budgets` route: an overall budget ring (% used, spent / limit, and an "On pace for ₹X" linear projection from current daily burn), plus per-category cards with progress bars and over/under copy. Reuses the existing category-spend aggregation pattern and the per-category `monthlyBudgetInPaise` + overall `SettingsDataStore` budget.
- Entry points: a wallet FAB on the Reports → Breakdown tab and a "Budgets" row in Settings; "Edit" jumps to the Categories screen (where budgets are set).

---

## [Phase 3: MVP Build — Recurring flag (schema v3) + Mark recurring] — 2026-06-22

### Added
- `transactions.isRecurring` column (Room schema v2 → v3, `MIGRATION_2_3`); added to `TransactionEntity`, the `Transaction` domain model, and the mapper.
- `TransactionDao.setRecurring` + `TransactionRepository.setRecurring`; the transaction detail screen's "Mark recurring" button now toggles the flag (and shows "✓ Recurring" when set). Foundation for subscription detection.

---

## [Phase 3: MVP Build — Auto-create accounts + same-merchant account memory] — 2026-06-22

### Added
- Auto-create accounts from captured transactions: when a bank/card tail is seen for the first time, an `Account` is created automatically (named from the detected issuer — HDFC, SBI, Union Bank… — or source app, with " Card" appended for credit cards, typed SAVINGS/CREDIT_CARD, given a stable color). It then shows up in the Accounts list for manual edit and in the add/edit picker.
- "Same merchant → same account" memory: a UPI notification with no card tail (e.g. "paid to Swiggy") links to the account most recently used for that merchant, via `TransactionDao.getLatestAccountIdByMerchant`.
- `AccountNotificationParser.extractIssuer` / `isCreditCard` + tests; issuer/card-name detection across major Indian banks.

### Changed
- `KaasuNotificationListenerService`: account resolution now auto-creates on a new tail and falls back to merchant memory, instead of only linking to a pre-existing account.

---

## [Phase 3: MVP Build — Make the add/edit transaction screen fully functional] — 2026-06-22

### Fixed
- `AddEditTransactionScreen`: the Account / Date / Note rows were dead (all `onClick = null`), the "Account" row wrongly showed the merchant string, the `+N More` category pill did nothing, and there was no way to type a payee or note. Now:
  - **Payee** and **Note** are inline-editable text fields.
  - **Account** opens a bottom-sheet picker of registered accounts (or "No account").
  - **Date** opens a Material 3 date picker.
  - **+N More** opens a bottom-sheet with the full category list; the selected category stays visible in the pill row even when outside the first five.
- `AddEditViewModel`: now loads accounts, tracks `accountId`, and exposes `onAccountChange`/`onDateChange`; the saved transaction includes the selected `accountId`. Works for Expense / Income / Transfer in both add and edit modes.

---

## [Phase 3: MVP Build — Strict source allowlist (UPI/bank apps + SMS only)] — 2026-06-22

### Added
- `SourceApps`: a built-in allowlist of UPI/wallet/bank packages (GPay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, BharatPe, MobiKwik, Freecharge, Slice, FamPay + major bank apps) and system messaging-app packages (Google Messages, Samsung, AOSP, Oppo/Realme, Vivo, Xiaomi).
- `NotificationFilterTest`: covers payment-app pass, shopping/chat-app drop (even with a verb), bank-SMS pass and promo-SMS drop.

### Changed
- `NotificationFilter`: capture is now restricted to a strict allowlist — a payment/bank app (built-in list ∪ user-added bank sources) or the system messaging app. Every other app (food, grocery, shopping, social) is dropped outright. Messaging-app notifications still require a real transaction verb so only bank SMS pass, not promotional SMS.

---

## [Phase 3: MVP Build — Restrict capture to real transactions, drop shopping-app marketing] — 2026-06-22

### Fixed
- `NotificationFilter`: notifications from apps not in the known-payment/bank list now require an actual transaction verb (debited / credited / spent / withdrawn / deducted / paid to / sent to / received from / transferred to) plus a currency amount — a bare ₹ is no longer enough. This stops marketing from shopping/grocery/food apps (Swiggy, Zepto, Instamart, etc.) that merely quote a price (e.g. "Get Doritos @ ₹70", "Stock up… ₹1", "FLAT ₹200 OFF") from being captured, while still letting genuine bank transaction SMS through.
- `PromotionalDetector`: added `cashback` (bare), `stock up`, `for you`, and `tap to get` as promo markers so wallet/offer marketing inside allowlisted apps (e.g. "Add ₹250 & get instant ₹10 cashback Pay Amazon se") is dropped instead of being saved as CASHBACK income. Added `added to` as a transactional signal so real wallet credits ("₹100 added to your Paytm Wallet") are still kept.

### Added
- `PromotionalDetectorTest`: real-world marketing samples from device (tap-to-get, stock-up, wallet-cashback, flat-off) and a genuine wallet-credit kept-case.
- `TransactionParserTest.promo_wallet_cashback_rejected`: end-to-end guard for the cashback-marketing leak.

---

## [Phase 3: MVP Build — Parser hardening: reject offers, fix merchant & direction, UI redesign to match design system] — 2026-06-22

### Added
- `PromotionalDetector`: code-based filter that drops offer/deal notifications which merely quote a rupee amount (e.g. "Get ₹100 cashback, use code…", "Flat 50% off, shop now"). A notification is promotional only when it has a promo marker AND no transactional-confirmation signal, so genuine receipts — including real "cashback credited to A/c …" — are kept. Lives in code (not just the seeded `ignored_patterns`) so existing installs are protected without a migration.
- `PromotionalDetectorTest` plus promo/UNKNOWN-rejection cases in `TransactionParserTest` and junk-absorption regressions in `MerchantParserTest`.

### Changed
 Reworked the app's screens to match the finalized design mockups on the cream/forest palette: onboarding, dashboard, transactions, transaction detail, manual entry, reports (breakdown + monthly), settings, app lock, and the shared theme.
- Dashboard: spend card shows the month uppercased with an account-selector affordance and "₹X left of ₹Y"; the two stat cards are now This week (7-day sparkline) and Daily avg (with a ▾/▴ comparison pill vs last month's daily average). View model computes week total, sparkline, daily average and the previous-month delta (across the month boundary).
- Transactions: filter chips are category-based ("All · N" plus the most-used categories) instead of Expense/Income; header uses a funnel icon; cards show the linked account's last-four.
- Manual entry: category chips render a colored dot (from the category color) instead of the raw icon identifier text (fixed "receipt_long Bills" → "● Bills").
- Settings: renamed the sources section to "BANK SOURCES" / "+ Add bank"; reworded the account screen's SMS reference to "bank notifications" — keeping the notification-based, privacy-safe framing (no READ_SMS).
- `TransactionParser`: rejects promotional notifications up front; requires a resolved debit/credit direction (UNKNOWN is no longer saved — without direction we cannot tell if money moved in or out); evidence-based confidence (account/card tail, UPI/txn reference) replaces the decisive +15 known-app boost (now a minor +5); `MIN_CONFIDENCE` raised 40 → 70.
- `MerchantParser`: tightened `TO`/`AT`/`FROM` patterns to stop at more boundaries (`for`, `with`, `is`, `has`, `ref`, `upi`, `rs`, `inr`, `(`, and digit-led tokens) and bar stop-words from being absorbed; `clean()` now trims leading/trailing punctuation, collapses whitespace, and rejects junk/stop-word-only names. Fixes cases like "Swiggy for 5% offer" being stored as the merchant.
- `TransactionTypeParser`: explicit self-transfers ("to self") classify as TRANSFER; "to your account" deliberately excluded to avoid misclassifying "credited to your account" income.
- `DatabaseSeeder`: added a few promo phrases ("use code", "% off", "cashback offer", "voucher", "apply now") to seeded ignored patterns for fresh installs.

---

## [Phase 3: MVP Build — Account name in transaction list and detail] — 2026-06-12

### Changed
- `TransactionCard`: added optional `accountName` param; renders between category and time in the subtitle row (e.g. "Food · Union Bank · 10:49 AM")
- `TransactionsViewModel`: injects `GetAccountsUseCase`, includes `accountMap: Map<Long, Account>` in `TransactionsUiState`
- `TransactionsScreen`: looks up account display name from `accountMap` and passes it to `TransactionCard`
- `DetailUiState`: added `account: Account?`
- `TransactionDetailViewModel`: injects `AccountRepository`, resolves account by `tx.accountId` during load
- `TransactionDetailScreen.DetailFields`: shows "Account" row when the transaction has a linked account

---

## [Phase 3: MVP Build — Accounts screen (list + add/edit)] — 2026-06-11

### Added
- `AccountsScreen`: full list of registered accounts with colored initial chip, account type, masked last-4 ("•••• 0913"), and per-row delete with confirmation dialog; empty-state message guides users to add their first account
- `AccountsViewModel`: collects accounts flow, exposes delete action
- `AddEditAccountScreen`: form with display name, optional last-4 digits (numeric, max 4), account type chips (Savings / Current / Credit Card / Wallet / Prepaid), and color picker reusing `CATEGORY_COLORS`
- `AddEditAccountViewModel`: loads existing account for edit, validates name + last-4 length, converts hex color ↔ Int ARGB on save/load
- `Screen.Accounts` and `Screen.AddEditAccount` navigation routes
- `ColorUtils.toColorHex()` extension: converts Int ARGB to "#RRGGBB" hex string

### Changed
- `AppNavigation`: wired `AccountsScreen` and `AddEditAccountScreen` composables
- `SettingsScreen`: added "Accounts" row under Manage section (new `onNavigateToAccounts` param); uses `Icons.Default.AccountBalance`

---

## [Phase 3: MVP Build — Account tracking schema + auto-link pipeline] — 2026-06-11

### Added
- `AccountType` enum: SAVINGS, CURRENT, CREDIT_CARD, WALLET, PREPAID
- `Account` domain model and `AccountRepository` interface
- `AccountEntity` with index on `lastFourDigits`; `AccountDao` with `getByLastFour` for auto-matching
- `AccountMapper` (entity ↔ domain)
- `AccountRepositoryImpl`, bound in `RepositoryModule`
- `AccountDao` provided via `DatabaseModule`
- `AccountNotificationParser`: extracts masked last-4 account/card digits from Indian bank SMS ("A/c *0000", "Card ending 1234", "account XXXX7890", etc.)
- `AccountNotificationParserTest`: 12 tests covering Union Bank, HDFC, SBI, ICICI, Axis, credit card, generic patterns, and no-match cases
- `MIGRATION_1_2`: creates `accounts` table and adds nullable `accountId` column to `transactions`
- `GetAccountsUseCase`, `SaveAccountUseCase`, `DeleteAccountUseCase`
- Account auto-link in `KaasuNotificationListenerService.process()`: extracts last-four from notification text, looks up registered accounts, sets `accountId` when exactly one account matches

### Changed
- `KaasuDatabase` bumped to version 2; `ALL_MIGRATIONS` array registered
- `TransactionEntity` and `Transaction` domain model: added nullable `accountId: Long?`
- `TransactionMapper`: maps `accountId` in both `toDomain()` and `toEntity()`
- `KaasuNotificationListenerService` entry point: exposes `accountRepository()`
- `KaasuDatabase.ALL_MIGRATIONS` wired into `DatabaseModule` via `addMigrations(*...)`

---

## [Phase 3: MVP Build — Duplicate detection + source notification UI] — 2026-06-11

### Fixed
- `DuplicateChecker`: bank SMS (null merchant) now correctly detected as duplicate of the UPI app notification (named merchant) for the same payment within 2-minute window — previously both GPay and Union Bank entries were saved for every transaction, producing double entries
- `DuplicateCheckerTest`: added `isDuplicate_whenIncomingNullAndExistingHasMerchant`; renamed `notDuplicate_whenOnlyOneIsNull` to `notDuplicate_whenExistingNullAndIncomingHasMerchant` to reflect the directional nature of bank confirmation detection

### Added
- `TransactionDao.getRawTextById`: narrow SELECT query to fetch only rawText column, avoiding full entity load
- `TransactionRepository.getRawText` / `TransactionRepositoryImpl`: surfaces stored notification text to the domain layer
- `GetTransactionRawTextUseCase`: use case wrapper for rawText retrieval
- `DetailUiState.capturedText`: exposes the original notification text in the detail screen state
- `TransactionDetailViewModel`: fetches rawText via `GetTransactionRawTextUseCase` alongside the transaction load
- `TransactionDetailScreen.CapturedNotificationSection`: collapsible "Source notification" card at the bottom of the detail screen; lets users verify what notification text was parsed (Axio/Walnut-style)

---

## [Phase 3: MVP Build — Parser real-world coverage (round 4)] — 2026-06-11

### Fixed
- `MerchantParser`: added `PAID_YOU_PATTERN` for GPay income notifications — "MEENAKSHI . paid you ₹1.00 for lunch" format puts the sender name before "paid/sent you"; previously MerchantParser returned null for all INCOME "paid you" transactions

### Added
- `MerchantParserTest`: `paid_you_single_name`, `paid_you_multiword_name`, `sent_you_single_name`, `paid_to_expense_returns_destination_not_sender`
- `TransactionParserTest`: `gpay_paidYou_multiWordSender`

---

## [Phase 3: MVP Build — Parser real-world coverage (round 3)] — 2026-06-11

### Fixed
- `TransactionTypeParser`: added `INCOME_PHRASES` list (`"paid you"`, `"sent you"`) checked before `DEBIT_KEYWORDS` — GPay "MEENAKSHI . paid you ₹1.00" was being saved as EXPENSE because bare `"paid"` matched the debit list first
- `DatabaseSeeder.seedIgnoredPatterns`: replaced broad `"otp"` pattern with five specific OTP phrases (`"is the otp"`, `"otp is"`, `"otp for"`, `"your otp"`, `"one time password"`) — Union Bank appends "Never Share OTP/PIN/CVV" to ALL transaction SMS messages, so the broad pattern was filtering out legitimate credits from Union Bank

### Note
Both bugs were caught by live device testing. For the OTP fix to take effect on an existing install, clear app storage (Settings → Apps → Kaasu → Storage → Clear all data) or uninstall and reinstall — the `ignored_patterns` table is only seeded on fresh database creation.

---

## [Phase 3: MVP Build — Parser real-world coverage (round 2)] — 2026-06-11

### Fixed
- `TransactionTypeParser`: moved DEBIT_KEYWORDS check before CREDIT_KEYWORDS — UPI P2P debit notifications say "A/c debited Rs.X; Recipient credited", checking CREDIT first was misclassifying them as INCOME
- `MerchantParser.AT_PATTERN`: the old pattern greedily captured trailing date/time context ("at Swiggy on 01 FEB 2026 at 02:19 PM Avbl Limit: INR …" → captured everything). New pattern: (a) limits to 1–3 words with a negative lookahead that prevents stop words ("on", "via", "using", "through") from being absorbed as part of the merchant name; (b) outer lookahead requires a stop word or punctuation/end-of-string after the last captured word. Result: "at Swiggy on DATE" → "Swiggy", "at ZOMATO LIMITED on DATE" → "ZOMATO LIMITED"
- `DatabaseSeeder.seedIgnoredPatterns`: added "e-mandate" for recurring payment schedule reminders from IDFC FIRST Bank
- `TransactionTypeParser`: added "spent" to keyword test coverage (`spent_keyword` test added)

### Added
- `TransactionTypeParserTest`: `spent_keyword`, `debit_wins_over_credited_recipient`
- `MerchantParserTest`: 5 real-world AT_PATTERN tests covering single merchant, multi-word merchant, long single-word merchant, "via" stop-word, end-of-string
- `TransactionParserTest`: IDFC FIRST (INR + "at MERCHANT on DATE"), IDFC FIRST salary credit, IDFC FIRST P2P debit with recipient-credited text, SBI Card UPI Zepto, SBI Card Math Unicode Zepto

---

## [Phase 3: MVP Build — Parser real-world coverage] — 2026-06-11

### Fixed
- `TransactionTypeParser`: removed `"credit"` from `CREDIT_KEYWORDS` — it was causing all "Credit Card" notifications (SBI, IDFC FIRST, HDFC) to be mis-classified as INCOME; only `"credited"` (past tense) reliably signals an incoming transaction
- `AmountParser`: extended `Rs[.:]?` prefix pattern to also handle `Rs:` separator format used by Union Bank (was `Rs\.?`)
- `RawNotification.fullText()`: added Mathematical Unicode normalization; bank apps like SBI and Union Bank render notification text with Mathematical Sans-Serif/Bold code points (U+1D5A0–U+1D607) that ASCII keyword regexes could not match
- `TransactionParser.KNOWN_FINANCE_PACKAGES`: added IDFC FIRST Bank, SBI Card, Slice, Union Bank, CRED
- `DatabaseSeeder`: added 5 new known-finance app sources (IDFC FIRST, SBI Card, Slice, Union Bank, CRED); added 6 new ignored patterns (declined, e-statement, bill due, bill generated, min due, total due)

### Added
- `RawNotificationTest`: unit tests for Unicode normalization (Math Sans-Serif, Math Bold) and `fullText()` joining
- `AmountParserTest`: `Rs:` format tests (Union Bank real-world)
- `TransactionParserTest`: real-world tests for Union Bank (Unicode + Rs: format), SBI Credit Card (Unicode "spent"), IDFC FIRST, Slice, CRED; new package confidence-boost coverage

---

## [Phase 3: MVP Build — Week 8 — v1.0.0-rc1] — 2026-06-11

### Added
- `PrivacyDataScreen` — privacy promise card (what's stored locally, what never leaves the device) + Export and Delete actions
- `PrivacyDataViewModel` — two-step delete confirmation (request → confirm dialog); emits `deleteCompleteEvent` SharedFlow so the Screen shows a snackbar and pops back on success
- `DeleteAllDataUseCase` — hard-deletes all transactions, user-created categories (isDefault=0), and user rules (priority≥100); resets monthly budget to 0; leaves seeded categories and system rules intact
- **Budget setting** in SettingsScreen — tapping the Monthly Budget row opens a dialog with rupee input; shows current budget or "Not set" in the subtitle; supports clearing the budget
- **Notification access row** in SettingsScreen — shows "Active" (green) or "Disabled — tap to enable" (red) based on `NotificationManagerCompat.getEnabledListenerPackages`; tapping opens `ACTION_NOTIFICATION_LISTENER_SETTINGS`; status rechecks on every screen resume via `DisposableEffect`
- **Notification banner** on DashboardScreen — error-container card with "Enable" button that appears when notification access is disabled; disappears immediately when the user grants access and returns to the app
- `SettingsViewModel` — exposes `monthlyBudgetInPaise` Flow from DataStore; `setBudget()` and `clearBudget()` helpers
- `Screen.PrivacyData` route + `AppNavigation` wiring; "Export" in PrivacyData navigates to Reports tab

### Changed
- `SettingsScreen` "Coming soon" section replaced with fully wired rows (Categories, Monthly budget, Notification access, Privacy & data)
- `TransactionRepository` + `CategoryRepository` interfaces extended with `deleteAll()` / `deleteAllUserCreated()`; impls and DAOs updated accordingly
- `DuplicateCheckerTest.FakeTransactionRepository` updated to implement new `deleteAll()` stub

---

## [Phase 3: MVP Build — Week 7 — v0.9.0] — 2026-06-11

### Added
- `ReportsScreen` — replaces stub; month selector with previous/next arrows (capped at current month), monthly summary card (spent/earned/saved), 6-month spend bar chart, and top-6 category breakdown with proportional bars colored by category color
- `ReportsViewModel` — derives all report data in-memory from `getAll()` flow so it reacts to new transactions instantly; month selection via `_selectedMonth: MutableStateFlow<YearMonth>`; 6-month trend computed by filtering `allTransactions` per month without extra DB queries
- `CsvExporter` — writes all transactions to `cacheDir/exports/kaasu_transactions.csv` (UTF-8, RFC 4180 escaping); columns: Date, Time, Type, Amount (INR), Merchant, Category, Note, Source App, Manual
- CSV export FAB in ReportsScreen; export covers ALL transactions (not just selected month) as a full data backup
- Export shared via Android system share sheet (`ACTION_SEND`) using `FileProvider` — no storage permission required
- `FileProvider` registered in `AndroidManifest.xml` with `res/xml/file_paths.xml` pointing to `cacheDir/exports/`
- Export URI emitted via `SharedFlow<Uri>` from ViewModel; Screen launches the chooser intent (ViewModel never touches Android UI context directly)

---

## [Phase 3: MVP Build — Week 6 — v0.8.0] — 2026-06-11

### Added
- `DashboardScreen` — replaces stub; scrollable layout with month summary card, today's spend card, category breakdown (top 5 with progress bars), and last 5 recent transactions with "See all" link
- `DashboardViewModel` — combines `TransactionRepository.getByDateRange()` + `CategoryRepository.getAllActive()` + `SettingsDataStore.monthlyBudgetInPaise` reactively via `combine`; computes total spent, total income, today's spend, saved/deficit, and category breakdown fully in-memory
- Budget bar on the month summary card — shows remaining vs over-budget in green/red with a `LinearProgressIndicator`; only rendered if a monthly budget is set in DataStore
- Category breakdown card — top 5 expense categories ranked by amount, each with a proportional horizontal bar colored by the category's own color
- Tapping a recent transaction navigates to `TransactionDetailScreen`; "See all" navigates to `TransactionsScreen`
- Empty state message when no transactions exist for the month (with notification-access prompt)

---

## [Phase 3: MVP Build — Week 5 — v0.7.0] — 2026-06-11

### Added
- `CategoryRuleEngine` — classifies parsed transactions against active rules from Room; supports CONTAINS/EQUALS/STARTS_WITH/ENDS_WITH/REGEX match types and source-app-only rules; first matching rule (ordered by priority DESC) wins
- ~50 seeded merchant keyword rules (Swiggy/Zomato → Food, Amazon/Flipkart → Shopping, Uber/IRCTC → Travel, Netflix/Spotify → Subscriptions, Jio/Airtel → Recharge, etc.) using subquery inserts so category IDs are never hardcoded
- `CategoryRuleEngine` wired into `KaasuNotificationListenerService` — auto-captured transactions now get categoryId set before saving; categoryId logged (debug only) alongside amount and confidence score
- `CategoriesScreen` — full list grouped by type (Expense/Income/System) with sticky headers, color circle per category, DEFAULT badge for seeded categories, archive button for custom ones
- `AddEditCategoryScreen` — name field, type selector (Expense/Income), 8-color palette picker with checkmark on selected; saves via `SaveCategoryUseCase`
- `CategoriesViewModel` + `AddEditCategoryViewModel` — reactive list from DB, edit pre-fill via `SavedStateHandle`
- `SaveCategoryUseCase` + `ArchiveCategoryUseCase`
- `ColorUtils.kt` — `String.toComposeColor()` extension + `CATEGORY_COLORS` palette constant
- `SettingsScreen` replaced stub with a proper settings list; "Manage Categories" row navigates to categories; other rows shown greyed-out as coming soon
- Navigation routes for `Categories` and `AddEditCategory`

---

## [Phase 3: MVP Build — Week 4 — v0.6.0] — 2026-06-11

### Added
- `TransactionsScreen` — replaces stub; date-grouped `LazyColumn` with sticky headers, search bar toggle, All/Expense/Income filter chips, FAB for manual add, empty state
- `TransactionDetailScreen` — shows amount (colored by type), all fields, edit button in top bar, delete with confirmation dialog
- `AddEditTransactionScreen` — amount field, type selector (Expense/Income/Transfer chips), merchant, category dropdown (from DB), note; handles both new and edit modes
- 5 use cases: `GetTransactionsUseCase`, `GetTransactionByIdUseCase`, `GetAllCategoriesUseCase`, `SaveTransactionUseCase`, `DeleteTransactionUseCase`
- `TransactionsViewModel` — combines transactions flow + categories flow + search + filter reactively via `combine`; groups by date (Today/Yesterday/date string)
- `TransactionDetailViewModel` — loads transaction + category by ID; exposes delete action
- `AddEditViewModel` — pre-fills form from DB in edit mode; parses amount text to Long paise via `SavedStateHandle`-driven init
- `MoneyFormatter.kt` — `Long.formatRupees()` and `String.parseToPaise()` using BigDecimal arithmetic
- `TransactionCard` shared component — merchant name, category, time, amount with color sign by type
- Navigation routes for `TransactionDetail` and `AddEditTransaction`; bottom bar hidden on detail/edit screens

---

## [Phase 3: MVP Build — Week 3 — v0.5.0] — 2026-06-11

### Added
- `NotificationFilter` — drops non-finance notifications by known-app package list and keyword/amount heuristics; checks ignored-pattern DB table; caches results per session
- `AmountParser` — extracts INR amounts from ₹, Rs., and INR prefix/suffix formats; returns Long paise using BigDecimal arithmetic (no floating-point)
- `TransactionTypeParser` — classifies EXPENSE/INCOME/REFUND/CASHBACK/TRANSFER/UNKNOWN from notification keyword matching; CASHBACK and REFUND take priority over generic CREDIT
- `MerchantParser` — extracts merchant from "paid to / sent to / transferred to / at / received from" patterns and VPA handles; strips @domain from UPI VPAs for clean display; stops at "via/using/through" to avoid capturing payment app names
- `TransactionParser` — orchestrates the full parse pipeline; computes confidence score (amount=40, type=30, merchant=15, known-app=15); rejects below score 40
- `DuplicateChecker` — queries recent transactions within a 2-minute window by same amount and type; merchant similarity is case-insensitive substring match
- Wired `KaasuNotificationListenerService.onNotificationPosted` with full pipeline using `@EntryPoint` pattern (avoids Hilt 2.52 Kotlin-metadata issue on Services)
- `TransactionRepository.insertParsed` — saves parsed transaction with rawText to entity layer without exposing rawText in domain model
- Parser log gated by `BuildConfig.ENABLE_PARSER_LOGS` — only amount/type/confidence logged in debug; raw notification text never logged
- 5 unit test classes (76 total assertions) covering AmountParser formats, type keyword priority, merchant extraction patterns, end-to-end parsing with real-world GPay/PhonePe/Paytm/BHIM/bank notification strings, and DuplicateChecker logic

---

## [Phase 3: MVP Build — Week 2 — v0.4.0] — 2026-06-11

### Added
- **Room database** (`kaasu.db`, version 1) with 6 entities: transactions, categories,
  rules, app_sources, monthly_budgets, ignored_patterns
- **Indexes** on transactionTime, categoryId, sourceAppPackage, rawTextHash per spec
- **6 DAOs** with all queries needed through Phase 6: full CRUD, Flow-based observation,
  date range, category filter, full-text search, duplicate-detection query (same amount
  + type within time window), monthly spend/income aggregates
- **Domain models**: Transaction, Category, Rule with typed enums (TransactionType,
  CategoryType, MatchType); rawText deliberately excluded from Transaction — it never
  surfaces above the data layer
- **Repository interfaces** (TransactionRepository, CategoryRepository, RuleRepository)
  and their Hilt-injected implementations with entity↔domain mappers
- **SettingsDataStore** wrapping DataStore<Preferences>: onboarding_complete,
  permission_explanation_shown, monthly_budget_in_paise, app_theme
- **DatabaseSeeder** (RoomDatabase.Callback.onCreate, raw SQL): seeds 25 default
  categories (17 expense, 6 income, 2 system), 9 known finance app sources
  (GPay, PhonePe, Paytm, BHIM, Amazon Pay, ICICI, SBI, Axis, IndusInd), and 8
  system ignored patterns (otp, offer, loan, etc.)
- **4 Hilt modules**: DatabaseModule, RepositoryModule, DataStoreModule, AppModule
  (application-scoped CoroutineScope with SupervisorJob)
- **Room schema export** to `app/schemas/` — committed so migrations can be verified
- **CategoryDaoTest**: 3 instrumented in-memory DB tests covering insert/retrieve,
  archive filtering, and default-category delete protection
- `BuildConfig.ENABLE_PARSER_LOGS` wired — seeder and all DB writes production-safe

---

## [Phase 3: MVP Build — Week 1 — v0.3.0] — 2026-06-11

### Added
- `kaasu-android/` — full Android project scaffold (Kotlin + Jetpack Compose)
- Version catalog (`gradle/libs.versions.toml`) with all MVP dependencies locked:
  AGP 8.7.2, Kotlin 2.1.0, Compose BOM 2025.01, Hilt 2.52, Room 2.7, Navigation 2.8.5
- KSP instead of KAPT for faster annotation processing (Hilt + Room compilers)
- Material 3 theme with Kaasu brand palette (deep emerald green primary), dynamic
  color support on Android 12+, full dark theme color scheme
- Custom typography scale — `displayMedium` sized for large rupee amount display
- Navigation scaffold: `AppNavHost` with bottom nav (Home / Transactions / Reports /
  Settings) + separate onboarding graph that pops itself once complete
- Shell screens for all 5 destinations (Onboarding, Dashboard, Transactions, Reports,
  Settings) — each with Compose Preview, ready to be filled in phase by phase
- `KaasuNotificationListenerService` stub — registered in manifest so notification
  access permission flow can be tested end-to-end before Phase 3 parser work
- `BuildConfig.ENABLE_PARSER_LOGS` flag — `true` in debug, `false` in release, enforcing
  the hard rule that raw notification text never reaches release logs
- `ENABLE_PARSER_LOGS` wired into `buildTypes` so the compiler eliminates log calls in
  release via R8/ProGuard
- Adaptive launcher icon (emerald green background, ₹ foreground) for API 26+
- `minSdk = 26` — covers ~97% of active Android devices, satisfies all MVP dependencies
- ProGuard rules for Room, Hilt, Vico, and the notification listener

### Notes
- No bank login, no SMS permission, no analytics. Privacy constraints from CLAUDE.md
  are enforced at the manifest level from day one.

---

## [Phase 0: Foundations — v0.2] — 2026-06-11

### Changed
- Replaced basic commit strategy with full professional developer workflow: branch naming
  conventions, step-by-step branch→PR→merge flow, pre-commit checklist, pre-push/PR
  checklist, PR description template, and commit message format
- Removed AI tool attribution from commit message format; commits are authored by the
  developer only

---

## [Phase 0: Foundations — v0.1] — 2026-06-11

### Added
- Full project documentation suite:
  - `PRODUCT_BLUEPRINT.md` — product vision, positioning, and core promise
  - `MVP_SCOPE.md` — feature set, acceptance criteria, success metrics
  - `TECHNICAL_ARCHITECTURE.md` — layered architecture, notification pipeline, parser design, security
  - `DATABASE_SCHEMA.md` — Room entity definitions, field notes, indexing strategy
  - `PERMISSION_STRATEGY.md` — permission philosophy, manifest rules, onboarding copy
  - `PRIVACY_POLICY_DRAFT.md` — draft privacy policy for Play Store submission
  - `PLAY_STORE_CHECKLIST.md` — pre-launch checklist for Google Play
  - `TESTING_CHECKLIST.md` — manual and automated test plan
  - `ROADMAP.md` — phased delivery plan (Phase 0 → Phase 9)
  - `MANIFEST.md` — document index
- `CLAUDE.md` — AI assistant guidance covering architecture, data rules, permission constraints
- `CHANGELOG.md` — this file; tracks app evolution from start to finish

---

<!-- Future entries go above this line, newest on top -->
