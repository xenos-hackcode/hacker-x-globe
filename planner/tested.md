# Tested — actually exercised in the running app

"Tested" here means the user opened the real app on their device and did
the thing — not just that it compiled. Most of these were confirmed via bug
reports (finding something broken IS a test — it proves the feature was
reached and used, just that the result was wrong at the time). Each line
notes what was actually confirmed, and whether it's now fixed.

- **Leave flow** — user opened it, found it showed member names immediately
  instead of options-first. **Fixed and re-shipped**, not re-confirmed by
  the user since the fix.
- **Group chat sender names** — user noticed raw IDs instead of names in
  group chat bubbles. **Fixed and re-shipped**, not re-confirmed since.
- **Keyboard vs. chat input** — user confirmed the keyboard was covering
  the input box while typing in a group chat. **Fixed and re-shipped**, not
  re-confirmed since.
- **Tag/mention system (`#`/`@`)** — user tried tagging, gave detailed
  correction on the intended semantics (private/public meaning, color
  scheme, the hide-prompt flow being unwanted). Original Round-4 version
  was exercised; the Round-5 simplified version has **not** been
  re-confirmed by the user yet.
- **Locked-permission-row color** — user raised it as a concern; on
  inspection the code was already correct (red lock icon for every
  viewer when locked) — no fix needed, but also not independently
  re-confirmed by the user in the app after that check.
- **Auto-delete toggle** — user tried it, correctly identified it was a
  fixed 30-day on/off toggle instead of a real picker. **Fixed and
  re-shipped**, not re-confirmed since.
- **Group Profile layout** — user's "too gushed" feedback implies they
  opened and scrolled the screen before the Overview/Security/Link split
  existed. Not re-confirmed since the restructure.
- **Search tabs row (Search/Requests/QR/GR)** — user confirmed it looked
  bad / didn't fit on one line. **Fixed and re-shipped**, not
  re-confirmed since.

**Net: nothing in the Round 1-5 group chat feature has been confirmed
working end-to-end by the user since it was fixed.** Everything above is
"known broken, then patched" — worth a real pass to confirm the fixes
actually landed as intended, not just re-guessed-at.
