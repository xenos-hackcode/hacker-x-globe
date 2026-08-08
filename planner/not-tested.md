# Not tested — shipped, never clicked through

Compiled, deployed, installed — zero on-device confirmation. This is most
of [done.md](done.md). Grouped by area so a testing pass can go top to
bottom.

## "Known" calling (2026-08-08)
- Settings > Privacy > "Share My Number" toggle actually gating whether a
  DM contact sees your real number (on vs. off, from a second account).
- Per-friend "Share My Number With ___" chips (Default/Always/Never) on a
  friend's profile actually overriding the global default in both
  directions - Always sharing even with the global default off, Never
  withholding even with it on.
- Call tab: only DM contacts appear (never a random searchable user), the
  search box actually filters, and the Call button only appears/works for
  contacts who've shared their number.
- DM friend profile's Call button launching the real device dialer
  pre-filled with the right number; Video Call's "coming in a future
  update" placeholder notice.
- Group Profile's "Group Call" row: visible before the description,
  Creator-only lock icon actually blocking a non-Creator from toggling
  `callsEnabled`, and non-Creator members seeing it correctly
  enabled/disabled once a Creator locks it.
- Group Call's member picker - only callable members are tappable, the
  rest show "Hasn't shared their number", and picking someone actually
  opens the dialer with their number.

## Permissions & roles
- Rank-threshold enforcement on all 5 settings (a plain Member/Admin
  actually getting rejected server-side when a setting is raised above
  their rank).
- Setting locks — locking a setting as Creator/Vice-Creator, confirming a
  plain Admin actually can't change it, confirming Vice-Creator still can
  and can unlock it.
- "Who can edit group info" restricted picker (only 2 options instead of
  4) — visual + actual enforcement.

## Discovery & membership (2026-08-08 revision)
- Public group search from a second account, and tapping JOIN actually adds
  you instantly (no approval step) and opens the group.
- Public group's link (Group Profile > LINK tab, visible to every member)
  opens to an instant "JOIN" button, same as search.
- Private group's LINK tab only appearing for admin/vice-creator/creator,
  not plain members.
- Private group's link opening to "REQUEST TO JOIN", and the request
  actually landing in the group's Join Requests list for an admin-tier
  member to approve/reject (and the joiner ending up a member / staying out
  correctly either way).
- Private group staying unsearchable in the GR tab regardless of how many
  people have its link.

## Per-chat "Add Shortcut" (2026-08-08)
- Pinning a shortcut from a 1-on-1 chat and from a friend profile, then
  tapping the pinned icon and confirming it lands directly in that chat
  thread (not just the app's normal entry screen).
- Invite link + QR: generating one, scanning/opening it, landing on the
  join-preview screen, Copy, Share, Reset Link (and confirming the OLD
  link/QR stops working after reset).
- 24-hour rejoin cooldown — kicked or self-left, confirm re-add/re-approve
  is rejected for 24h and allowed after.
- Block group — confirm re-add is rejected, confirm the join-request path
  also respects it.
- Report group.

## Messaging features
- **Message pagination (2026-08-04)** — scroll up in a group chat with
  more than 200 messages and confirm older messages auto-load with a
  spinner, that scroll position is preserved (doesn't jump to top), and
  that the poll loop doesn't duplicate or lose messages when it refreshes
  while paginated history is loaded.
- Group-wide pin/unpin, including the rank-check (a lower-rank Admin can't
  unpin a Creator's pin).
- Clear Chat (Creator-only — confirm Vice-Creator is rejected).
- Group-wide disappearing messages + "Keep" exemption surviving the purge.
- Per-message disappearing — both "For Everyone" (real delete for all) and
  "Custom" (hidden only from sender) modes.
- Tag system end to end: `#` private tag hides content from non-tagged
  viewers with the yellow/green color rule, `@` public tag, multi-tag in
  one message, the mention picker's live filtering, the Default/Custom
  name-search split (especially a genuinely non-alphabetic/emoji-only
  display name landing in Custom correctly).
- "No Tag" / "Hider" / DM toggles (personal + per-group + group-wide) —
  every precedence combination (e.g., Hider off forcing a tag public even
  when the composer chose private).
- Secured Mode — Save action disappearing, FLAG_SECURE active with no
  View Once content present.
- Saved Messages — save from a group message, view/delete in the
  self-chat.
- Chat Lock — biometric/passcode gate on re-opening a locked group's
  thread, confirming other groups are unaffected.
- Group Rules — one-time sheet on join, not reappearing after dismissal,
  still readable from Group Profile.

## Media & storage
- Byte-size totals actually matching real file sizes.
- Media & Storage sub-screen: all 5 tabs, Download All / Delete All,
  long-press multi-select (both individual and by-month), and that
  deleting actually clears the message + frees up the count.
- **Full-history pagination (2026-08-07)** — a group with more than 200
  messages actually shows old media/files/polls beyond that cap in the
  totals and lists, and the "Loading..." state appears/clears sensibly
  instead of flashing "Nothing here yet." first.

## Group Profile structure
- Overview/Security/Link tab switcher, including Link only appearing for
  public groups and never for private ones.
- ⋮ menu (Clear Chat/Clear Media/Report/Block).
- Auto-delete duration picker (setting it, seeing the group actually
  delete itself at the scheduled time, the 1-day/1-year clamp rejecting
  out-of-range values).

## Creator leave flow
- Vice-Creator-exists path (auto-succeed, no picker).
- Admin-exists path (explicit pick or Random required, no silent choice).
- Alone path (Dissolve vs. System-owner, confirming the system account
  becomes `creatorId` and never shows up in friend search or as a
  message/friend-request target).
- Dissolve path (whole group actually gone, all members removed).

## Small follow-ups (2026-08-04)
- `AppUpdatePublishScreen.kt` `imePadding` fix — confirm the keyboard
  pushes the form up instead of covering the changelog field / publish
  button when editing on an edge-to-edge screen.
- Rejoin-cooldown cleanup on group deletion — confirm that dissolving or
  auto-deleting a group leaves no `group_rejoin_cooldowns` rows behind
  (verifiable server-side, not really a UI test).

## Everything from earlier in the session
- GitHub two-way sync — the OAuth deep-link return trip specifically has
  "zero precedent elsewhere in this app" per its own history entry and was
  flagged as the riskiest untested piece even before this session's later
  work. See `history/2026-07-31-github-sync.md`'s Verification section for
  the full 7-step manual test plan, still outstanding.
- Original group roles/permissions + view-once (kick/promote across all 4
  roles, `whoCanSendMessages` flip, view-once send/reveal/purge,
  creator-leave-transfers-ownership) — see
  `history/2026-07-30-group-roles-permissions.md`'s Verification section,
  also still outstanding as of that entry (may be superseded by the
  Round 1-5 work above, but worth a fresh pass either way since the leave
  flow in particular has changed shape twice since).
