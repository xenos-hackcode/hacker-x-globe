# Cedal — Product/Business Reminders

## Virtual Items / Star Coins (2026-07-02)

Decision: **one-way purchase only** for now — users can buy Star Coins/items
with real money, but there is no cash-out path. This matches how most game
currencies (Robux, V-Bucks, etc.) work and avoids money-transmission
regulation. The terms text (`TermsConfig.TEXT` in cedal-android, and the RN
app's `terms-full.tsx`) already reflects this: "Virtual Items have no
real-world monetary value... cannot be exchanged for cash outside Cedal."

Future plan: once 18, look into partnering with an already-licensed payments
business to offer real two-way cash-out. Two-way conversion of a virtual
currency requires money transmitter licensing (FinCEN MSB registration + per-
state licenses in the US, or an e-money institution license in the UK/EU),
KYC/AML compliance, and banking relationships most banks won't extend to an
unlicensed entity — operating it without a license is illegal, not just
risky. Don't build cash-out functionality before this is sorted with a
licensed partner and real legal counsel.

## Terms of Use & age (2026-07-02)

Currently under 18. In most jurisdictions, minors can't independently enter
binding legal agreements (like the app's own Terms of Use) without a parent/
guardian's consent. Not a blocker for building/testing the app solo, but
needs addressing (parental consent flow, or adult co-signer/business
structure) before this becomes a real public-facing product.
