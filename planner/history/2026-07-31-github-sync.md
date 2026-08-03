# Code area <-> GitHub two-way sync (2026-07-31)

**Status: DONE, unverified on-device** — server and Android both compile clean.
No emulator/device run-through has happened yet (see Verification below,
copied from the plan doc) - do that before calling this shipped, especially
the OAuth deep-link return trip, which has zero precedent elsewhere in this
app and is the riskiest new piece. (Note: a second deep-link scheme,
`cedalcode://group/{token}`, was added later for the group invite Link tab -
see `2026-08-03-group-chat-expansion.md` - following the same pattern this
entry established.)

**Full design doc:** `C:\Users\WINDOWS11\.claude\plans\mellow-doodling-glacier.md`

**What:** The Code area's Documents tab (a phone-local file tree, never
previously touched the server) can now connect the user's own GitHub
account via OAuth (`repo` scope) and two-way sync the whole folder against
a chosen repo - push local changes, pull remote ones, and surface real
conflicts (changed on both sides since the last sync) for the user to
resolve file-by-file (Keep Local / Keep GitHub, no merge view).

**Key decisions** (see design doc for full justification of each):
- Reuses the GitHub OAuth App already registered for the Developer Mode
  signup flow (`GITHUB_OAUTH_CLIENT_ID/SECRET`, already in Secret Manager) -
  scope is chosen per-authorization, not fixed at app registration, so this
  flow requests `repo` while that one still requests `read:user`. Both
  share the ONE registered callback URL (`/platform/github/callback`),
  disambiguated server-side by whether `state` matches a pending row this
  flow minted (`PendingCodeGithubOAuth`) - must run before that route's
  existing cookie-CSRF check, since this flow never sets that cookie.
- `GitHubService.kt` split into a reusable `GitHubRepoClient` class
  (per-call owner/repo/token, gains `getRecursiveTree`/`getBlob`/
  `deleteFile`) and a thin `object GitHubService` facade unchanged for its
  two existing callers (`AiChangeRequestService`/`DeveloperSubmissionService`,
  the app's-own-repo/one-PAT/no-delete-capability flow - untouched trust
  boundary).
- Sync is N sequential Contents-API commits (not a Git Data API atomic
  multi-file commit) - noisier history, but `putFile` is already idempotent
  create-or-update so a partial failure just re-runs cleanly, and building
  real tree/blob/commit plumbing wasn't worth it on top of everything else
  net-new here.
- Conflict detection is a per-file last-synced fingerprint
  (`CodeSyncFiles`: GitHub blob sha + local content hash) - deliberately
  not a CRDT/merge system.
- The GitHub access token is AES-GCM encrypted (`CryptoService`, the same
  mechanism `PlatformEmailCredentials` already uses for a developer's SMTP
  password) before being stored in `CodeGithubConnections`.
- **The OAuth browser-redirect-back-into-the-app piece is entirely new
  infrastructure** - this app never had deep-link handling before. New
  custom scheme `cedalcode-oauth://github-callback`, `MainActivity`
  `android:launchMode="singleTask"` + a new `onNewIntent` override, handed
  off via a new `CodeGithubOAuthState` ambient singleton (same idiom as
  `AppLockState`/`CornealBubbleState`) that `MemberCodeBody` observes.
- Binary files are skipped in v1 (NUL-byte heuristic) - `CodeStorage`'s
  read/write is UTF-8 text only, no byte-array path exists yet.

**Files touched:** new `cedal-server` `services/CodeGithubSyncService.kt` +
`routes/CodeGithubRoutes.kt`; modified `db/Tables.kt` (4 new tables),
`services/GitHubService.kt`, `routes/PlatformRoutes.kt`, `models/Models.kt`,
`db/DatabaseFactory.kt`, `Application.kt`. New `cedal-android`
`ui/CodeGithubOAuthState.kt` + `data/CodeGithubModels.kt`; modified
`AndroidManifest.xml`, `MainActivity.kt`, `data/ApiService.kt`,
`viewmodel/AuthViewModel.kt`, `ui/screens/member/MemberCodeScreen.kt` (new
"GitHub" toolbar button in the Documents tab, alongside the existing `+`/`⋮`).

**Two bugs found and fixed only by actually compiling** (not caught by
review): a `Modifier.padding(horizontal = ..., top = ...)` call mixing
params from two different overloads (Compose doesn't allow that); and a
Gradle daemon file-lock `IOException` on a rebuild that looked like a real
compile failure until a clean re-run (after `gradlew --stop`) confirmed it
wasn't.

**Verification (not yet done - needs a real device):**
1. Connect: Documents tab -> GitHub -> Connect GitHub. Confirm the browser
   shows `repo` scope requested, and after approving, the app returns to
   the foreground on the Documents tab (not a duplicate task in Recents,
   confirming `singleTask` + `onNewIntent` actually worked) with the menu
   now offering "Choose Repo". Restart the app fully and confirm connection
   state persists without re-prompting OAuth.
2. Choose a small test repo; confirm the menu updates to "Sync with
   owner/repo".
3. Push 3-4 files (with a subfolder), tap Sync, confirm they land on GitHub
   correctly and the commit history shows N sequential commits.
4. Edit one file on github.com only, Sync again, confirm it pulls down with
   no conflict; edit a different file locally only, Sync, confirm it pushes
   with no conflict.
5. Edit the SAME file on both sides without syncing between, Sync, confirm
   it's flagged as a conflict (not silently overwritten either direction),
   and that both "Keep Local" and "Keep GitHub" (tried on two separate
   forced conflicts) resolve it correctly.
6. Delete propagation both directions; a dropped-in binary file gets
   skipped with a notice instead of erroring the whole sync; Disconnect
   actually clears connection state.
7. Regression: the existing Code -> Rules AI-change-request flow and the
   Developer Mode submission-approval flow still work unchanged (confirms
   the `GitHubService` facade split didn't disturb its two existing callers).
