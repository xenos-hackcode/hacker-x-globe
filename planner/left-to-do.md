# Left to do

## Deliberately deferred (explicit scope cuts, confirmed with the user)

- **Voice chat** — a standalone real-time subsystem (WebRTC/signaling),
  comparable in size to the entire group chat expansion on its own.
- **Live location sharing** — background location + live map, same scale
  concern as voice chat.
- **"Bots"/"Leo" AI bot-builder platform (2026-08-03 ask) - NOT STARTED,
  needs its own dedicated planning pass before any code.** Recorded here in
  full so nothing gets lost, per the user's "never forget" instruction.
  **What exists today:** `MemberBotsBody` (`MemberBotsScreen.kt`) is
  already a real "character sheet" form (name/age/gender/character/
  personality/bio/occupation/life story/description), ported from
  cedal-mobile's `bots.tsx` - but `handleSave()` is an explicit stub, never
  persists anywhere, and the file's own comment already flags "Leo" (a
  draggable AI assistant that helps fill the form) as "a distinct AI-chat
  feature... its own later milestone" - i.e. this was already scoped as
  future work before this session, the user is now asking to actually
  build it.
  **What the user described wanting, verbatim-ish:**
  - The Bots form/feature should let a user build bots for WhatsApp,
    Telegram, "and so more" (other chat platforms).
  - An AI named **Leo** reads whatever the user filled into the form and
    "sets it down as code" - i.e. generates the actual bot implementation.
  - The bot "gets a free API key out there" - some external/free-tier LLM
    API key, auto-provisioned somehow.
  - User can optionally add a picture/icon for their bot.
  - **Monetization/quota idea:** a free session is capped at "1000 tokens"
    of usage; past that, the user needs to buy "premium" for £10 (their
    stated lowest price point) to continue - and buying premium is what
    triggers creating them a personal API key.
  - **Explicit fallback if auto-provisioning an API key turns out not to
    be feasible:** build everything else (the form → Leo → generated bot
    code flow), but require the user to bring their own API key (BYOK)
    instead of the app provisioning one - "no API gotten by us, user would
    need to get their own API."
  **Why this wasn't just built:** genuinely large, multi-part, and touches
  things that shouldn't be guessed at -
  1. **Real payment processing** (£10 purchase) - Google Play Billing vs.
     a custom flow is a real decision with financial/legal weight, same
     category of thing this session already avoids guessing at.
  2. **"Free API key out there" is technically unclear** - most LLM
     providers require manual signup/ToS acceptance per account; having
     the app auto-provision a key on a user's behalf (rather than the
     BYOK fallback the user already described) needs real research into
     which provider and whether that's even allowed under that
     provider's terms before committing to it.
  3. **Third-party bot-platform integration** (WhatsApp Business API,
     Telegram Bot API, "and so more") is significant scope on its own -
     each platform has its own registration/webhook/hosting requirements
     for a bot to actually run somewhere.
  4. **"Leo reads everything and sets it down as code"** needs a concrete
     technical design (what does the generated code look like, where does
     it run, is it hosted by cedal-server or handed to the user) before
     it's buildable at all.
  This is comparable in size to the voice-chat/live-location cuts above -
  worth its own planning session (probably its own multi-round build, the
  same way the group chat expansion took 5 rounds) rather than guessing at
  the payment/provisioning/hosting questions inside an unrelated task.

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
- **No push notifications** for any of this — join requests, tags,
  pins, etc. are all pull/poll, matching how the rest of the app already
  works, not a gap introduced by this work specifically.
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
