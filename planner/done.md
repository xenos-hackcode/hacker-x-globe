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
- Public/private groups, group search (public only).
- **(2026-08-08 revision)** Public groups: search or link joins instantly,
  no approval — the old join-request/approve/reject flow no longer applies
  to them. Private groups: never searchable, but now get an invite link too
  (admin/vice-creator/creator-tier only, both to view and to reset)
  — opening it still goes through the join-request → admin-tier approval
  flow (formerly public-only).
- Group invite **link + QR code** — Copy, Share, Reset Link, deep link
  (`cedalcode://group/{token}`) landing on a join-preview screen; button
  reads "JOIN" for a public group's link, "REQUEST TO JOIN" for a private
  one's.
- 24-hour rejoin cooldown after being kicked or leaving voluntarily.
- Block group (nobody can re-add you), Report group.

## Per-chat "Add Shortcut" deep link (2026-08-08)
- Header ⋮ menu → "Add Shortcut" (1-on-1 chat and friend profile) pins a
  home-screen shortcut that now actually opens straight into that chat
  thread, instead of just the app's normal entry point — see
  `OpenChatDeepLinkState`/`MainActivity.handleChatShortcutDeepLink`.

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

## 2026-08-09: Bots/"Leo" bot-builder platform, Round 1

Planned in a dedicated planning pass (plan file
`C:\Users\WINDOWS11\.claude\plans\hashed-finding-trinket.md`), which
resolved three open product decisions: Leo generates literal source code
the user self-hosts (not a persona cedal-server hosts); Telegram +
WhatsApp both from the start; monetization deferred to an
admin-toggleable `isPremium` flag (no payment processor exists anywhere
in this app, and it isn't Play-Store-distributed so Play Billing isn't
even an option). Round 1 itself is just real, owner-scoped CRUD for the
character-sheet form `MemberBotsScreen.kt` already had as a UI-only stub
(`handleSave()` never persisted) - Leo, the `/bots/{id}/converse` brain
endpoint, and code generation are later rounds, see `left-to-do.md`.

- New `Bots` table (`db/Tables.kt`), `BotService.kt`, `BotRoutes.kt`
  (`/bots` CRUD), DTOs (`BotCreate`/`BotUpdate`/`BotResponse`) mirrored on
  both server and Android - credentials (`secretToken`/`telegramToken`/
  `whatsappAccessToken`/`userApiKey`) never come back in a list/get
  response, only `has*` booleans.
- `MemberBotsScreen.kt` rebuilt from its single-form stub into
  `MemberBotsListBody` (list of your bots) + `MemberBotEditBody`
  (the same character sheet, now with a Telegram/WhatsApp/Both platform
  picker, credential fields, icon upload via a new `bot_icon` upload kind,
  and a real save/delete). Nav: `member_bots` → `member_bot_edit/{botId}`
  (`new` for create).
- Compiled clean on both `cedal-server` and `cedal-android` (a stale
  `MemberBotsBody` import in `NavGraph.kt`, left over from the rename,
  broke the first Android compile attempt - caught by reading the actual
  build log rather than trusting a task-completion summary, fixed in a
  follow-up commit).
- Deployed (`cedal-server-00107-4rj`) and installed on the test device
  2026-08-09, same session as the fix above for the 2026-08-08 "Known"
  calling/join-flow work's own pending deploy.
- **Correction, same day:** despite the above, the `Bots` table itself
  never actually existed in production Postgres - defined in `Tables.kt`
  but never added to `DatabaseFactory.kt`'s creation list (same class of
  miss that file's own comments already document for `LessonCompletions`/
  `DeveloperSubmissions`), so every `/bots` call 500'd the entire time.
  Only surfaced when `AccountService.deleteAccount`'s new Bots cleanup
  line hit it during a live Clear Data attempt. Fixed and redeployed
  (`cedal-server-00112-28x`) - see `risks.md` for the full recurring
  pattern this is worth watching for.

## 2026-08-10: Bots/"Leo" bot-builder platform, Round 2 (the brain endpoint)

