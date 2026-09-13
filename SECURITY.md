# Security Policy

## Reporting a vulnerability

**Please don't open a public issue for a security or privacy problem.**

Report it privately through
[GitHub Security Advisories](https://github.com/abdulhakeem19/kaasu/security/advisories/new), or by
email to **abdul.hakeem5764@gmail.com** with `[kaasu security]` in the subject.

Please include what the issue is, how to reproduce it, and what an attacker could actually get. You
should get a first response within about a week — this is a personal project maintained in spare
time, not a funded product, so please size your expectations accordingly.

## What counts as a security issue here

Kaasu holds a complete record of someone's spending and never sends it anywhere. The things that
matter most are therefore:

- **Any path that sends transaction data off the device.** There should be none. If you find one,
  that is the most serious class of bug this project can have.
- **Raw notification or SMS text reaching a release-build log**, where other apps with log access
  could read it.
- **Another app being able to read `kaasu.db`**, the DataStore preferences, or an exported backup.
- **Bypassing the app lock** (biometric or PIN) to reach transaction data.
- **The accessibility service reading outside its declared scope** — it is restricted to payment-app
  packages and to window-change events, and must never be able to observe PIN entry or arbitrary
  text input.
- **Backup or CSV export writing transaction data somewhere world-readable.**

## Not security issues

- Requiring notification, SMS or accessibility permission. These are the app's documented function —
  see [PERMISSION_STRATEGY.md](docs/PERMISSION_STRATEGY.md).
- A parser mis-reading a message, or missing a bank. Those are ordinary bugs; open a normal issue.
- Anything requiring physical access to an unlocked, already-authenticated device.

## A note on installing this

Kaasu is not on Google Play. Official builds are published as signed APKs on the
[releases page](../../releases), built by GitHub Actions from a tagged commit — no binary is
uploaded from anyone's machine.

**Only install Kaasu from this repository's releases, or build it from source.** An app holding
notification, SMS and accessibility access is worth attacking, and you cannot tell what is inside
an APK someone else compiled. Every release publishes a SHA-256 checksum next to the APK; verifying
it takes one command:

```bash
sha256sum kaasu-1.0.0.apk
```

If a Kaasu build is offered anywhere else — a mirror, an APK site, a chat forward — treat it as
untrusted regardless of how legitimate it looks.

## Supported versions

Kaasu is a personal project maintained in spare time. Fixes land on `main` and go out in the next
release; there is no backport process for older tags. If you are running an old APK, updating is
the fix.
