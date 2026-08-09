# In progress

**2026-08-09: `cedal-server` redeployed, live at revision
`cedal-server-00107-4rj`.** Covers "Known" calling, the 2026-08-08
join-flow change, and Bots Round 1 (below) - all now actually live, not
just compiled. Deployed via `gcloud run deploy --source .` (no existing
deploy script for this repo, unlike `cedal-services/*`).

**Found and fixed while verifying the deploy: the service had NO public
IAM invoker binding** (`gcloud run services get-iam-policy` returned empty
bindings) - every request was hitting a Google-Frontend-level 403 before
it ever reached Ktor, regardless of this session's changes. `gcloud run
deploy` doesn't touch IAM policy on redeploy, so this predates this
session's redeploy - the API was very likely already down for real usage
before now, for an unknown period back to whenever the invoker binding was
lost. Fixed with `gcloud run services add-iam-policy-binding cedal-server
--member=allUsers --role=roles/run.invoker`; confirmed the service
responds `200` again afterward. Worth figuring out later how the binding
disappeared (manual console edit? org policy sync?) so it doesn't
silently recur.

**Bots/"Leo" bot-builder platform, Round 1** (planned + built 2026-08-09,
plan file `C:\Users\WINDOWS11\.claude\plans\hashed-finding-trinket.md`):
character-sheet CRUD - new `Bots` table/service/routes/DTOs on the server,
`MemberBotsScreen.kt` rebuilt from a UI-only stub into a real list +
create/edit flow (`MemberBotsListBody`/`MemberBotEditBody`). Compiles
clean on both server and Android (a stale `MemberBotsBody` import in
`NavGraph.kt` broke the first Android compile attempt - caught by actually
reading the build log instead of trusting the task-completion summary,
fixed in a follow-up commit). **Now live on the redeployed server above.**
See `left-to-do.md`'s Bots entry for Round 1's exact scope and the
Round 2-4 roadmap (brain endpoint, Leo code generation, premium/quota UI).

**APK install still outstanding** - the connected test device (Galaxy
A54, `SM-A546B`) dropped off `adb` partway through this session's
`installDebug` run (12-minute build, device wasn't visible by the end -
likely a USB/debugging-timeout disconnect, not a build problem). Needs the
device reconnected/unlocked and a fresh `gradlew installDebug` before any
of this session's work - or the 2026-08-08 work before it - can actually
be tested on-device. "Secretive" (in-app data/video calling) is still
deliberately not started - see `left-to-do.md`, it needs a WebRTC
signaling/TURN-cost decision first.
