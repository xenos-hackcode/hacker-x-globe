# In progress

**Latest work (2026-08-09): Bots/"Leo" bot-builder platform, Round 1.**
Planned this session (plan file:
`C:\Users\WINDOWS11\.claude\plans\hashed-finding-trinket.md`), then Round 1
(character-sheet CRUD - new `Bots` table/service/routes/DTOs on the server,
`MemberBotsScreen.kt` rebuilt from a UI-only stub into a real
list + create/edit flow) built the same day. Compiled clean on both server
(`./gradlew compileKotlin`) and Android (`./gradlew compileDebugKotlin`);
**not yet deployed to cedal-server or installed on the test device** - see
`not-tested.md`'s new "Bots" entry. See `left-to-do.md`'s Bots entry for
Round 1's exact scope and the Round 2-4 roadmap (brain endpoint, Leo code
generation, premium/quota UI).

Also still pending from 2026-08-08 (same as before): per-chat "Add
Shortcut" deep-linking + the instant public-join/admin-approved private
invite-link flow, compiled clean but not yet installed/verified on device
either - see `not-tested.md`'s 2026-08-08 entries. Reminder: `cedal-server`
still needs a redeploy for "Known" calling, the 2026-08-08 join-flow
change, AND this Bots Round 1 work to all actually be live - none of it is
on the live `cedal-server-00105-nsk` revision yet. "Secretive" (in-app
data/video calling) is still deliberately not started - see
`left-to-do.md`, it needs a WebRTC signaling/TURN-cost decision first.
