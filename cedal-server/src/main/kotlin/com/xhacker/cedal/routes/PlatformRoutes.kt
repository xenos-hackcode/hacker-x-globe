package com.xhacker.cedal.routes

import com.xhacker.cedal.models.PlatformConfirmCodesRequest
import com.xhacker.cedal.models.PlatformRegisterEmailRequest
import com.xhacker.cedal.models.PlatformSendEmailRequest
import com.xhacker.cedal.models.PlatformSendSmsRequest
import com.xhacker.cedal.models.PlatformSubmitVerificationRequest
import com.xhacker.cedal.services.CodeGithubSyncService
import com.xhacker.cedal.services.PlatformDeveloperService
import com.xhacker.cedal.services.PlatformEmailService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.SecureRandom

// Self-service SMS relay platform signup - see PlatformDeveloperService's
// doc comment for the full flow. Plain server-rendered HTML + fetch() pages
// since the audience is external developers with no reason to have the
// Cedal app installed, and GitHub OAuth's redirect requires a real HTTP(S)
// URL to land on.
fun Route.platformRoutes() {
    val random = SecureRandom()
    fun randomState(): String {
        val b = ByteArray(24)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    route("/platform") {
        get("/signup") {
            val state = randomState()
            call.response.cookies.append(
                Cookie(
                    name = "platform_oauth_state", value = state,
                    maxAge = 600, path = "/", httpOnly = true, secure = true, extensions = mapOf("SameSite" to "Lax"),
                ),
            )
            call.respondText(signupHtml(state), ContentType.Text.Html)
        }

        get("/terms") {
            call.respondText(termsHtml(), ContentType.Text.Html)
        }

        // GitHub OAuth CSRF check: the state we handed out in /signup must
        // come back unchanged. Without this, an attacker could craft a
        // callback URL that completes OAuth as THEIR github account inside
        // a victim's browser session (classic OAuth login CSRF).
        get("/github/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]

            // Code area <-> GitHub sync uses this SAME callback URL (GitHub
            // OAuth Apps only ever have one registered callback), told apart
            // from the platform-signup flow below by whether `state` matches
            // a pending row CodeGithubSyncService minted - that flow's
            // authorize-url call comes from the app's API client, not a page
            // this server rendered, so it has no cookie to check against
            // (see CodeGithubSyncService.authorizeUrl/resolvePendingState).
            // Must run BEFORE the cookie-CSRF check below, since this flow
            // never sets platform_oauth_state.
            val codeSyncUserId = CodeGithubSyncService.resolvePendingState(state)
            if (codeSyncUserId != null) {
                val ok = code != null && CodeGithubSyncService.completeOAuth(codeSyncUserId, code)
                call.respondText(codeSyncCallbackHtml(ok), ContentType.Text.Html)
                return@get
            }

            val expectedState = call.request.cookies["platform_oauth_state"]
            if (state == null || expectedState == null || state != expectedState) {
                call.respondText(errorPage("Session expired or invalid - go back to /platform/signup and try again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                return@get
            }
            if (code == null) {
                call.respondText(errorPage("GitHub sign-in was cancelled or failed - go back and try again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                return@get
            }
            val user = PlatformDeveloperService.handleGitHubCallback(code)
            if (user == null) {
                call.respondText(errorPage("GitHub sign-in failed - go back and try again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                return@get
            }
            // Single-use proof of identity handed to the client - never the
            // raw githubId (see PlatformDeveloperService.createOAuthSession).
            val signupToken = PlatformDeveloperService.createOAuthSession(user.id.toString(), user.login)
            call.respondText(verifyFormHtml(signupToken, user.login), ContentType.Text.Html)
        }

        post("/verify/submit") {
            val req = call.receive<PlatformSubmitVerificationRequest>()
            when (val result = PlatformDeveloperService.submitVerification(req.signupToken, req.packageName, req.email, req.phone, req.acceptedTerms)) {
                PlatformDeveloperService.SubmitResult.Sent ->
                    call.respondText(confirmFormHtml(req.signupToken), ContentType.Text.Html)
                PlatformDeveloperService.SubmitResult.InvalidSession ->
                    call.respondText(errorPage("Your sign-in expired - go back to /platform/signup and start again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                PlatformDeveloperService.SubmitResult.TermsNotAccepted ->
                    call.respondText(errorPage("You need to accept the Terms to continue."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                is PlatformDeveloperService.SubmitResult.InvalidInput ->
                    call.respondText(errorPage(result.reason), ContentType.Text.Html, HttpStatusCode.BadRequest)
                PlatformDeveloperService.SubmitResult.AlreadyRegistered ->
                    call.respondText(errorPage("That GitHub account or package name is already registered."), ContentType.Text.Html, HttpStatusCode.Conflict)
                PlatformDeveloperService.SubmitResult.TooSoon ->
                    call.respondText(errorPage("A code was already sent recently - check your email/texts, or wait a minute before requesting a new one."), ContentType.Text.Html, HttpStatusCode.TooManyRequests)
                PlatformDeveloperService.SubmitResult.EmailFailed ->
                    call.respondText(errorPage("Couldn't send the verification email - double check the address and try again."), ContentType.Text.Html, HttpStatusCode.BadGateway)
            }
        }

        post("/verify/confirm") {
            val req = call.receive<PlatformConfirmCodesRequest>()
            when (PlatformDeveloperService.confirmCodes(req.signupToken, req.emailCode, req.phoneCode)) {
                PlatformDeveloperService.ConfirmResult.Completed ->
                    call.respondText(doneHtml(), ContentType.Text.Html)
                PlatformDeveloperService.ConfirmResult.Expired ->
                    call.respondText(errorPage("Those codes expired - go back to /platform/signup and start again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                PlatformDeveloperService.ConfirmResult.InvalidCode ->
                    call.respondText(errorPage("One or both codes are wrong."), ContentType.Text.Html, HttpStatusCode.BadRequest)
                PlatformDeveloperService.ConfirmResult.InvalidSession ->
                    call.respondText(errorPage("Your session expired - go back to /platform/signup and start again."), ContentType.Text.Html, HttpStatusCode.BadRequest)
            }
        }

        // The actual ongoing API a developer's own app/backend calls to
        // send an SMS through their own relay phone.
        post("/sms/send") {
            val header = call.request.headers["Authorization"] ?: ""
            val key = header.removePrefix("Bearer ").trim()
            val developerId = PlatformDeveloperService.verifyDeveloperToken(key)
            if (developerId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val req = call.receive<PlatformSendSmsRequest>()
            PlatformDeveloperService.enqueueSms(developerId, req.phoneNumber, req.message)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }

        // Email add-on - optional, registered after signup using the same
        // activation key. See PlatformEmailService for the two modes.
        post("/email/register") {
            val key = (call.request.headers["Authorization"] ?: "").removePrefix("Bearer ").trim()
            val req = call.receive<PlatformRegisterEmailRequest>()
            when (PlatformEmailService.registerCredentials(key, req.mode, req.host, req.port, req.username, req.password, req.from)) {
                PlatformEmailService.RegisterResult.Registered -> call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                PlatformEmailService.RegisterResult.InvalidKey -> call.respond(HttpStatusCode.Unauthorized)
                PlatformEmailService.RegisterResult.InvalidMode -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "mode must be \"own_smtp\" or \"shared\""))
                PlatformEmailService.RegisterResult.MissingFields -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "host, username, and password are required for own_smtp"))
                PlatformEmailService.RegisterResult.ConnectionFailed -> call.respond(HttpStatusCode.BadGateway, mapOf("error" to "Couldn't authenticate with that SMTP server - check your credentials"))
            }
        }

        post("/email/send") {
            val key = (call.request.headers["Authorization"] ?: "").removePrefix("Bearer ").trim()
            val req = call.receive<PlatformSendEmailRequest>()
            when (PlatformEmailService.sendEmail(key, req.to, req.subject, req.body)) {
                PlatformEmailService.SendResult.Sent -> call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                PlatformEmailService.SendResult.InvalidKey -> call.respond(HttpStatusCode.Unauthorized)
                PlatformEmailService.SendResult.NotRegistered -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Call /platform/email/register first"))
                PlatformEmailService.SendResult.RateLimited -> call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Daily shared-mode send limit reached"))
                PlatformEmailService.SendResult.SendFailed -> call.respond(HttpStatusCode.BadGateway, mapOf("error" to "Send failed"))
            }
        }
    }
}

private fun htmlEscape(s: String): String = s
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;").replace("'", "&#39;")

private fun pageShell(body: String): String = """
<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cedal - Developer Signup</title>
<style>
:root{color-scheme:dark}
*{box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;margin:0;min-height:100vh;
  background:radial-gradient(circle at 15% -10%,#0b2436 0%,#020617 45%),#020617;color:#e5e7eb;
  display:flex;align-items:center;justify-content:center;padding:32px 18px}
.wrap{width:100%;max-width:420px}
.brand{display:flex;align-items:center;gap:10px;margin-bottom:22px}
.brand .glyph{width:34px;height:34px;border-radius:10px;background:#0f172a;border:1px solid rgba(56,189,248,.65);
  display:flex;align-items:center;justify-content:center;color:#38bdf8;font-size:16px}
.brand .word{font-weight:800;letter-spacing:3px;font-size:16px;color:#e5e7eb}
.brand .sub{font-size:10px;letter-spacing:2px;color:#38bdf8;text-transform:uppercase;margin-top:1px}
.card{background:rgba(15,23,42,.92);border:1px solid rgba(148,163,184,.18);border-radius:18px;padding:26px 24px;
  box-shadow:0 20px 60px -20px rgba(0,0,0,.6)}
h2{margin:0 0 8px;font-size:19px;color:#e5e7eb}
p{color:#9ca3af;line-height:1.55;font-size:13.5px;margin:0 0 16px}
code{background:#0b1220;border:1px solid rgba(148,163,184,.25);border-radius:5px;padding:1px 6px;font-size:12.5px;color:#38bdf8}
label{display:block;font-size:11.5px;letter-spacing:.4px;color:#64748b;text-transform:uppercase;margin:14px 0 6px}
input[type=text],input[type=email],input[type=tel]{width:100%;padding:11px 12px;border-radius:9px;
  border:1px solid rgba(148,163,184,.3);background:#0b1220;color:#e5e7eb;font-size:14px;outline:none;transition:border-color .15s}
input:focus{border-color:#38bdf8}
.check-row{display:flex;align-items:flex-start;gap:9px;margin-top:16px}
.check-row input{margin-top:3px}
.check-row label{margin:0;text-transform:none;font-size:12.5px;color:#9ca3af;letter-spacing:0}
.check-row a{color:#38bdf8;text-decoration:none}
button,a.btn{appearance:none;border:none;width:100%;display:block;text-align:center;text-decoration:none;
  background:linear-gradient(180deg,#22d3ee,#0891b2);color:#04121a;font-weight:700;font-size:14px;
  padding:12px 16px;border-radius:10px;cursor:pointer;margin-top:20px;transition:filter .15s}
button:hover,a.btn:hover{filter:brightness(1.08)}
button:disabled{opacity:.5;cursor:not-allowed;filter:none}
.foot{margin-top:18px;text-align:center;font-size:11px;color:#475569}
.err{background:rgba(248,113,113,.1);border:1px solid rgba(248,113,113,.35);color:#f87171;
  border-radius:9px;padding:10px 12px;font-size:12.5px;margin-top:14px}
ul{color:#9ca3af;font-size:13px;line-height:1.6;padding-left:18px}
h3{font-size:13px;color:#e5e7eb;margin:18px 0 6px}
</style></head>
<body><div class="wrap">
<div class="brand"><div class="glyph">&#9678;</div><div><div class="word">CEDAL</div><div class="sub">Developer Platform</div></div></div>
<div class="card">$body</div>
<div class="foot">Self-hosted SMS relay - bring your own phone, your own carrier plan.</div>
</div></body></html>
""".trimIndent()

private fun errorPage(message: String): String = pageShell(
    """<h2>Something's not right</h2><div class="err">${htmlEscape(message)}</div>
    <a class="btn" href="/platform/signup">Start over</a>""",
)

// Landing page for the Code-area GitHub sync OAuth callback - the browser
// tab this opens in isn't the Cedal app, so it hands control back via a
// custom-scheme deep link (see MainActivity.onNewIntent), auto-firing via
// the script tag and falling back to a manual tap for browsers that block
// programmatic scheme navigation without a gesture.
private fun codeSyncCallbackHtml(ok: Boolean): String {
    val target = if (ok) "cedalcode-oauth://github-callback?ok=true" else "cedalcode-oauth://github-callback?ok=false"
    val heading = if (ok) "Connected!" else "Connection failed"
    val message = if (ok) "GitHub is linked - return to the Cedal app to continue." else "GitHub sign-in didn't complete - return to the Cedal app and try again."
    return pageShell(
        """
        <h2>$heading</h2><p>$message</p>
        <a class="btn" href="$target">Return to Cedal</a>
        <script>location.replace('$target')</script>
        """,
    )
}

private fun signupHtml(state: String): String = pageShell(
    """
    <h2>Developer Signup</h2>
    <p>Run your own SMS relay against your own phone and carrier plan instead of paying for Twilio. Sign in with GitHub to start - it takes about a minute.</p>
    <a class="btn" href="${PlatformDeveloperService.githubAuthorizeUrl(state)}">Sign in with GitHub</a>
    <div class="foot">By continuing you'll be asked to accept the <a href="/platform/terms" style="color:#38bdf8">Terms</a>.</div>
    """,
)

private fun verifyFormHtml(signupToken: String, githubLogin: String): String = pageShell(
    """
    <h2>Hi, ${htmlEscape(githubLogin)}</h2>
    <p>Enter your Android app's package name, and an email + phone number we can verify.</p>
    <form id="f">
      <label>Package name</label><input type="text" id="packageName" placeholder="com.yourname.yourapp" required>
      <label>Email</label><input type="email" id="email" required>
      <label>Phone (with country code)</label><input type="tel" id="phone" placeholder="+15551234567" required>
      <div class="check-row">
        <input type="checkbox" id="terms" required>
        <label for="terms">I've read and accept the <a href="/platform/terms" target="_blank">Terms &amp; Conditions</a>.</label>
      </div>
      <button type="submit">Send codes</button>
    </form>
    <script>
    document.getElementById('f').onsubmit = async (e) => {
      e.preventDefault();
      const res = await fetch('/platform/verify/submit', {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({
        signupToken: ${jsStr(signupToken)},
        packageName: document.getElementById('packageName').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value,
        acceptedTerms: document.getElementById('terms').checked,
      })});
      document.querySelector('.card').innerHTML = await res.text();
    };
    </script>
    """,
)

private fun confirmFormHtml(signupToken: String): String = pageShell(
    """
    <h2>Check your email and texts</h2>
    <p>Two codes were sent - one by email, one by SMS. Enter both below.</p>
    <form id="f">
      <label>Email code</label><input type="text" id="emailCode" required>
      <label>SMS code</label><input type="text" id="phoneCode" required>
      <button type="submit">Confirm</button>
    </form>
    <script>
    document.getElementById('f').onsubmit = async (e) => {
      e.preventDefault();
      const res = await fetch('/platform/verify/confirm', {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({
        signupToken: ${jsStr(signupToken)},
        emailCode: document.getElementById('emailCode').value,
        phoneCode: document.getElementById('phoneCode').value,
      })});
      document.querySelector('.card').innerHTML = await res.text();
    };
    </script>
    """,
)

private fun doneHtml(): String = pageShell(
    """
    <h2>You're set</h2>
    <p>Your activation key was split in two: the first half is in your email, the second half was texted to your phone.
    Combine them (first half immediately followed by second half, no space) - that's your real key.</p>
    <p>Install the <code>cedal-sms-relay</code> app on your own phone, enter this server's URL and your combined key,
    and start it. From then on your SMS traffic goes through your own phone only - it never touches ours again.</p>
    """,
)

private fun termsHtml(): String = pageShell(
    """
    <h2>Terms &amp; Conditions</h2>
    <p>Effective for anyone signing up for the Cedal SMS relay developer platform.</p>
    <h3>1. What this is</h3>
    <p>A free, best-effort self-hosted SMS relay: your own phone, running the <code>cedal-sms-relay</code> app, sends
    messages your own backend enqueues via your API key. There is no uptime guarantee and no support SLA.</p>
    <h3>2. Your responsibility</h3>
    <p>You are solely responsible for the content of every message you send, and for complying with SMS/telecom
    regulations in your jurisdiction (e.g. consent and opt-out requirements). Do not use this to send spam,
    harassment, or unlawful content of any kind.</p>
    <h3>3. Your key</h3>
    <p>Your activation key is shown to you exactly once, split across email and SMS. Keep it private - anyone who
    has it can send messages through your phone. We cannot recover a lost key; you would need to contact us for a
    reset.</p>
    <h3>4. Data</h3>
    <p>Your email and phone number are used only to deliver verification codes and your key, then stored solely as
    one-way hashes - we do not retain them in plaintext and cannot look them up or use them to contact you again.</p>
    <h3>5. Termination</h3>
    <p>Access may be revoked at any time for abuse, without notice.</p>
    <h3>6. No warranty</h3>
    <p>Provided "as is," with no warranty of any kind, express or implied.</p>
    <a class="btn" href="/platform/signup">Back to signup</a>
    """,
)

// Minimal JS string-literal escaping for values interpolated into inline
// <script> blocks above (signupToken is a random hex token so this is
// belt-and-suspenders, not load-bearing - but cheap insurance).
private fun jsStr(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
