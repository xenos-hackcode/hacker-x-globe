# What we'd need

- **Reconnect the test device and re-run `gradlew installDebug`.** The
  Galaxy A54 (`SM-A546B`) dropped off `adb` mid-build during this
  session's install attempt (2026-08-09) - not a build problem (the APK
  compiled fine), just a USB/debugging-timeout disconnect. Once
  reconnected, `cedal-server` is already redeployed and live (see below),
  so this is the only remaining step before Known calling, the
  2026-08-08 join-flow change, and Bots Round 1 can all actually be tested
  on-device.
- ~~A `cedal-server` Cloud Run redeploy~~ - **done 2026-08-09**, live at
  revision `cedal-server-00107-4rj`. Also found and fixed a pre-existing
  issue while verifying it: the service had no public IAM invoker binding
  at all (every request 403'd at the Google Frontend before reaching
  Ktor) - see `in-progress.md` for the full story and the fix.
- **A real device-testing pass against [not-tested.md](not-tested.md).**
  This is the single biggest thing blocking "done" from becoming
  "verified" — an AI can compile, deploy, and install, but can't tap
  through Compose UI. Everything in the group chat expansion needs the
  user to actually open the app and try it.
- **A second device (or emulator) to test the invite link/QR end to end**
  — scanning a QR code or opening a `cedalcode://` link needs something
  other than the one device that generated it, to confirm the deep link
  actually routes into the app and lands on the join-preview screen.
- **A decision on the "Custom" disappearing-message cleanup gap** (see
  risks.md) — leave it as unbounded row growth for now, or worth a small
  follow-up (e.g. a periodic sweep, or just capping how long a
  self-only-hidden row is kept before a real delete)?
- **Confirmation on whether voice chat / live location are still wanted**
  as separate future efforts, and if so, roughly when — both are
  standalone-subsystem-sized, so worth their own dedicated planning pass
  rather than folding into more group-chat rounds.
