# Contributing to Kaasu

Thanks for considering a contribution. Kaasu is a personal-use app that happens to be open source,
so the bar is "does this make the app better for someone tracking their own money" rather than
"does this grow the product".

## The one rule that matters most

> ### ⚠️ Never commit a real bank message, notification or statement.
>
> Parser work naturally starts from a message you actually received. That message contains a real
> account tail, a real reference number, and often **another person's name**. Those people did not
> agree to appear in a public repository.
>
> Before a sample goes in a test, change:
> - **names** of people → an invented name of the same shape (`RAVI KUMAR S`)
> - **account and card tails** → `XX1234`, `*0000`
> - **reference / RRN / UPI transaction numbers** → `100000000001`
> - **balances** → a round invented figure
>
> Keep the *sentence structure* exactly as the bank wrote it — that is the only part the parser
> cares about, and the only part worth documenting.
>
> Real statement files belong in `Statements/`, which is gitignored and must stay that way.

## What's most useful

**Parser coverage is the highest-value contribution.** Kaasu can only categorise what it can read,
and every bank words its messages differently. If your bank isn't handled, adding it helps everyone
on that bank.

Also welcome: bug fixes, accessibility improvements, and OEM-specific notification quirks (Samsung,
Xiaomi, Realme and OnePlus all behave differently).

## Adding support for a new bank

1. **Capture the format.** Note how your bank words a debit, a credit and a UPI transfer. They are
   usually three different sentences.
2. **Anonymise it** — see the rule above.
3. **Write the test first**, in `app/src/test/java/com/kaasu/app/notification/parser/`. A failing
   test that documents a real format is a useful contribution even on its own.
4. **Add the pattern** to the relevant parser:
   - amounts → `AmountParser`
   - debit vs credit → `TransactionTypeParser`
   - who was paid → `MerchantParser`
   - the note the payer typed → `NoteParser`
5. **Check you didn't break the others** — `./gradlew test`. The parsers are shared by all four
   capture channels, so a greedy new regex can quietly break a bank you never touched.

New merchant patterns run *after* the existing ones, and narrow format-specific patterns must come
before loose fallbacks like the VPA handle matcher, which will match almost anything.

## Development setup

```bash
git clone https://github.com/abdulhakeem19/kaasu.git
cd kaasu/kaasu-android
./gradlew test          # should pass before you change anything
./gradlew installDebug  # device recommended; capture can't be tested on an emulator
```

JDK 17, Android Studio Ladybug or newer, minSdk 26.

## Before you open a pull request

- [ ] `./gradlew test` passes
- [ ] `./gradlew lint` adds no new warnings
- [ ] No real personal data in any test, comment or commit message
- [ ] No `Log.d` / `println` / `TODO` left in production paths
- [ ] No raw notification or SMS text logged in a release path
- [ ] `CHANGELOG.md` has an entry
- [ ] A schema change ships with a Room migration

## Branches and commits

```
feature/xxx   new screen or capability
fix/xxx       bug fix
refactor/xxx  internal restructure, no behaviour change
docs/xxx      documentation only
chore/xxx     build config, dependencies, tooling
```

Never commit directly to `main`.

Commit messages use `<type>: <short imperative summary>` and **explain the why in the body**, not
the what — the diff already shows the what. What problem did this solve, what did you decide, what
constraint is non-obvious?

```
fix: prevent duplicate transactions from PhonePe grouped notifications

PhonePe posts an updated grouped notification after every transaction.
The old code saved each update as a new entry. Now checks rawTextHash
plus amount within a 2-minute window before inserting.
```

## Things that will be declined

- **Anything that uploads transaction data.** Local-only is the entire point, not a default.
- **Analytics that touch transaction content.** Merchant names, amounts and notes never leave.
- **Logging raw captured text in release builds.**
- **Soft-deleting user data.** Deletion means deletion.
- **Floating-point money.** Amounts are integer paise, always.
- **Active accessibility automation.** The accessibility channel reads what is already on screen and
  must never tap, navigate or automate anything.

## Code of conduct

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).
