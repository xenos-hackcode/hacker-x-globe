# Left to do

## Deliberately deferred (explicit scope cuts, confirmed with the user)
- **Voice chat** — a standalone real-time subsystem (WebRTC/signaling),
  comparable in size to the entire group chat expansion on its own.
- **Live location sharing** — background location + live map, same scale
  concern as voice chat.

## Smaller open items
- **`imePadding` sweep beyond what was checked.** The Round-4 fix covered
  `GroupChatThreadScreen.kt` (the actual bug) and confirmed
  `GroupProfileScreen.kt`/`CornealChatScreen.kt`/`AlucardChatScreen.kt`/
  `PinnedMessagesScreen.kt`/`SystemFeedScreen.kt`/`CreateGroupScreen.kt`
  already had it. `AppUpdatePublishScreen.kt` and `MemberScaffold.kt` were
  noted as having no `imePadding` import at all, but weren't chased down
  since neither is a "chat" screen in the sense the user meant — worth a
  look if either ever grows a bottom-anchored text input.
- **No cleanup job for "Custom" (self-only) disappearing messages.** These
  rows are never deleted, only filtered out of the sender's own view once
  expired — see `risks.md` for why this means unbounded row growth for
  that mode specifically.
- **Rejoin-cooldown rows aren't cleaned up when a group is deleted.**
  Harmless dangling rows (a cooldown for a group that no longer exists),
  just untidy.
- **No push notifications** for any of this — join requests, tags,
  pins, etc. are all pull/poll, matching how the rest of the app already
  works, not a gap introduced by this work specifically.
- **Existing groups keep whatever `whoCanEditInfo` value they had before
  the VICE_CREATOR/CREATOR-only restriction landed** — no retroactive
  migration, so a pre-existing group could still be sitting on `ADMIN` or
  `MEMBER` until someone with permission explicitly changes the setting
  (which will then be validated against the new restriction).
- **Media & Storage / message-history cap.** `getGroupMessages` (and
  therefore the Media & Storage sub-screen) only ever looks at the 200
  most recent messages — there's no pagination, so a very old
  photo/file/poll won't show up in totals or the browsable list.
