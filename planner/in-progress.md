# In progress

**Nothing is actively being worked on right now.** Latest work
(2026-08-09): planned and built Bots/"Leo" Round 1 (character-sheet CRUD -
see `done.md`), then redeployed `cedal-server` (now live at
`cedal-server-00107-4rj`, covering Known calling, the 2026-08-08 join-flow
change, and Bots Round 1 all at once) and reinstalled the debug APK on the
test device - see `done.md`'s 2026-08-09 entries and `README.md`'s
"Latest deploys" line.

While verifying the redeploy, found and fixed a pre-existing production
issue: `cedal-server` had no public IAM invoker binding at all, so every
request was 403'ing at the Google Frontend layer regardless of app
version - unrelated to this session's code changes (`gcloud run deploy`
doesn't touch IAM policy), likely broken for some unknown prior period.
Fixed with `gcloud run services add-iam-policy-binding cedal-server
--member=allUsers --role=roles/run.invoker`; confirmed the service
responds `200` again. Worth figuring out later how the binding
disappeared so it doesn't silently recur.

Everything above is now shipped in the full "done.md" sense (compiled,
deployed, installed) but unverified on-device - see `not-tested.md`'s
2026-08-08 and 2026-08-09 entries. Idle, waiting on either a
device-testing pass or new direction from the user. "Secretive" (in-app
data/video calling) is still deliberately not started - see
`left-to-do.md`, it needs a WebRTC signaling/TURN-cost decision first.
