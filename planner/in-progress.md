# In progress

**Nothing is actively being worked on right now.** Latest work
(2026-08-09, continuing the same day as the Bots Round 1 + redeploy work
below): several install rounds across 4 different physical devices
(Galaxy A54, X32, Galaxy A53, and the A54 again after a reconnect) to
confirm the redeployed build actually installs cleanly - all succeeded
(one needed an uninstall first, `INSTALL_FAILED_UPDATE_INCOMPATIBLE` from
a differently-signed prior install; one hit a transient USB `EOF` on
first try, succeeded on retry - neither was a build problem).

**Signup screen**: the single phone number field is now split into a
"+ [country code]" box and a separate local-number box, combined into
E.164 on submit (leading `0` stripped from the local part). Compiled,
installed, live on `cedal-server-00109-vbk`.

**Full production account wipe, admin-requested and executed 2026-08-09.**
Every real user account (and everything tied to one - chats, groups,
bots, etc.) was deleted via a temporary admin-only route
(`POST /temp-wipe-all-accounts`, one-off hardcoded secret) that ran raw
`TRUNCATE TABLE users CASCADE` - chosen over looping the existing
`AccountService.deleteAccount` because that function's hand-maintained
table list turned out to have real gaps (see `risks.md`'s new entry).
The system account and "Cedal Team" account got wiped too but both
self-heal on next use, so nothing is actually broken by that. The route
was deployed, triggered once (by the user directly - the assistant's
attempt to fire the actual destructive HTTP call was blocked by the
permission layer, by design, so the user ran it), then removed and
redeployed clean the same session - confirmed gone (`404`) on
`cedal-server-00109-vbk`. **The live database is now empty of real user
accounts.**

While auditing `AccountService.deleteAccount` for that wipe, fixed the
`Bots` gap (added with Round 1 the same day) but found several other
pre-existing gaps that are still NOT fixed - self-service "Delete
Account" is currently broken for a lot of real usage, see the new
`risks.md` entry for the full list and why it matters.

Idle, waiting on either a device-testing pass or new direction from the
user. "Secretive" (in-app data/video calling) is still deliberately not
started - see `left-to-do.md`, it needs a WebRTC signaling/TURN-cost
decision first.
