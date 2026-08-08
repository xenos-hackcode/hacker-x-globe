# Cedal — Planner

Replaces the old root `PLANS.md` (single running log) with a folder split by
status, so "what's done" doesn't get buried under "what's left" in one long
file. Old dated entries weren't deleted — they moved to `history/`, same
"mark DONE, never delete" rule `PLANS.md` used to state at its top.

**Last updated:** 2026-08-08
**Latest deploys:** `cedal-server` revision `cedal-server-00105-nsk` (Cloud
Run, `cedal-fd4a2`/`us-central1`) · `cedal-android` debug APK installed on
the connected test device (includes the Round 5 + GR-tab-label fix).

## Files here

- **[done.md](done.md)** — shipped: compiled, deployed, installed.
- **[tested.md](tested.md)** — of the above, what the user has actually
  exercised in the running app and confirmed (by finding bugs in it, which
  is how most of this list was built).
- **[not-tested.md](not-tested.md)** — shipped but nobody has clicked
  through it on-device yet. This is most of the list — an AI can compile
  and deploy Compose UI but can't tap through it.
- **[in-progress.md](in-progress.md)** — what's actively being worked on
  right now (currently: nothing — idle, waiting on the next round of
  feedback or a new task).
- **[left-to-do.md](left-to-do.md)** — deliberately deferred or explicitly
  still-open items.
- **[risks.md](risks.md)** — known simplifications, edge cases, and things
  that could bite later.
- **[needs.md](needs.md)** — what has to come from the user (device testing,
  decisions) before certain items can move from "not tested" to "done" or
  from "left to do" to "in progress."
- **[history/](history/)** — the old `PLANS.md` dated entries, one file
  each, preserved as-is plus a new entry for the Round 1-5 group chat
  expansion (the bulk of what `done.md` now summarizes).

## How to keep this current

- Finish something → move its line from `left-to-do.md` (or add fresh) to
  `done.md`.
- User reports trying a feature (works OR finds a bug) → move it to
  `tested.md` with a one-line note of what was confirmed either way. A bug
  report IS a test — it means the feature was exercised, even if the
  result was "broken."
- Starting real work on something → note it in `in-progress.md`, remove
  once it lands in `done.md`.
- Discover a real design compromise while building → add it to `risks.md`
  with why it's a deliberate simplification and what would need to change
  to fix it properly.