- New `BotService.verifySecretToken`, new `services/BotBrainService.kt` -
  builds a system prompt from the character-sheet fields, calls
  `AiProviderService.ask()` the same way `CornealChatService.kt` does,
  enforces the 1000-token free cap (chars/4 estimate) unless `isPremium` or
  a `userApiKey` is set, persists turns in a new `BotConversationTurns`
  table (added directly to `DatabaseFactory.kt`'s creation list this time,
  learning from Round 1's miss above).
- `ask()` is suspend and can't be called inside Exposed's synchronous
  `transaction{}` - `converse()` is split into a pre-transaction block (load
  bot, check quota, append the user turn), the bare suspend AI call, then a
  post-transaction block (append the assistant turn, update token usage).
- New `POST /bots/{id}/converse` (`BotRoutes.kt`) - authenticated by the
  bot's own `secretToken` via `Authorization: Bearer`, outside the JWT
  `authenticate` block, since this is meant to be called by Round 3's
  eventual self-hosted generated code, not the app itself.
- Also added a JWT-gated, owner-only `GET`/`POST /bots/{id}/test-chat` path
  and an in-app `MemberBotTestChatBody` screen (new "TEST CHAT" button on
  the bot edit screen) - not in the original Round 2 scope, added because
  otherwise there was no way to actually try a bot's persona out before
  Round 3's code generation exists.
- `AccountService.deleteAccount` now clears `BotConversationTurns` before
  `Bots` (FK ordering).
- Compiled clean on both `cedal-server` and `cedal-android`. Deployed
  (`cedal-server-00116-kvh`) and installed on the test device same day.
  Sanity-checked `/converse` with a deliberately wrong `secretToken` →
  correct `401`, not a crash.

## 2026-08-10: Bots/"Leo" - In-App bot type + real BYOK

User feedback after trying Round 2: worried the brain was running on their
own personal API, confused that the Telegram token field always looked
empty on reopen (it wasn't actually lost - the app never re-displays saved
secrets, and the server only overwrites a credential when a new one is
actually sent), and asked for bots to not require any external setup at
all. Resolved via two clarifying
questions: **keep Telegram as its own real thing** (unchanged), **add a
separate "In-App" platform option** so a bot can just live inside Cedal
with zero setup, and **wire up BYOK for real** (the `userApiKey` column
already existed but only exempted a bot from the free-tier cap - it never
actually routed the AI call through the user's own key).

- New `"inapp"` `botType` value (`MemberBotsScreen.kt`'s platform picker,
  now `In-App / Telegram / WhatsApp / Both`, wrapped in `horizontalScroll`
  since 4 chips no longer fit unscrolled on narrow screens) - default for
  new bots, needs no credentials, "TEST CHAT" button relabels to "CHAT" for
  this type since the in-app chat *is* its real home, not a preview.
- `userApiKey` added to `BotCreate`/`BotUpdate` on both server and Android
  (was previously write-only via direct DB access, no API path to set it)
  and a real form field ("leave blank to keep it" pattern, matching the
  Telegram/WhatsApp credential fields).
- New `AiProviderService.askWithKey(prompt, maxTokens, apiKey)` - same
  Anthropic request shape as `tryAnthropic`, but takes the key directly and
  throws instead of silently falling back to Cedal's shared keys on
  failure (falling back would defeat BYOK's whole point - the user's own
  quota/cost). `BotBrainService.converse` now calls this instead of
  `ask()` whenever the bot has a `userApiKey` set.
- Documented the raw-storage risk this creates (`userApiKey` is
  plain-text `varchar`, never echoed back to clients but not encrypted at
  rest either) and the confirmed incident-response plan if `cedal-server`'s
  DB is ever compromised - post to the Cedal System Feed, which already
  lets the admin broadcast to every user with an unread badge, no new code
  needed. See `risks.md`.
- Compiled clean on both `cedal-server` and `cedal-android`. Deployed
  (`cedal-server-00117-8pq`) and installed on the test device same day.

## 2026-08-10: Bots/"Leo" Round 3 - real Telegram/WhatsApp connectivity

Full planning-pass rationale (webhook-vs-polling architecture decision) in
the plan file referenced by `left-to-do.md`. User asked for both a free
self-hosted path (the original Round 3 plan, unchanged) and a paid
cedal-hosted path where cedal-server runs the bot directly - "do both, user
who pay money would get cedal server hosting."

- New `Bots.hostingMode` column (`"self"` default | `"cedal"`, only
  settable once `isPremium` is true). `BotService.create`/`update`/`delete`
  are now `suspend` (may register/unregister a Telegram webhook - a
  network call, split pre/post-transaction like `BotBrainService.converse`).
- **Cedal-hosted Telegram**: new `TelegramBotService.kt` wraps
  `setWebhook`/`deleteWebhook`/`sendMessage`. New unauthenticated
  `POST /bots/{id}/telegram-webhook`, secured by the
  `X-Telegram-Bot-Api-Secret-Token` header Telegram echoes back (registered
  as the bot's own `secretToken` at `setWebhook` time - no new secret).
  Chosen over `getUpdates` long-polling specifically because Cloud Run has
  zero precedent anywhere in this server for a process-lifetime background
  task and scales to zero/multi-instance in ways a persistent poller
  doesn't survive - confirmed via exploration before writing any code.
- **Cedal-hosted WhatsApp**: new `WhatsAppBotService.kt` (send only - Meta
  has no per-bot webhook registration API). New top-level
  `GET`/`POST /webhooks/whatsapp` - one shared route for every cedal-hosted
  WhatsApp bot, disambiguated by the `phone_number_id` embedded in each
  incoming payload (`BotService.findByWhatsappPhoneNumberId`). Verify
  handshake uses a single server-wide `WHATSAPP_WEBHOOK_VERIFY_TOKEN` env
  var (set on Cloud Run, matches a hardcoded Android-side constant) since
  Meta's handshake happens before it knows which bot it's for.
- **Self-hosted download** (the original Round 3 plan, now actually built):
  new `BotTemplateService.kt` generates a `telegram_bot.py`
  (`getUpdates`-polling - fine on the user's own always-on machine, unlike
  Cloud Run) and/or `whatsapp_bot.py` (Flask webhook receiver, documented
  as needing the user's own public URL) with real credentials embedded,
  zipped via `java.util.zip` (no new dependency), served by owner-only
  `GET /bots/{id}/download`. New `BotService.getCredentialsForDownload` -
  the one deliberate exception to "credentials never leave the DB raw,"
  since this is the owner explicitly triggering their own bot's code.
- **Admin premium toggle**: `POST /bots/{id}/set-premium`, gated by the
  same hardcoded admin-email check `SystemFeedService.isAdmin` already
  uses, not owner-scoped (admin can flip any bot). Surfaced in
  `MemberBotsScreen.kt` as an "ADMIN: MARK/REMOVE PREMIUM" button, visible
  only to the admin account (same client-side check `SystemFeedScreen.kt`
  already uses).
- **Android**: new "Hosting" section on the bot edit screen (Self-hosted
  free / Cedal-hosted premium picker, gated with a clear message rather
  than hidden when not premium yet), a "DOWNLOAD BOT CODE" button
  (authenticated Retrofit `@Streaming` download → cache file → share via
  the same `FileProvider` pattern `MemberCodeScreen.kt` already uses for
  sharing a code file - required adding a `bot_downloads` entry to
  `file_paths.xml`, a real gap that would have thrown at runtime otherwise),
  and a WhatsApp webhook URL/verify-token display with copy buttons for
  cedal-hosted WhatsApp bots.
- Verified the WhatsApp verify handshake live: correct token → `200` +
  challenge echoed back; wrong token → `403`.
- Known simplification, documented in `risks.md`: WhatsApp incoming webhook
  payloads aren't signature-verified (`X-Hub-Signature-256`), and
  `AccountService.deleteAccount`'s direct `Bots.deleteWhere` bypasses
  Telegram webhook unregistration (an orphaned webhook, not a security
  hole - it just keeps pointing at a deleted bot until overwritten).
- Compiled clean on both `cedal-server` and `cedal-android`. Deployed
  (`cedal-server-00118-8p8`, then `-00119-dcj` for the env var) and
  installed on the test device same day.

## 2026-08-10: Bots/"Leo" Round 3 follow-up fixes (same-day live testing)

Real bugs found within minutes of the user actually trying to connect a
Telegram bot end to end - none caught by compiling clean, all found by
using it:
- **`TelegramBotService.registerWebhook` swallowed every failure** -
  Telegram's Bot API always returns HTTP 200 even when it rejects a
  request (bad token, bad URL - the real signal is the `ok` field in the
  body), so a `runCatching` with no result check meant "Save" could report
  success while Telegram never actually registered anything. Now parses
  the response and throws `AuthException` with Telegram's own description
  on failure - surfaces through the same `StatusPages`/`apiCall` path every
  other real error already uses.
- **Delete Bot failed with a live foreign key violation** -
  `BotConversationTurns.botId` references `Bots` with no cascade; any bot
  that had ever been messaged (even just via Test Chat) couldn't be
  deleted. `BotService.delete` now clears `BotConversationTurns` first,
  same ordering `AccountService.deleteAccount` already used for this pair
  (a gap in `BotService.delete` specifically, not caught when that fix
  landed there).
- **The system back gesture bypassed save-then-leave** - only the visible
  in-app back arrow was wired to `::save`; the phone's own back
  button/gesture just popped the screen, silently discarding a freshly
  typed credential. Added `BackHandler(onBack = ::save)`, matching
  `GuiSessionScreen.kt`'s existing pattern - a gap that likely existed
  since Round 1, only surfaced now under real use.
- **Saved-credential fields looked empty even when a value was on file** -
  the "on file" hint was a small caption easily missed above a
  genuinely-empty-looking text field. Placeholder text itself now says
  "✓ saved — type to replace" when a credential exists.
- **Replies echoed the bot's own name and literal markdown asterisks** -
  `buildTranscript`'s "Name: message" formatting (shown to the model as
  context) was being imitated in the model's actual reply, and none of
  the three delivery channels (Telegram, WhatsApp, in-app) render
  markdown, so `**Name**: reply` showed up as literal text. System prompt
  now explicitly tells the model not to prefix its name or use markdown.
- **Bot name never synced to Telegram** - new
  `TelegramBotService.setMyName`, called on create/update whenever a
  Telegram token is present, independent of hosting mode. **Profile
  picture has no equivalent** - Telegram's Bot API has no method for a
  bot's own avatar; it's `@BotFather`'s `/setuserpic` command only, not
  automatable from Cedal.
- Deployed (`cedal-server-00120-jcg` → `-00121-b9k` → `-00122-mwf`) and
  installed on the test device across this fix cycle.

