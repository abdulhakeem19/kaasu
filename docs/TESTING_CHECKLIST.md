# Kaasu Testing Checklist

## 1. Testing Goals

Kaasu must be tested for:

- Notification capture reliability
- Parser accuracy
- Duplicate prevention
- Category accuracy
- Dashboard correctness
- Privacy controls
- Device/OEM behavior
- Play Store readiness

## 2. Unit Testing

## Parser Tests

Test amount extraction:

- [ ] `₹250`
- [ ] `Rs.250`
- [ ] `Rs 250`
- [ ] `INR 250`
- [ ] `₹1,250.50`
- [ ] `INR 2,000.00`

Test debit detection:

- [ ] paid
- [ ] debited
- [ ] spent
- [ ] sent
- [ ] purchase
- [ ] withdrawn
- [ ] charged
- [ ] payment successful

Test credit detection:

- [ ] credited
- [ ] received
- [ ] deposited
- [ ] refund
- [ ] cashback
- [ ] reversed

Test ignore detection:

- [ ] OTP
- [ ] offer
- [ ] loan
- [ ] pre-approved
- [ ] reward points
- [ ] sale
- [ ] discount
- [ ] statement generated

## Sample Parser Inputs

```text
Paid ₹250 to SWIGGY via Google Pay
Rs.500 debited from A/c XX1234
You have received ₹1,000 from Rahul
INR 799 spent on your HDFC Credit Card
Payment of Rs.1200 successful to Amazon
₹350 paid to Uber using PhonePe
Rs 200 credited to your account
Refund of ₹499 processed successfully
```

## Expected Tests

- [ ] Amount extracted correctly
- [ ] Transaction type detected correctly
- [ ] Merchant extracted when possible
- [ ] Source app stored
- [ ] Confidence score generated
- [ ] Promotional messages ignored
- [ ] OTP messages ignored

## 3. Duplicate Detection Tests

Test duplicates when:

- [ ] Same notification is posted twice
- [ ] Payment app and bank app both send same transaction
- [ ] Notification group updates
- [ ] App restarts
- [ ] Device reboots
- [ ] Same amount paid to same merchant within short time
- [ ] Same amount paid to different merchant within short time

## 4. Database Tests

- [ ] Insert transaction
- [ ] Update transaction
- [ ] Delete transaction
- [ ] Query by month
- [ ] Query by category
- [ ] Query by date range
- [ ] Query dashboard totals
- [ ] Export CSV
- [ ] Delete all data
- [ ] Migration test from version 1

## 5. UI Tests

## Onboarding

- [ ] Welcome screen loads
- [ ] Privacy screen loads
- [ ] Permission explanation screen loads
- [ ] Settings redirect works
- [ ] Permission enabled state works
- [ ] Permission disabled state works
- [ ] Budget setup works
- [ ] Skip budget works

## Dashboard

- [ ] Empty state works
- [ ] Monthly spend shown
- [ ] Today spend shown
- [ ] Budget remaining shown
- [ ] Recent transactions shown
- [ ] Category chart shown
- [ ] No crash with zero transactions

## Transactions

- [ ] Transaction list loads
- [ ] Search works
- [ ] Date filter works
- [ ] Category filter works
- [ ] Source app filter works
- [ ] Edit transaction works
- [ ] Delete transaction works
- [ ] Manual add works

## Reports

- [ ] Monthly summary works
- [ ] Category breakdown works
- [ ] Top merchants works
- [ ] Biggest spends works
- [ ] CSV export works

## Settings

- [ ] Privacy page opens
- [ ] Delete all data works
- [ ] Export works
- [ ] Open permission settings works
- [ ] App version visible

## 6. Device Testing

Test on at least:

- [ ] Samsung
- [ ] Redmi/Xiaomi
- [ ] Realme
- [ ] Oppo/Vivo
- [ ] OnePlus
- [ ] Pixel/Motorola

## Android Versions

- [ ] Android 10
- [ ] Android 11
- [ ] Android 12
- [ ] Android 13
- [ ] Android 14
- [ ] Android 15
- [ ] Android 16 if available

## 7. Real-World Transaction Testing

Test with:

- [ ] Google Pay payment
- [ ] PhonePe payment
- [ ] Paytm payment
- [ ] BHIM payment
- [ ] UPI received money
- [ ] Bank debit alert
- [ ] Bank credit alert
- [ ] Credit card spend
- [ ] Wallet cashback
- [ ] Failed transaction
- [ ] Refund
- [ ] Subscription payment
- [ ] Recharge payment
- [ ] Food delivery payment
- [ ] Shopping payment
- [ ] Travel payment

## 8. Privacy Testing

- [ ] App works without account
- [ ] App works without internet
- [ ] No transaction data uploaded in MVP
- [ ] Delete all data clears database
- [ ] Export works locally
- [ ] No raw notification logs in release
- [ ] No unnecessary permissions in manifest
- [ ] Privacy policy matches app behavior

## 9. Battery and Performance Testing

- [ ] App does not run unnecessary background loops
- [ ] Notification listener work is lightweight
- [ ] Dashboard loads quickly
- [ ] Large transaction list performs well
- [ ] CSV export works with many transactions
- [ ] No major battery drain after 24 hours

## 10. Beta Testing Checklist

- [ ] 20 beta users onboarded
- [ ] Feedback form created
- [ ] Parser issue reporting flow ready
- [ ] Version number visible
- [ ] Known issue list maintained
- [ ] Weekly feedback review done

## 11. Release Smoke Test

Before every release:

- [ ] Fresh install works
- [ ] Onboarding works
- [ ] Permission flow works
- [ ] Manual add works
- [ ] Dashboard works
- [ ] Delete all data works
- [ ] Export works
- [ ] App restart works
- [ ] No debug logs
- [ ] No crash on launch
