# In progress

**Nothing is actively being worked on right now.** Latest work (2026-08-08,
same day as "Known" calling round 1):
- Per-chat "Add Shortcut" now actually deep-links into the right chat
  instead of just opening the app.
- Public group join (search or link) is now instant, no approval; private
  groups get an admin-tier-only invite link whose join still needs
  admin-tier approval. See `done.md`'s 2026-08-08 entries.

Both compiled clean on server and Android; not yet installed/verified on
device - see [not-tested.md](not-tested.md)'s new 2026-08-08 entries.
Reminder: `cedal-server` still needs a redeploy for "Known" calling (and
now this join-flow change too) to actually be live. "Secretive" (in-app
data/video calling) is deliberately not started - see `left-to-do.md`, it
needs a WebRTC signaling/TURN-cost decision first. Idle, waiting on either
a device-testing pass or new direction from the user.
