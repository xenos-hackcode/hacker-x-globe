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
- **`AccountService.deleteAccount` (Settings > Delete Account, self-service)
  is missing several tables with a foreign key onto `Users`, found
  2026-08-09 while auditing it for an admin-requested full account wipe.**
  Its own doc comment already warned this hand-maintained list "is
  otherwise silently out of sync with the schema" - confirmed true. Fixed
  the one gap that mattered for that session's work (`Bots`, added the
  same day), but NOT fixed: `Groups` (`creatorId`), `GroupMessages`
  (`senderId`), `GroupReports` (`reporterId`), `SavedMessages` (`userId`),
  `DeveloperSubmissions`, `CodeSyncJobs`, `PendingSmsJobs`,
  `PlatformDevelopers`, `PlatformEmailSends`, `PlatformSmsJobs`. **Right
  now, any user who has created a group, sent a group message, saved a
  message, submitted a developer request, or touched the code-sync/
  SMS-relay-platform features will get a database error instead of a
  successful "Delete Account"** - a Postgres FK RESTRICT violation on
  whichever of those tables has rows first. Not caught by this session's
  wipe (that used a temporary raw-SQL `TRUNCATE ... CASCADE` route
  instead, specifically because this list was known-incomplete), so it's
  a live, real defect on the current empty-database state going forward
  as soon as anyone signs up and uses more than the most basic features.
  Needs the same table-by-table treatment `Bots` just got - go through
  every `reference(..., Users)` in `Tables.kt` (about 40 lines'-worth
  across ~15 tables at last count) and cross-check against this
  function's coverage, or consider adding `onDelete = ReferenceOption.CASCADE`
  to the relevant `reference()` declarations in `Tables.kt` instead so the
  database enforces this itself and the function stops needing manual
  upkeep entirely.
