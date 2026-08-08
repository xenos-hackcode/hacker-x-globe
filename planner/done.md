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

## 2026-08-04: rejoin-cooldown cleanup + message pagination

- **Rejoin-cooldown rows now cleaned up on group deletion.**
  `deleteGroupFully` now deletes `GroupRejoinCooldowns` rows for the group,
  so a dissolved/auto-deleted group doesn't leave dangling cooldown rows
  behind forever.
- **Cursor-based pagination for `getGroupMessages`.** The server's
  `getGroupMessages` now accepts an optional `beforeTimestamp` parameter
  (the `sentAt` of the oldest message the client already has). The Android
  chat thread (`GroupChatThreadScreen.kt`) auto-loads older pages when the
  user scrolls to within 5 items of the top, with a loading spinner and
  scroll-position preservation so the viewport doesn't jump. The 200-
  message default stays for the initial page; older history is fetched on
  demand. The Media & Storage sub-screen still uses the default 200-message
  fetch (noted in `left-to-do.md` as a separate, lower-priority fix).
- **`imePadding` sweep confirmed resolved.** `AppUpdatePublishScreen.kt`
  already had `imePadding` applied (fixed after the planner entry was
  written). `MemberScaffold.kt` checked and confirmed clean — its only
  text inputs are a header search bar (top-anchored) and a `Dialog`-wrapped
  custom-days prompt (separate window), neither of which needs it.

## 2026-08-07: Media & Storage full-history pagination

- **`GroupMediaScreen.kt` no longer caps at 200 messages.** `refresh()`
  now walks every page via `getGroupMessages`' `beforeTimestamp` cursor
  (the pagination primitive added 2026-08-04 for the chat thread) and
  accumulates full history before computing totals or rendering the
  Photos/Videos/Stickers/Files/Polls lists, instead of only ever seeing
  the most recent 200 messages. Added a "Loading..." state since fetching
  can now take multiple round-trips for a group with deep history. Closes
  the last open half of the "Media & Storage / message-history cap" item
  in `left-to-do.md` (the chat thread itself was already unbounded since
  2026-08-04).

## 2026-08-08: "Known" calling (round 1 of 2 - native dialer, DM/group)

User asked for a two-mode calling system: **"Known"** (real phone number,
native carrier call, fast/reliable, costs the caller's own cellular
minutes - like a normal phone call) and **"Secretive"** (in-app data call,
hides your number - see `left-to-do.md`, its own future round). Round one
shipped Known only, confirmed with the user up front since it needed no new
real-time infra, unlike Secretive's WebRTC/TURN decision.

- **New opt-in phone-number-sharing permission**, mirroring the existing
  Popularity global-default + per-friend-override shape
  (`PopularitySettings`/`ChatPopularityOverrides`): `Users.shareNumberDefault`
  (off by default - Settings > Privacy > "Share My Number") plus a new
  `PhoneShareOverrides` table for per-friend exceptions (a specific friend
  can always/never get your number regardless of the global default,
  settable via "Share My Number With ___" chips on their own friend-profile
  screen). Resolved server-side by `CallService.canCall` - a friend's real
  `Users.phoneNumber` is only ever handed back in `UserProfile`/
  `FriendSummary`/`GroupMemberDto` when this resolves true for the
  requesting viewer; `Users.phoneNumber`'s existing owner-only access rule
  (`SecurityService`'s `/phone` routes) is untouched.
- **Call tab replaces the old empty Base tab** (`MemberTab.BASE` renamed to
  `MemberTab.CALL`, visible again in the bottom bar) - `CallListScreen.kt`
  lists DM contacts only (reuses the existing `listFriends`/`FriendSummary`
  population, same list the Bank "Send" picker already uses) with a local
  search box, per the explicit "only people in dm are the people u can
  call" ask. Each row shows a Call button when that friend has shared their
  number, or a "hasn't shared their number" note when they haven't.
- **Call / Video Call buttons on 1:1 DM friend profiles**
  (`MemberFriendProfileScreen.kt`'s Actions section). Call launches the
  device's own dialer (`ACTION_DIAL`, not `ACTION_CALL` - no `CALL_PHONE`
  runtime permission needed, user still taps "send" themselves) via the new
  `launchDialer` helper in `CallUtils.kt`. Video Call is a visible
  placeholder for now ("coming in a future update") since that's Secretive,
  not built yet.
- **Group Profile "Group Call"** (`GroupProfileScreen.kt`, placed in the
  header before the description field, per the ask) - Creator-only lock
  (narrower than the usual Vice-Creator-can-too pattern, via a new
  `Groups.callsEnabled` column and a Creator-only gate in
  `updateGroupSettings`). Tapping it opens a member picker
  (`GroupCallPickerOverlay`) rather than starting a true conference call -
  a real multi-party call isn't achievable through a native dialer intent,
  so this places a normal 1:1 Known call to whichever member is picked and
  has shared their number with the viewer.
- Compiled clean on both `cedal-server` (`compileKotlin`) and
  `cedal-android` (`compileDebugKotlin`).

