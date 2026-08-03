# Group chat: settings, permissions & moderation expansion (2026-08-03)

**Status: DONE (5 rounds), unverified on-device.** Server and Android both
compile clean each round; every round was deployed to Cloud Run and
installed as a debug APK. No manual run-through has confirmed any of it
actually works as intended yet - see `../not-tested.md` for the full list
and `../tested.md` for the (short) list of what the user has actually
clicked through, which is mostly "found it broken, got a fix, never
re-confirmed the fix."

**Full design doc:** `C:\Users\WINDOWS11\.claude\plans\mellow-doodling-glacier.md`
(all 5 rounds' design decisions, server/Android change lists, and original
verification checklists live there in full detail - this entry is a
condensed history summary, not a replacement for it).

**What, in one line per round:**

- **Round 1** - the original ask: permissions/ranks, media visibility,
  notifications (mute), manage storage, kept/disappearing messages, chat
  lock, group permission model (everyone/admin-tier sees what), clear chat,
  secured mode, report/block group, group-wide pin, chat saver (Saved
  Messages). Voice chat and live location deliberately cut (own-subsystem
  sized); QR/invite links deferred except minimal join-request plumbing.
- **Round 2** (folded in mid-Round-1-build) - per-setting locks, Group
  Rules field, private tags (single-tag version), group auto-delete,
  Creator-leave rework, System-owner fallback account.
- **Round 3** - DM toggles (personal `dmClosed`, group-wide
  `dmClosedByCreator`, per-member override), "No Tag" personal toggle.
- **Round 4** (first post-ship feedback batch) - leave-flow
  options-first fix, real auto-delete duration picker, Group Profile
  Overview/Security tab split + ⋮ menu, `imePadding` bug fix, real
  media byte-size tracking + Media & Storage sub-screen, multi-tag support,
  "Hider" toggle + tag-privacy downgrade logic, `@`/`#` mention picker with
  Unicode-normalized Default/Custom name search.
- **Round 5** (second post-ship feedback batch) - leave-flow search bar,
  group-chat sender-name bug fix (was showing raw IDs), tag color scheme
  simplified (`#`=private/`@`=public, dropped the ask-once-per-message
  hide prompt from Round 4), per-message disappearing (For
  Everyone/Custom), 24h rejoin cooldown, Link tab (invite token + QR +
  share/copy/reset + deep link), `whoCanEditInfo` restricted to
  Vice-Creator/Creator, and a follow-up fix for the search tabs row
  overflowing (shortened to "GR", made horizontally scrollable).

**Pattern across all 5 rounds:** each one was built via many small,
targeted `Edit` calls rather than large file rewrites (same lesson as the
2026-07-30 entry's "content filtering" note), verified with
`compileKotlin`/`compileDebugKotlin` before every deploy, and a couple of
real compile errors were caught this way each round (a variable-shadowing
bug in `updateGroupSettings`'s `unpinMessage`, a missing import after
adding `GroupLinkPreviewDto`, a missing `@OptIn(ExperimentalFoundationApi)`
on new `combinedClickable` usages, missing `getValue`/`setValue` imports
for a `by mutableStateOf` delegate).

**Deploy trail:** `cedal-server-00101`→`00105` (Cloud Run revisions across
the 5 rounds, `cedal-fd4a2`/`us-central1`); Android debug APK rebuilt and
reinstalled after every round plus the final GR-tab-label fix.

**Full breakdown of what's done/tested/left/risky/needed:** see the
sibling files one level up - `../done.md`, `../tested.md`,
`../not-tested.md`, `../left-to-do.md`, `../risks.md`, `../needs.md`.
