# Left to do

## Deliberately deferred (explicit scope cuts, confirmed with the user)

- **"Secretive" calling (2026-08-08) - in-app data/video calling that hides
  your real number, WebRTC-based - NOT STARTED.** This is the other half of
  the "Known"/"Secretive" two-mode calling system the user described (see
  `done.md`'s 2026-08-08 entry for "Known", which IS built). Round one
  deliberately shipped "Known" only (native dialer, real phone number,
  fast/reliable, uses the caller's own cellular minutes) because it needed
  no new infrastructure decisions. "Secretive" is the opposite trade-off -
  hides your number, needs a data connection, and needs a real architecture
  choice before any code: WebRTC signaling (who exchanges offer/answer/ICE -
  likely a new websocket layer, this app's existing REST/poll pattern isn't
  built for that) plus a NAT-traversal choice between STUN-only (free, but
  calls between two people on restrictive networks can simply fail to
  connect - no fallback) and STUN+TURN (reliable everywhere, but relays
  media through a paid server - real bandwidth cost risk against the
  £10/mo `cedal-fd4a2` billing cap, see memory `cedal_billing_killswitch`).
  Comparable in size to the original group chat expansion - worth its own
  planning pass, not something to bolt onto an unrelated task.
- **Live location sharing** — background location + live map, same scale
  concern as the original voice-chat cut above.
- **"Bots"/"Leo" AI bot-builder platform (2026-08-03 ask) - Round 1
  (character-sheet CRUD) shipped 2026-08-09; Round 2 (the brain endpoint),
  an In-App bot type + real BYOK, and Round 3 (real Telegram/WhatsApp
  connectivity, both self-hosted and cedal-hosted) all shipped 2026-08-10,
  see `done.md`'s entries.** Original planning-pass file at
  `C:\Users\WINDOWS11\.claude\plans\hashed-finding-trinket.md` (now holds
  the Round 3 plan specifically - overwritten same-file per this repo's
  planning convention; Round 1's original content is preserved in this
  file's and `done.md`'s history).
  **Round 2 detail:** `BotBrainService.kt` builds the system prompt from
  the character sheet, calls `AiProviderService.ask()` outside any Exposed
  `transaction{}` (it's suspend), enforces the 1000-token free cap via a
  chars/4 estimate, and persists turns in `BotConversationTurns`. Reachable
  two ways: `POST /bots/{id}/converse` (secret-token auth, for self-hosted/
  cedal-hosted code) and a JWT-gated owner-only `/bots/{id}/test-chat` +
  in-app "TEST CHAT" screen.
  **Round 3 detail:** cedal-hosted Telegram uses `setWebhook` (not
  `getUpdates` polling - Cloud Run has no precedent for a process-lifetime
  background task); cedal-hosted WhatsApp uses one shared
  `/webhooks/whatsapp` route disambiguated by `phone_number_id` (Meta has
  one webhook URL per app, not per bot); self-hosted download bundles
  Python templates via `BotTemplateService.kt`; `isPremium` is settable via
  a new admin-only `/bots/{id}/set-premium` (still no real payment
  processor - same deferred admin-toggle shape as before).
  **Remaining roadmap (not started):** "polish with Leo" AI-assist button
  on the character sheet (an AI rewrite of persona fields before
  self-hosted download, distinct from Leo's original code-gen job which is
  now done); remaining-quota UI on the Bots list/detail screens; real
  payment processing (Stripe or similar) to replace the admin-toggle
  `isPremium` flag.

## Smaller open items
- **`imePadding` sweep — `AppUpdatePublishScreen.kt` fixed** (it has a
  scrollable form with text fields and a publish button the keyboard can
  cover). `MemberScaffold.kt` checked and confirmed clean — its only text
  inputs are a header search bar (top-anchored) and a `Dialog`-wrapped
  custom-days prompt (separate window), neither of which needs `imePadding`.
- **No cleanup job for "Custom" (self-only) disappearing messages.** These
  rows are never deleted, only filtered out of the sender's own view once
  expired — see `risks.md` for why this means unbounded row growth for
  that mode specifically.
- **No push notifications for group-specific moments** — join requests
  (the "Request" feature), tag mentions, pins, etc. are still pull/poll
  only. Real FCM push infrastructure now exists (2026-08-13/14, see
  `in-progress.md`/`risks.md`) and is wired into 1-on-1 messages, group
  messages, friend requests, and AI change-request updates — extending it
  to these other group events is just a matter of adding more
  `PushNotificationService.send(...)` call sites, not new plumbing.
- **Existing groups keep whatever `whoCanEditInfo` value they had before
  the VICE_CREATOR/CREATOR-only restriction landed** — no retroactive
  migration, so a pre-existing group could still be sitting on `ADMIN` or
  `MEMBER` until someone with permission explicitly changes the setting
  (which will then be validated against the new restriction).
- **"EXP for using everything in the app" (2026-08-03 ask, partially
  done).** Creating a group chat now awards Profile rank exp (50, same
  flat amount as completing an Invest > Learn lesson, via the same
  `RankUpService.checkRankUp` pattern - see `GroupChatService.createGroup`).
  The broader ask - "using everything in the app" - wasn't instrumented
  beyond that one action, since auditing every feature in the app for a
  sensible exp amount is its own unbounded task; worth a dedicated pass
  deciding which actions deserve exp and how much, rather than guessing
  amounts feature-by-feature inside this task.
