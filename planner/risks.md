# Risks & known simplifications

Things flagged as deliberate trade-offs while building, or genuine
uncertainties given none of this has had a manual test pass yet (see
[not-tested.md](not-tested.md)).

- **A new `object` in `Tables.kt` doesn't actually exist in production
  until it's ALSO added to `DatabaseFactory.kt`'s
  `SchemaUtils.createMissingTablesAndColumns(...)` call - two separate
  places, easy to update one and forget the other.** Happened three times
  now per that function's own accumulating doc comments: `LessonCompletions`,
  `DeveloperSubmissions`, and `Bots` (2026-08-09 - added in the Round 1
  commit, missed here, silently 500'd every `/bots` call with `relation
  "bots" does not exist` until a live Clear Data failure surfaced it days
  later). Worth checking this list specifically any time a new table gets
  added, rather than assuming "compiled clean" means "actually queryable in
  production" - Exposed only validates the Kotlin side at compile time, the
  Postgres schema itself is a separate, easy-to-forget step.

- **Nothing in the big group chat feature has been manually tested since
  its last fix.** Every round shipped based on compile success + deploy
  success only. Real risk of layout/interaction bugs an AI can't catch by
  reading code — especially the composer color-dot logic, the mention
  picker's positioning when the keyboard is open, and the multi-select
  media picker.
- **"Custom" self-only disappearing messages never actually get deleted**
  — only filtered out of the sender's own message list once expired.
  Every other viewer keeps the row forever, and there's no cleanup job
  (this codebase has none — everything is lazy/opportunistic-on-read by
  convention). Over a long enough time, self-only-disappeared messages
  accumulate in the database indefinitely.
- **QR generation loops `Bitmap.setPixel` once per pixel** (480×480 =
  ~230,000 calls) via zxing's raw `BitMatrix` output. Not benchmarked —
  could be noticeably slow on a low-end device opening the Link tab.
- **The invite deep link (`cedalcode://group/{token}`) has no web
  fallback.** It only works from something that can already trigger a
  custom URI scheme (another app, a QR scanner, this app's own share
  sheet). Pasting the link into a browser or most messaging apps likely
  won't do anything, since there's no `https://` universal-link version
  registered.
- **Tag privacy is message-wide, not per-recipient.** If a message tags
  both someone with Hider on and someone with Hider off, the whole message
  is silently downgraded to public rather than partially hiding — a
  deliberate simplification (a message has one shared body, not per-viewer
  content copies), but worth remembering if it ever looks like a bug.
- **All disappearing/auto-delete purges are lazy** (opportunistic, only
  checked when someone actually fetches messages or opens the group/list).
  A group nobody opens past its auto-delete date won't actually delete on
  schedule — it'll sit there until the next read triggers the check. Same
  behavior the original view-once purge already had; not new to this
  work, but compounds now that there are more expiry mechanisms.
- **Build environment has been intermittently slow/flaky this session** —
  Gradle builds routinely took 10-30 minutes, and "Detected multiple
  Kotlin daemon sessions" warnings showed up more than once. Worth a
  `gradlew --stop` if a future build looks stuck or produces a suspicious
  instant "UP-TO-DATE" result after real source changes (happened once
  this session and turned out to be a real stale-cache hit, confirmed by
  forcing `--rerun-tasks`).
- ~~`AccountService.deleteAccount` missing tables with a foreign key onto
  `Users`~~ - **fixed 2026-08-09, same day it was found.** Confirmed via
  Godmode Clear Data actually failing on a real account (silently, on top
  of it - see the next entry) that this was live, not theoretical. Traced
  every `reference(..., Users)` in `Tables.kt` this time (not just the
  ones adjacent to what that session happened to be touching) and closed
  all of them: the whole group system (`Groups` via the existing
  `GroupChatService.leaveGroup` succession/dissolve logic, so other
  members' groups aren't destroyed just because one member - even the
  Creator - deletes their account, plus every Round-2 per-group table),
  `SavedMessages`, `CodeGithubConnections`, `PendingCodeGithubOAuth`,
  `CodeSyncFiles`, `CodeSyncJobs`, `DeveloperSubmissions`,
  `PhoneShareOverrides`. (`PendingSmsJobs`/`PlatformDevelopers`/
  `PlatformEmailSends`/`PlatformSmsJobs` turned out to never reference
  `Users` at all on closer reading - a wrong assumption in the original
  version of this entry, not an actual gap.) Still hand-maintained, so the
  underlying "silently out of sync with the schema" risk this function's
  own doc comment already warned about isn't gone forever - just closed
  for everything that exists today. `onDelete = ReferenceOption.CASCADE`
  on the `Tables.kt` side remains the real long-term fix if this doc
  comment ever gets out of sync with a new table again.
- **Godmode's Ban/Unban/Permanent Ban/Clear Data were discarding their
  `Result` entirely, found the same session as the gaps above** - a
  failed action (like Clear Data hitting one of those missing tables)
  looked identical to a successful one: no error shown anywhere, the
  target account just silently stayed in the list. Fixed by actually
  checking `.onFailure` and showing it via `CedalErrorText`. A second,
  separate bug found earlier the same session: the fingerprint prompt for
  these same actions did nothing at all when tapped - `GodmodeScreen.kt`
  wrapped `AccountVerifyOverlay` (which already draws its own full-screen
  scrim) in an extra `Dialog { }`, and that redundant window was blocking
  `BiometricPrompt`'s own window/fragment attachment from ever showing.
  Fixed by rendering the overlay directly, matching how
  `MemberSwitchAccountScreen.kt` already did it correctly. Same fix
  applied to `ArchivedChatsScreen.kt`'s identical pattern (hidden-chats
  verify) since it had the same latent bug, just not yet hit.
