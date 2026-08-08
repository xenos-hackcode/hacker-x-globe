# What we'd need

- **A `cedal-server` Cloud Run redeploy before "Known" calling
  (2026-08-08) can be tested at all.** The live server
  (`cedal-server-00105-nsk`) predates the calling backend changes (new
  `PhoneShareOverrides` table, `Users.shareNumberDefault`,
  `Groups.callsEnabled`, the `/users/{id}/number-share` routes, the
  `canCall`/`phoneNumber` fields on profile/friend/group-member DTOs) - the
  installed Android APK already has the new UI and will call these
  endpoints, so the Call tab / Share My Number / Group Call will error
  until the server is redeployed. No deploy script exists for
  `cedal-server` specifically (unlike the `cedal-services/*` subprojects,
  each of which has its own `deploy.sh`) and this wasn't done automatically
  since a Cloud Run deploy touches the live production service and needs
  DB connection secrets - held off per the user's explicit "hold off on it"
  (2026-08-08, device was about to be rebooted). Whoever redeploys needs
  the `gcloud run deploy` invocation (region `us-central1`, project
  `cedal-fd4a2`) plus the DB env vars `DatabaseFactory.kt` expects
  (`DB_INSTANCE_CONNECTION_NAME`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`).
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
