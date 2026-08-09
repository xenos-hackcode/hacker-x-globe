# Risks & known simplifications

Things flagged as deliberate trade-offs while building, or genuine
uncertainties given none of this has had a manual test pass yet (see
[not-tested.md](not-tested.md)).

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