- **Bots' credential columns (`telegramToken`, `whatsappAccessToken`,
  `userApiKey` - the last one a user's own third-party AI API key, added
  2026-08-10 for BYOK) are stored as plain-text `varchar` in Postgres, not
  encrypted at rest.** They're never echoed back to the client (`BotResponse`
  only ever returns `has*` booleans - see `BotService.toResponse`), so
  normal API use can't leak them, but a direct database compromise would
  expose them raw. **Incident-response plan if that ever happens (2026-08-10,
  user asked for this explicitly): post to the Cedal System Feed** -
  `SystemFeedService.createPost` already lets the hardcoded admin account
  (`hackerxenos06@gmail.com`) broadcast to every user, who sees it as an
  unread badge on the "Cedal System Feed" row at the top of their chat list
  (`ChatService.listConversations`) - no new code needed, this already
  exists and covers the ask ("tell everyone to go rotate/delete their API
  keys on Telegram/WhatsApp/Anthropic's own site"). Real encryption-at-rest
  for these columns is the actual fix that would make this notice
  unnecessary in the first place - not done yet, worth doing before this
  feature has real users relying on BYOK.
- **WhatsApp cedal-hosted incoming webhooks (Round 3, 2026-08-10) aren't
  signature-verified.** Meta supports an `X-Hub-Signature-256` HMAC (via
  the developer's Meta App Secret) to prove a webhook POST genuinely came
  from Meta; `/webhooks/whatsapp`'s `POST` handler doesn't check it,
  trusting the `phone_number_id` lookup alone. Since that route only acts
  on bots explicitly in `hostingMode == "cedal"` and just calls the
  existing `BotBrainService.converse`, the practical exposure is someone
  forging a fake incoming WhatsApp message to a specific bot they'd need
  to already know the `phone_number_id` of - not nothing, but bounded.
  Worth adding before this sees real (non-admin) premium users.
- **`AccountService.deleteAccount`'s direct `Bots.deleteWhere` bypasses
  `BotService.delete`'s Telegram webhook unregistration (Round 3).** If an
  account with a `hostingMode == "cedal"` Telegram bot deletes their whole
  Cedal account, the row disappears but Telegram's `setWebhook` registration
  for that bot's token isn't torn down - an orphaned webhook pointing at a
  now-404ing route (Telegram just logs delivery failures, no other
  consequence) until/unless that same token is reused. Same root cause as
  the `AccountService`-hand-maintained-table-list risk higher up this file -
  a cleanup path that doesn't reuse the "real" deletion function.
- **Baileys WhatsApp bots (`whatsappMethod: "baileys"`, added 2026-08-10)
  are a deliberate ToS tradeoff, not a bug - documenting it here so it
  doesn't get mistaken for one later.** `@whiskeysockets/baileys` links a
  real WhatsApp number by imitating the WhatsApp Web protocol - not an
  officially supported integration, and the linked number carries a real
  (if commonly tolerated for hobby use) risk of being flagged/banned by
  WhatsApp. This was an explicit user request after hitting real friction
  with Meta's official Developer Console (particularly bad on mobile
  browsers). The tradeoff is disclosed in three places on purpose (the
  Android picker's own hint text, the generated `README.txt`, and
  Corneal's knowledge) rather than left for the user to discover after
  the fact. No code fix needed here - just don't quietly "clean this up"
  by removing the warnings in a later pass without re-confirming with the
  user first.
- **Push notifications (2026-08-13) use a small in-process OAuth token
  cache with no jitter/lock around refresh.** `PushNotificationService`
  caches the Cloud Run metadata-server access token and refreshes it once
  it's within 60s of expiry; under concurrent sends right at that boundary,
  multiple coroutines could each fire their own metadata-server fetch
  instead of sharing one - harmless (the metadata server tolerates this
  fine) but wasteful. Not worth a mutex unless this service starts sending
  at real volume.
- **A dead/uninstalled FCM token is never cleared from `Users.fcmToken`.**
  If a user uninstalls the app or a token otherwise goes stale, FCM's send
  API returns an error for that token but `PushNotificationService.send`
  only logs/swallows it (see the `runCatching` around the POST) rather than
  clearing the column - so every future notification for that user keeps
  making a doomed API call forever. Cheap fix later: on a specific
  "unregistered/invalid token" error code from FCM, null out `fcmToken`.
- **Push notifications were never installed/clicked-through on the test
  device this session** - the physical device went USB-offline mid-install
  (`adb` showed `offline`/`device not found`, unrelated to the build,
  which compiled clean) and the retry wasn't done before the session moved
  on. Server-side is deployed and live; Android is compiled but the actual
  install + a real "kill the app, send a message from another account,
  confirm a system notification appears" pass hasn't happened yet - see
  `not-tested.md`.
