# Done — compiled, deployed, installed

Everything below passed `./gradlew compileKotlin` (server) and
`./gradlew compileDebugKotlin` (Android), and is live on `cedal-server`
(Cloud Run) and installed on the test device. "Done" here means **shipped**,
not necessarily **verified correct** — see [tested.md](tested.md) vs
[not-tested.md](not-tested.md) for that distinction.

## Group chat: settings, permissions & moderation (the big one — Rounds 1-5)

Full design rationale: `history/2026-08-03-group-chat-expansion.md` and the
original Claude plan-mode doc at
`C:\Users\WINDOWS11\.claude\plans\mellow-doodling-glacier.md`.

**Permissions & roles**
- 4-tier rank threshold (MEMBER < ADMIN < VICE_CREATOR < CREATOR) on 5
  settings: who can send messages, edit group info, add members, see group
  stats, send media.
- Per-setting **locks** — Creator/Vice-Creator can freeze one setting so
  plain Admins can't change it (lock icon renders red when locked, for
  every viewer).
- "Who can edit group info" restricted to Vice-Creator/Creator only
  (narrower than the other 4 settings).
- Share-history-with-new-members toggle.

**Discovery & membership**
- Public/private groups, group search, join-request + admin approval flow.
- Group invite **link + QR code** (public groups only) — Copy, Share,
  Reset Link, deep link (`cedalcode://group/{token}`) landing on a
  join-preview screen.
- 24-hour rejoin cooldown after being kicked or leaving voluntarily.
- Block group (nobody can re-add you), Report group.

**Messaging features**
- Group-wide pinned message ("Pin for Everyone"), rank-checked unpin.
- Clear Chat (Creator-only, messages only — group/members survive).
- Group-wide disappearing messages (admin-set duration) + "Keep" exemption.
- **Per-message disappearing** (header menu, next to View Once): "For
  Everyone" (real delete after a duration) or "Custom" (hidden only from
  the sender's own view).
- `#`/`@` tag system: `#` always private (visible only to sender + tagged
  members), `@` always public — composer picker with live-filtered
  Default/Custom name search (Unicode-normalized so a stylized name still
  matches the plain letter), multi-tag support, composer status dot
  (red = View Once armed, yellow = private tag queued, blue = normal).
  Sent messages render yellow text if privately tagged, green if also
  View Once.
- Personal privacy toggles: "No Tag" (block being tagged at all), "Hider"
  (allow/deny being tagged *privately* — off forces your tags public),
  "Close My DMs", per-group DM override, group-wide DM-closed (Creator).
- Secured Mode (disables Save-to-Saved-Messages, forces FLAG_SECURE).
- Saved Messages (personal self-chat, reachable from Chats overflow menu).
- Chat Lock (per-group, per-device biometric/passcode gate, local-only).
- Group Rules (one-time sheet on join, always readable from Group Profile).

**Media & storage**
- Real byte-size tracking on upload (client sends actual size).
- Media & Storage sub-screen: Photos/Videos/Stickers/Files/Polls tabs, real
  KB/MB totals, Download All / Delete All, long-press multi-select
  (individual or by-month), reachable from Group Profile.

**Group Profile structure**
- Overview / Security / Link tab switcher (Link only for public groups).
- ⋮ menu holds Clear Chat, Clear Media, Report, Block (moved out of the
  main scroll).
- Auto-delete group (Creator-only, real duration picker, 1 day–1 year).

**Creator leave flow**
- Options-first (Pick Random / Choose a Specific Person / Dissolve Group) —
  names only shown after "Choose a Specific Person," with a search bar.
- Succession order: Vice-Creator auto-succeeds; else Admin (explicit pick
  or Random); else plain Member (same); else Creator alone → Dissolve or
  hand off to the built-in **System-owner** account.
- Single-Vice-Creator invariant confirmed already enforced (no change
  needed — `setRole` auto-demotes the previous one).

**Bug fixes found via user testing**
- Group chat bubbles were showing raw truncated user IDs instead of
  resolved names — fixed by threading `nameFor` into `GroupMessageBubble`.
- Keyboard covering the chat input in `GroupChatThreadScreen.kt` — dead
  `imePadding` import that was never applied to the outer `Column`.
- Search tabs row (Search/Requests/QR/Groups) overflowing — shortened
  "Groups" to "GR" and made the row horizontally scrollable.

## Everything else this session

- Cloud infrastructure investigation (`cedal-db` Cloud SQL confirmation),
  billing kill-switch explanation (see memory: `cedal_billing_killswitch`).
- SMS relay multi-developer platform (see memory:
  `cedal_sms_relay_multidev_platform`).
- Code area ↔ GitHub two-way sync — full detail in
  `history/2026-07-31-github-sync.md`.
- 1:1 chat menu items duplicated into the friend profile's Actions section;
  Popularity now displays in-profile and de-duplicated from the chat menu.
- Group chat menu restructuring (moved Rename/Add Members/Members/Leave
  into Group Profile, kept only Group Info + View Once in the thread menu).
- Admin "App Updates" screen (replaces hand-rolled `curl` calls to publish
  a new APK version) with a permanent, reusable Firebase Storage download
  URL (see memory: `cedal_stable_apk_url`).
- Original group roles/permissions + view-once port — full detail in
  `history/2026-07-30-group-roles-permissions.md`.
