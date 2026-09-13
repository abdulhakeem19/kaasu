# Kaasu Product Blueprint

## Personal-use pivot (2026-08-30)

This document was written to position Kaasu as a Play Store product for other people ("Target Users," competitive differentiation vs. other finance apps, Play Store policy risk). Kaasu has since pivoted to a personal-use-only build for the repo owner — it will not be published, so there is no target market, no competitors to differentiate against in the market sense, and no Play policy risk. The vision/pillars below are kept as historical context for why the app is shaped the way it is; the passages that specifically assumed a public product (notably the "Capture method" differentiation row and the Play Store policy risk row, below) have been annotated rather than rewritten wholesale, since most of this document's product thinking still describes the app the owner actually wants.

## 1. Product Name

**Kaasu**

## 2. Play Store Title

**Kaasu: Private UPI Expense Tracker**

## 3. Tagline

**Your money. Your phone. No cloud.**

## 4. One-Line Description

Kaasu is a private Android expense tracker for India that reads transaction notifications locally and shows where money goes without ads, loans, signup, bank login, or cloud sync.

## 5. Product Vision

Build the most trusted local-first expense tracker for Indian users who want automatic spending clarity without giving financial data to another fintech company.

## 6. Core Problem

Indian users make frequent UPI, card, wallet, and bank transactions every day. Manual expense tracking is repetitive, boring, and easy to abandon. Existing finance apps often create distrust through ads, lending offers, account linking, spammy upsells, subscriptions, or cloud dependency.

## 7. Product Solution

Kaasu uses Android notification access to detect transaction alerts from supported payment, bank, card, and wallet apps. It processes data locally, categorizes spending, and presents a simple dashboard and monthly reports.

## 8. Target Users

### Primary Users

- Salaried employees
- Students
- Freelancers
- UPI-heavy users
- Privacy-conscious Android users
- Users tired of loan/credit-focused finance apps

### Secondary Users

- Small service providers
- Couples or families in future versions
- People moving away from Excel/manual tracking

## 9. Initial Geographic Focus

- Tamil Nadu
- Kerala
- Karnataka
- Andhra Pradesh
- Telangana
- Android-first Indian users

The UI should be English-first initially, with future regional language support.

## 10. Positioning Statement

For Indian Android users who want spending clarity without financial surveillance, Kaasu is a private UPI expense tracker that processes transaction alerts locally and gives clean monthly insights without ads, loans, bank login, or forced cloud sync.

## 11. Core Differentiation

| Area | Typical Finance Apps | Kaasu |
|---|---|---|
| Data model | Cloud-first | Local-first |
| Onboarding | Signup/account required | No account in MVP |
| Monetization | Ads, loans, subscriptions | Optional one-time Pro |
| Trust | Vague privacy claims | Privacy is the product |
| Capture method | SMS/account linking/cloud | Notification-first (historical positioning — for the owner's own personal build, Kaasu now also uses SMS and statement import directly, since there's no public audience to differentiate for) |
| Target | Broad finance management | Spending clarity |
| Complexity | Feature-heavy | Simple and focused |

## 12. Brand Principles

- Privacy over growth hacks
- Utility over fintech noise
- Local-first by default
- Clear permission explanations
- No lending, no ads, no spam
- User can delete everything anytime
- Build trust before monetization

## 13. Product Pillars

### Pillar 1: Automatic Tracking

Capture transaction alerts with minimal manual effort.

### Pillar 2: Local Privacy

Process and store transaction data on the device.

### Pillar 3: Spending Clarity

Show simple monthly summaries, not complex finance dashboards.

### Pillar 4: User Control

Users can edit, delete, categorize, export, and reset data anytime.

## 14. User Jobs To Be Done

- “Help me know where my salary went this month.”
- “Help me track UPI spends without entering everything manually.”
- “Help me use a finance app without ads, loans, or spam.”
- “Help me see my food, travel, shopping, and subscription spending.”
- “Help me keep my financial data on my phone.”

## 15. Main Use Cases

1. Automatic UPI expense capture
2. Monthly spending dashboard
3. Manual cash expense entry
4. Category correction and learning rules
5. Monthly CSV export
6. Local deletion/reset

## 16. MVP Promise

Kaasu v1 will not try to become a full finance platform. It will focus on one loop:

```text
Transaction notification → Parse → Categorize → Show dashboard → User corrects → Rules improve
```

## 17. Monetization Direction

### Beta

Free.

### v1

Free core with optional one-time Pro unlock.

### Pro Unlock Ideas

- Unlimited rules
- Advanced reports
- CSV/JSON export
- Custom categories
- App lock
- Recurring payment detection
- Encrypted backup later

### Avoid Forever

- Ads
- Lending leads
- Loan offers
- Credit card spam
- Selling data
- Dark patterns

## 18. Success Definition

Kaasu succeeds if users trust it enough to keep it installed and it accurately captures the majority of their real transactions without requiring manual entry.

## 19. Risks

| Risk | Impact | Mitigation |
|---|---:|---|
| Users fear notification access | High | Strong privacy onboarding |
| Parser misses transactions | High | Beta test with real notifications |
| OEM background restrictions | Medium | Device-specific testing |
| Low willingness to pay | Medium | Free core + low-cost Pro |
| Competitors copy features | Medium | Trust, UX, local-first positioning |
| Play Store policy friction | N/A post-pivot | Not applicable — Kaasu is a personal-use build and is not being submitted to Google Play |

## 20. Future Product Expansion

- Regional language support
- App lock
- Encrypted local backup
- Optional cloud sync
- Receipt OCR
- AI monthly insights
- Family/shared budgets
- iOS app
- Web dashboard
