# Kaasu MVP Scope

## Personal-use pivot (2026-08-30)

This document was written when Kaasu was scoped for a public Play Store MVP. Kaasu has since pivoted to a personal-use-only build for the repo owner — no store submission, no other users. Constraints below that existed specifically to keep the app Play-eligible (notably "no SMS permission") have been lifted: SMS capture is now implemented (`feature/sms-capture`), bank/UPI statement import is now implemented (`feature/statement-import`, `feature/statement-import-real-formats`), and Accessibility Service capture is in progress on a separate branch. The rest of this document — categorization, dashboard, reports, etc. — is unaffected and still describes current scope reasonably well.

## MVP Goal

The MVP should answer one question clearly:

> Where did my money go this month?

Kaasu MVP should not try to solve all personal finance problems. It should focus on automatic local transaction tracking and clear spending visibility.

## MVP User Flow

```text
Install app
→ Understand privacy promise
→ Enable notification access
→ Set monthly budget
→ App captures transaction notifications
→ App categorizes spends
→ User views dashboard
→ User corrects categories
→ App learns rules
→ User reviews monthly report
```

## MVP Feature Set

## 1. Onboarding

### Screens

- Splash screen
- Welcome screen
- Privacy promise screen
- Notification permission explanation screen
- Permission status screen
- Monthly budget setup screen

### Acceptance Criteria

- User understands what Kaasu does.
- User understands why notification access is needed.
- User understands that no bank login or cloud sync is required.
- User can continue after permission is enabled.
- User can skip budget setup if needed.

---

## 2. Notification Capture

### Scope

Capture transaction-related notifications from payment, bank, wallet, and card apps.

### Supported Initially

- Google Pay
- PhonePe
- Paytm
- BHIM
- Common bank apps
- Credit card apps
- Wallet apps

### Acceptance Criteria

- App detects posted notifications.
- App filters non-financial notifications.
- App stores only required transaction-relevant data.
- App shows permission disabled state if access is turned off.

(Originally also required "App does not request SMS permission" — lifted after the personal-use pivot; see note at top of this document. SMS is now an additional capture channel alongside notifications.)

---

## 3. Parser Engine

### Extract

- Amount
- Currency
- Debit/credit/refund/transfer type
- Merchant or person name
- Source app
- Timestamp
- Confidence score
- Raw notification text/reference

### Acceptance Criteria

- Amount is extracted from common formats like ₹250, Rs.250, INR 250.
- Debit and credit are detected using keywords.
- Duplicate transactions are minimized.
- Low-confidence items can be marked as uncategorized or review-needed.

---

## 4. Categorization

### Default Categories

- Food
- Travel
- Shopping
- Bills
- Recharge
- Subscriptions
- Groceries
- Rent
- Health
- Education
- Entertainment
- Fuel
- Family
- Personal
- Cash
- Other
- Uncategorized
- Transfer
- Refund
- Income

### Acceptance Criteria

- Known merchants are categorized automatically.
- User can change category manually.
- User can save correction as future rule.
- Rules apply to future transactions.

---

## 5. Dashboard

### Must Show

- This month’s total spend
- Today’s spend
- Budget remaining
- Category breakdown
- Recent transactions
- Top category
- Daily average

### Acceptance Criteria

- Dashboard updates when new transactions are saved.
- Dashboard handles empty state.
- Dashboard handles no permission state.
- Dashboard is readable and simple.

---

## 6. Transactions

### Features

- List all transactions
- Search by merchant/note
- Filter by date
- Filter by category
- Filter by source app
- Filter by debit/credit
- Edit transaction
- Delete transaction
- Manual add

### Acceptance Criteria

- User can find and edit any transaction.
- User can manually add cash/missed expenses.
- User can mark items as transfer/refund.
- Deleted transactions are removed from summaries.

---

## 7. Reports

### Must Show

- Monthly total spend
- Income vs expense
- Category-wise spend
- Top merchants
- Biggest transactions
- Daily spend trend
- CSV export

### Acceptance Criteria

- Reports are generated from local database.
- User can select current/previous month.
- CSV export includes amount, type, category, merchant, date, source, note.

---

## 8. Settings

### Must Include

- Privacy and data
- Notification access status
- Supported apps
- Categories
- Rules
- Export
- Delete all data
- About
- App version

### Acceptance Criteria

- User can delete all local data.
- User can export data.
- User can access privacy policy.
- User can open Android notification access settings.

---

## Out of Scope for MVP

- Bank login
- Account Aggregator
- Cloud sync
- AI chatbot
- Credit score
- Loan offers
- Investment advice
- Family sharing
- iOS
- Web dashboard
- Receipt scanning
- OCR
- Multi-device sync
- Subscription cancellation automation

## MVP Completion Checklist

- [ ] Onboarding complete
- [ ] Notification permission flow complete
- [ ] Transaction notification capture working
- [ ] Parser working for sample payment messages
- [ ] Duplicate detection added
- [ ] Categories and rules working
- [ ] Dashboard working
- [ ] Transaction list working
- [ ] Manual expense entry working
- [ ] Transaction edit/delete working
- [ ] Reports working
- [ ] CSV export working
- [ ] Delete all data working
- [ ] Privacy policy draft ready
- [ ] Play Store checklist ready (not applicable post-pivot — Kaasu is not being published; kept for historical record)

## MVP Success Metrics

- 90%+ amount extraction accuracy during beta
- Less than 5% duplicate rate
- 20+ beta users
- 7-day retention above 25%
- At least 10 transactions captured per active user/week
- Less than 20% transactions need manual correction
