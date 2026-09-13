# Kaasu Roadmap

## Product Strategy

Kaasu should grow carefully. The first priority is trust and accuracy, not feature count.

The roadmap follows this order:

```text
Reliability → Trust → Reports → Power features → Optional sync/AI
```

---

# Phase 0: Preparation

## Goal

Prepare product, technical, design, and policy foundation before development.

## Tasks

- [ ] Finalize name: Kaasu
- [ ] Confirm name availability
- [ ] Create GitHub repo
- [ ] Create documentation
- [ ] Prepare UI/UX design
- [ ] Prepare privacy policy draft
- [ ] Prepare parser sample messages
- [ ] Prepare Play Store checklist
- [ ] Prepare validation landing page

## Deliverables

- README
- Product blueprint
- MVP scope
- Technical architecture
- Database schema
- Permission strategy
- Privacy policy draft
- Play Store checklist
- Testing checklist

---

# Phase 1: Validation

## Goal

Confirm that users want a private UPI expense tracker.

## Duration

1–2 weeks.

## Tasks

- [ ] Create landing page
- [ ] Create simple product mockups
- [ ] Post on LinkedIn/WhatsApp/Reddit
- [ ] Talk to 20–30 target users
- [ ] Collect beta signup list
- [ ] Validate pricing expectation

## Success Criteria

- [ ] 50+ interested users
- [ ] 20+ beta testers
- [ ] 10+ users willing to pay
- [ ] Strong repeated pain around manual tracking/privacy

---

# Phase 2: Notification Prototype

## Goal

Prove the hardest technical part first.

## Duration

1 week.

## Features

- [ ] Notification access flow
- [ ] Raw notification capture
- [ ] Finance notification filter
- [ ] Amount extraction
- [ ] Local save
- [ ] Debug list screen

## Success Criteria

- [ ] Captures GPay notifications
- [ ] Captures PhonePe notifications
- [ ] Captures Paytm notifications
- [ ] Captures at least one bank/card app notification
- [ ] Ignores most non-finance notifications
- [ ] Works across at least 3 devices

---

# Phase 3: MVP Build

## Goal

Build the first usable version.

## Duration

6–8 weeks.

## Milestones

## Week 1: App Foundation

- [ ] Kotlin Compose project
- [ ] Navigation
- [ ] Material 3 theme
- [ ] App icons placeholder
- [ ] Basic screen shell

## Week 2: Local Data

- [ ] Room setup
- [ ] DataStore setup
- [ ] Entities
- [ ] DAO
- [ ] Repository
- [ ] Seed default categories

## Week 3: Notification + Parser

- [ ] Notification listener
- [ ] Parser engine
- [ ] Amount parser
- [ ] Type parser
- [ ] Merchant parser
- [ ] Duplicate checker

## Week 4: Transactions

- [ ] Transaction list
- [ ] Transaction detail
- [ ] Edit transaction
- [ ] Manual add
- [ ] Delete transaction

## Week 5: Categories + Rules

- [ ] Category screen
- [ ] Custom categories
- [ ] Rule engine
- [ ] Save correction as rule
- [ ] Apply rules automatically

## Week 6: Dashboard

- [ ] Monthly spend
- [ ] Today spend
- [ ] Budget remaining
- [ ] Category breakdown
- [ ] Recent transactions
- [ ] Empty states

## Week 7: Reports + Export

- [ ] Monthly report
- [ ] Top categories
- [ ] Top merchants
- [ ] Biggest spends
- [ ] CSV export

## Week 8: Privacy + Polish

- [ ] Privacy screen
- [ ] Delete all data
- [ ] Settings
- [ ] Permission disabled state
- [ ] Error states
- [ ] UI polish

---

# Phase 4: Private Beta

## Goal

Test with real users and real payment notifications.

## Duration

2–3 weeks.

## Tasks

- [ ] Recruit 20–50 users
- [ ] Share beta build
- [ ] Collect device/app details
- [ ] Collect missed transaction examples
- [ ] Fix parser issues
- [ ] Fix category rules
- [ ] Fix onboarding confusion
- [ ] Improve performance

## Success Criteria

- [ ] 90%+ amount detection accuracy
- [ ] Less than 5% duplicate transactions
- [ ] 25%+ 7-day retention
- [ ] Users understand privacy flow
- [ ] No major battery complaints

---

# Phase 5: Public Launch — SUPERSEDED (2026-08-30)

**Not applicable.** Kaasu pivoted to a personal-use-only build for the repo owner and will not be published to Google Play or any other store. This phase is kept below as the historical plan, not as a live target.

Separately, outside this phase structure, the owner has since built and is building capture channels that go beyond what this roadmap originally scoped for a public MVP: direct SMS capture (implemented, `feature/sms-capture`), bank/UPI statement import (implemented, `feature/statement-import` and `feature/statement-import-real-formats`), and Accessibility-Service-based capture reading UPI apps' own on-screen transaction history (in progress, separate branch, not yet complete or merged).

## Goal

Launch Kaasu on Google Play.

## Tasks

- [ ] Final app icon
- [ ] Feature graphic
- [ ] Screenshots
- [ ] Store listing
- [ ] Privacy policy page
- [ ] Data Safety section
- [ ] Content rating
- [ ] Production AAB
- [ ] Internal test release
- [ ] Production rollout

## Launch Strategy

Start with a small rollout:

```text
10% → 25% → 50% → 100%
```

Increase rollout only if crashes and reviews are stable.

---

# Phase 6: v1.1 Reliability Update

## Goal

Improve transaction accuracy and supported apps.

## Features

- [ ] More bank patterns
- [ ] More UPI app patterns
- [ ] Better duplicate detection
- [ ] Parser confidence UI
- [ ] User report wrong transaction flow
- [ ] Weekly summary notification

---

# Phase 7: v1.2 Pro Features

## Goal

Add paid value without harming trust.

## Features

- [ ] Unlimited custom rules
- [ ] Advanced reports
- [ ] Category budgets
- [ ] CSV/JSON export
- [ ] App lock
- [ ] Recurring payment detection

## Monetization

- One-time Pro unlock: ₹299–₹499

---

# Phase 8: v1.3 Optional Intelligence

## Goal

Add useful intelligence without making AI core.

## Features

- [ ] Smart monthly summary
- [ ] Unusual spend detection
- [ ] Natural language query
- [ ] Better categorization suggestions

## Rule

AI must be optional and privacy-explained. Do not send transaction data to AI APIs without clear consent.

---

# Phase 9: v2 Expansion

## Possible Features

- [ ] Encrypted local backup
- [ ] Optional Google Drive backup
- [ ] Optional cloud sync
- [ ] Family/shared budget
- [ ] Regional languages
- [ ] Receipt scanning
- [ ] iOS app
- [ ] Web dashboard

## Not a Priority

- [ ] Loans
- [ ] Credit score
- [ ] Investment advice
- [ ] Bank account linking
- [ ] Account Aggregator unless properly regulated/partnered

---

# Long-Term Vision

Kaasu can become the trusted private money layer for Indian users.

But the foundation must remain:

```text
No ads.
No loans.
No dark patterns.
User data under user control.
```
