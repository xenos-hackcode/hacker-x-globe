package com.xhacker.cedal.ui.screens.member

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.WebView
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xhacker.cedal.ui.screens.PermissionBlockedDialog
import com.xhacker.cedal.ui.screens.rememberPermissionGate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.xhacker.cedal.data.AiChangeRequestDto
import com.xhacker.cedal.data.AiCurrentFileDto
import com.xhacker.cedal.data.AiFileActionDto
import com.xhacker.cedal.data.AndroidBuildJob
import com.xhacker.cedal.data.CodeFile
import com.xhacker.cedal.data.CodeLanguageItem
import com.xhacker.cedal.data.CodeRunResult
import com.xhacker.cedal.data.CodeStorage
import com.xhacker.cedal.data.CodeSyncFileEntry
import com.xhacker.cedal.data.GithubRepoDto
import com.xhacker.cedal.data.GithubStatusDto
import com.xhacker.cedal.data.SyncConflictDto
import com.xhacker.cedal.data.SyncJobDto
import com.xhacker.cedal.ANDROID_BUILD_NOTIFICATION_CHANNEL_ID
import com.xhacker.cedal.ui.CodeGithubOAuthResult
import com.xhacker.cedal.ui.CodeGithubOAuthState
import com.xhacker.cedal.ui.CodeRunSession
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// The "Code" work mode — a notepad, not an IDE: plain text in, real output
// out, any language. Execution happens server-side (see CodeExecutionService
// in cedal-server, which proxies to the free Wandbox run API) rather than
// on-device, since there's no safe way to run arbitrary user code directly
// on the phone or on our own server without a real sandbox.
//
// Laid out like Invest's Overview/Watchlist/Trade/Learn bottom-tab router:
// Pad (a read-only address bar showing what you've entered, then edit +
// Run), Command (stdin — what your program reads, like typing at a command
// prompt), Documents (a real file explorer backed by an actual folder on the
// phone's storage — see CodeStorage — so everything genuinely persists),
// View (the last run's output). State is hoisted here and shared across all
// four tabs.
//
// Documents owns naming; Pad only edits + runs whatever you've "Entered".
// A file's language always comes from its extension (script.py, main.go,
// ...) — there's no manual override, since renaming (and therefore fixing
// an unrecognized extension) is Documents' job now.
// Not private - DeveloperScaffold.kt drives this externally (via
// MemberCodeBody's externalActiveTab param) to promote Pad/Documents/View
// into its own top-level Code/Explorer/View tabs. See that param's doc
// comment on MemberCodeBody.
internal enum class CodeTab { PAD, COMMAND, DOCUMENTS, VIEW, RULES }

// Mirrors CodeStorage's CodeNode (id/parentId/name/isFolder, backed by a
// real DocumentFile) — kept as a distinct type in the UI layer so Pad/
// Documents don't need to import the storage layer's model directly.
private data class CodeEntry(val id: String, val parentId: String?, val name: String, val isFolder: Boolean)

// Result of an "ad" (this app's "cd") navigation attempt — newLocationId is
// where you end up (unchanged from the input if the move failed), error is
// the message to show if it did.
private data class AdResult(val newLocationId: String?, val error: String?)

// The full "folder\file" style path to an entry, backslash-separated to
// match how the address bar in Pad should read — just the name if it's at
// the root (file or folder), or "parent\child" for anything nested.
private fun addressFor(entries: List<CodeEntry>, entry: CodeEntry): String {
    val parts = mutableListOf(entry.name)
    var parentId = entry.parentId
    while (parentId != null) {
        val parent = entries.firstOrNull { it.id == parentId } ?: break
        parts.add(0, parent.name)
        parentId = parent.parentId
    }
    return parts.joinToString("\\")
}

// All ids in the subtree rooted at rootId, rootId included — used to guard
// against moving/copying a folder into its own descendant (the real delete
// itself just relies on the storage provider recursively deleting a
// non-empty folder, which the standard external-storage provider does).
private fun List<CodeEntry>.subtreeIds(rootId: String): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque<String>().apply { add(rootId) }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (result.add(current)) {
            this.filter { it.parentId == current }.forEach { queue.add(it.id) }
        }
    }
    return result
}

// All non-folder descendants of folderId (folderId == null means the true
// root), recursive — used by Command's "run" to bundle a whole folder as one
// project instead of a single file.
private fun filesUnder(entries: List<CodeEntry>, folderId: String?): List<CodeEntry> {
    val result = mutableListOf<CodeEntry>()
    val queue = ArrayDeque<String?>().apply { add(folderId) }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        entries.filter { it.parentId == current }.forEach { child ->
            if (child.isFolder) queue.add(child.id) else result.add(child)
        }
    }
    return result
}

// Same idea as addressFor, but relative to rootId instead of the true root —
// this is the "file" path Wandbox/Kotlin Playground need for a bundled
// supporting file (e.g. "utils/helper.py"), forward-slashed to match.
private fun relativePath(entries: List<CodeEntry>, rootId: String?, entry: CodeEntry): String {
    val parts = mutableListOf(entry.name)
    var parentId = entry.parentId
    while (parentId != null && parentId != rootId) {
        val parent = entries.firstOrNull { it.id == parentId } ?: break
        parts.add(0, parent.name)
        parentId = parent.parentId
    }
    return parts.joinToString("/")
}

// Plain text search across every OTHER file for the current file's name -
// lets the AI (see AiChangeRequestService.processFileAction server-side)
// notice "app.py imports welcome" before renaming/converting/deleting
// welcome.py, and ask permission before touching app.py too, instead of
// silently leaving a dangling reference. Capped (file count + total chars)
// since this reads every candidate file's full content client-side before
// a request even goes out - fine for a "did I mention this filename
// anywhere" scan, not meant to scale to huge projects.
private const val MAX_LINKED_FILES = 5
private const val MAX_LINKED_FILE_CHARS = 20_000

private suspend fun findLinkedFiles(
    context: Context,
    entries: List<CodeEntry>,
    currentEntry: CodeEntry?,
    currentFile: AiCurrentFileDto?,
): List<AiCurrentFileDto> {
    if (currentEntry == null || currentFile == null) return emptyList()
    val baseName = currentFile.path.substringAfterLast('/')
    val stem = baseName.substringBeforeLast('.', baseName)
    if (stem.length < 3) return emptyList()

    val matches = mutableListOf<AiCurrentFileDto>()
    for (other in entries) {
        if (other.isFolder || other.id == currentEntry.id) continue
        if (matches.size >= MAX_LINKED_FILES) break
        val content = try {
            CodeStorage.readText(context, Uri.parse(other.id))
        } catch (e: Exception) {
            continue
        }
        if (content.contains(baseName) || content.contains(stem)) {
            matches.add(AiCurrentFileDto(relativePath(entries, null, other), content.take(MAX_LINKED_FILE_CHARS)))
        }
    }
    return matches
}

// "folder" the first time in a given parent, "folder 2" next, and so on —
// same idea as a laptop file manager's "New Folder (2)". Scoped per-parent
// so two different folders can each have their own plain "file" untouched.
private fun uniqueSiblingName(entries: List<CodeEntry>, parentId: String?, base: String): String {
    val siblingNames = entries.filter { it.parentId == parentId }.map { it.name }
    if (base !in siblingNames) return base
    var i = 2
    while ("$base $i" in siblingNames) i++
    return "$base $i"
}

// Whichever item is currently selected in Documents decides where the "+"
// button and Paste add things: inside it if it's a folder (since only
// folders can contain anything), alongside it if it's a file, or at the
// root if nothing's selected.
private fun targetParentFor(entries: List<CodeEntry>, selectedId: String?): String? {
    val selected = entries.firstOrNull { it.id == selectedId } ?: return null
    return if (selected.isFolder) selected.id else selected.parentId
}

// Flattens the tree into an ordered (entry, depth) list for a single
// LazyColumn — a folder's children only appear here while it's expanded, so
// expanding/collapsing is just adding/removing rows, no nested scrollables.
private fun visibleRows(entries: List<CodeEntry>, expandedFolderIds: Set<String>): List<Pair<CodeEntry, Int>> {
    val result = mutableListOf<Pair<CodeEntry, Int>>()
    fun walk(parentId: String?, depth: Int) {
        entries.filter { it.parentId == parentId }.forEach { entry ->
            result.add(entry to depth)
            if (entry.isFolder && entry.id in expandedFolderIds) walk(entry.id, depth + 1)
        }
    }
    walk(null, 0)
    return result
}

// Short extension -> the exact display-language string the server's
// /code/languages returns (matched case-insensitively), so typing
// "script.py" resolves a runnable compiler without needing a picker.
//
// HTML is the one exception: it isn't a real Wandbox language (there's no
// compiler for it — HTML doesn't produce stdout, it's meant to be rendered,
// not run) so it's a client-only pseudo-language. "Running" an .html file
// never touches the server; it writes the code to a temp file and hands it
// straight to the phone's actual default browser via ACTION_VIEW.
private const val HTML_PSEUDO_LANGUAGE = "HTML"

private val EXTENSION_TO_LANGUAGE = mapOf(
    "py" to "Python",
    "js" to "JavaScript", "mjs" to "JavaScript",
    "ts" to "TypeScript",
    "java" to "Java",
    "kt" to "Kotlin", "kts" to "Kotlin",
    "c" to "C",
    "cpp" to "C++", "cc" to "C++", "cxx" to "C++",
    "cs" to "C#",
    "go" to "Go",
    "rs" to "Rust",
    "rb" to "Ruby",
    "php" to "PHP",
    "sh" to "Bash",
    "lua" to "Lua",
    "swift" to "Swift",
    "html" to HTML_PSEUDO_LANGUAGE, "htm" to HTML_PSEUDO_LANGUAGE,
)

private fun languageFromFilename(name: String): String? {
    val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return EXTENSION_TO_LANGUAGE[ext]
}

// Common entry-point filenames, checked in order — lets `run <folder>`
// (Command tab) guess which file to start a multi-file project from without
// the user having to `ad` in and name it explicitly. Falls back to "the only
// runnable file directly in this folder" when none of these match, and gives
// up (asking the user to be explicit) only when that's still ambiguous.
private val ENTRY_FILE_CANDIDATES = listOf(
    "main.py", "app.py", "main.js", "index.js", "main.ts", "index.ts",
    "main.go", "main.rs", "main.php", "main.rb", "main.lua", "main.sh",
    "main.c", "main.cpp", "main.cc", "main.cs", "program.cs", "main.java",
)

private fun detectEntryFile(entries: List<CodeEntry>, folderId: String): CodeEntry? {
    val runnable = entries.filter {
        it.parentId == folderId && !it.isFolder &&
            languageFromFilename(it.name).let { lang -> lang != null && lang != HTML_PSEUDO_LANGUAGE }
    }
    ENTRY_FILE_CANDIDATES.forEach { candidate ->
        runnable.firstOrNull { it.name.equals(candidate, ignoreCase = true) }?.let { return it }
    }
    return runnable.singleOrNull()
}

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

// Wraps a non-HTML run's stdout/stderr/exit code in a minimal page, for the
// "Browser" output destination — mirrors cedal-mobile's uploadRunOutputPreview.ts.
private fun buildRunResultHtml(result: CodeRunResult): String {
    val status = "exit ${result.exitCode ?: "?"}"
    return """
        <!doctype html><html><head><meta charset="utf-8" />
        <title>${escapeHtml(result.language)} run output</title>
        <style>
          body { background:#0b1220; color:#e5e7eb; font-family: monospace; padding: 16px; }
          h1 { font-size: 14px; color:#9ca3af; text-transform: uppercase; letter-spacing: 1px; }
          pre { white-space: pre-wrap; word-break: break-word; font-size: 13px; }
          .stderr { color:#f87171; }
          .status { margin-top:16px; color:#6b7280; font-size:12px; }
        </style></head>
        <body>
          <h1>${escapeHtml(result.language)} run output</h1>
          <pre>${escapeHtml(result.stdout)}</pre>
          ${if (result.stderr.isNotBlank()) "<pre class=\"stderr\">${escapeHtml(result.stderr)}</pre>" else ""}
          <div class="status">${escapeHtml(status)}</div>
        </body></html>
    """.trimIndent()
}

// Writes the HTML to a cache file and opens it in the user's default
// browser via a FileProvider content:// URI (raw file:// URIs shared with
// another app are blocked on modern Android). Returns an error message on
// failure, null on success.
private fun openHtmlInBrowser(context: Context, html: String): String? = try {
    val dir = File(context.cacheDir, "code_previews").apply { mkdirs() }
    val file = File(dir, "preview.html")
    file.writeText(html)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/html")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
    null
} catch (e: Exception) {
    e.message ?: "Couldn't open a browser for this file"
}

private const val ANDROID_BUILD_NOTIFICATION_ID = 1001

// Fires a system notification when a Kotlin build finishes - lets the user
// leave Code (or background the app) entirely instead of having to keep the
// screen open watching. Silently does nothing without notification
// permission (Android 13+) - never blocks on requesting it here, since the
// finished result is still fully visible next time Code is reopened either
// way (CodeRunSession keeps it around).
private fun notifyBuildFinished(context: Context, job: AndroidBuildJob) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val title = if (job.status == "done") "Your Android build is ready" else "Android build failed"
    val text = if (job.status == "done") "Tap Cedal to install it." else (job.errorMessage ?: "Something went wrong.")
    val notification = NotificationCompat.Builder(context, ANDROID_BUILD_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(ANDROID_BUILD_NOTIFICATION_ID, notification)
    com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
}

private const val BACKER_CHECK_NOTIFICATION_ID = 1003

// Same reasoning as notifyBuildFinished above, for the manual "Check Code"
// button - a Backer scan is usually just a few seconds, but if you've
// already left Code by the time it finishes, this is how you'd know.
private fun notifyBackerCheckFinished(context: Context, hasIssue: Boolean) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val title = if (hasIssue) "Backer found something" else "Backer check complete"
    val text = if (hasIssue) "Tap Cedal to see what it found." else "No obvious issues found in your code."
    val notification = NotificationCompat.Builder(context, ANDROID_BUILD_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(BACKER_CHECK_NOTIFICATION_ID, notification)
    com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
}

private val KOTLIN_ACTIVITY_BASE_CLASSES = listOf("Activity", "ComponentActivity", "AppCompatActivity")

// A Kotlin build is a real Gradle/APK build server-side, not a free snippet
// run - android-builder already supplies the AndroidManifest.xml, build.gradle
// and everything else around the user's code (see its template/ dir), so the
// only thing that's ever actually missing is a valid Activity class in the
// code itself. Checking that here, before ever calling the server, means an
// obviously-incomplete file never wastes a build (those cost real Cloud Run
// time) and the user gets told what's missing immediately instead of waiting
// minutes for a Gradle failure.
private fun kotlinIncompleteReason(code: String): String? {
    val hasActivityClass = KOTLIN_ACTIVITY_BASE_CLASSES.any { base ->
        Regex("""class\s+\w+\s*(?:<[^>]*>)?\s*:\s*$base\b""").containsMatchIn(code)
    }
    if (!hasActivityClass) {
        return "This won't build yet. Cedal already supplies the Android manifest, Gradle setup and everything else around your code (Jetpack Compose included, same as the rest of this app) — it just needs a class that extends Activity (or ComponentActivity / AppCompatActivity) to actually show a screen, for example:\n\nclass MainActivity : ComponentActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        setContent {\n            Text(\"Hello\")\n        }\n    }\n}"
    }
    if (!Regex("""\bonCreate\s*\(""").containsMatchIn(code)) {
        return "This won't build yet — your Activity class needs an onCreate() override, that's where the screen actually gets set up. Cedal handles the manifest and Gradle project for you; you just need this one method."
    }
    return null
}

internal val apkDownloadClient = OkHttpClient()

// Downloads a finished Android build's APK to cacheDir/apk_builds and hands
// it to Android's own install-confirmation dialog via a FileProvider
// content:// URI. The user still needs to have granted Cedal "Install
// unknown apps" once via system settings before that dialog actually shows -
// this alone doesn't request or skip that; Android's own installer surfaces
// its own prompt to go grant it if it's still missing. Returns an error
// message on failure, null on success. internal (not private) so ARC Ops'
// real practice-app downloads (see ArcOpsScreen.kt) can reuse this instead
// of duplicating the same file/FileProvider/intent plumbing.
internal suspend fun downloadAndInstallApk(context: Context, downloadUrl: String, jobId: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "apk_builds").apply { mkdirs() }
            val file = File(dir, "$jobId.apk")
            val response = apkDownloadClient.newCall(Request.Builder().url(downloadUrl).build()).execute()
            response.use {
                if (!it.isSuccessful) return@withContext "Download failed (HTTP ${it.code})"
                val body = it.body ?: return@withContext "Empty download response"
                body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            e.message ?: "Couldn't download/install this build"
        }
    }

@Composable
internal fun MemberCodeBody(
    onBack: () -> Unit,
    highlightId: String? = null,
    onOpenGuiSession: (String, String, List<String>) -> Unit = { _, _, _ -> },
    viewModel: AuthViewModel = hiltViewModel(),
    // Lets a different host (DeveloperScaffold's own swell-animated bottom
    // bar, promoting Pad/Documents/View to top-level Code/Explorer/View
    // tabs - see that file's doc comment) drive which sub-tab is showing
    // and hide this composable's own CodeBottomBar, instead of duplicating
    // all the state/logic above just to reskin the tab chrome. Null
    // (the default) preserves the exact original behavior for the normal
    // member Code tab - untouched.
    externalActiveTab: CodeTab? = null,
    onActiveTabChange: ((CodeTab) -> Unit)? = null,
    showOwnBottomBar: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Requested contextually right before a Kotlin build starts (not eagerly
    // on screen open) - Android 13+ only, older versions don't need it at
    // all. Declining just means notifyBuildFinished silently does nothing;
    // the result is still there in View when you check back manually.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // Documents first — naming/creating/organizing lives there now, so
    // that's the natural landing spot; Pad is only reached once you've
    // actually entered something.
    var internalActiveTab by remember { mutableStateOf(if (highlightId != null) CodeTab.RULES else CodeTab.DOCUMENTS) }
    val activeTab = externalActiveTab ?: internalActiveTab
    fun setActiveTab(tab: CodeTab) {
        internalActiveTab = tab
        onActiveTabChange?.invoke(tab)
    }
    // Clears the Work Mode Selector's red-dot the moment you actually look at
    // View - not just on opening Code, since you might come straight back to
    // Documents/Pad without checking the result.
    LaunchedEffect(activeTab) {
        if (activeTab == CodeTab.VIEW) CodeRunSession.markBuildResultSeen()
    }

    // First-time walkthrough (auto-shown once, persisted via
    // SecureStorage.codeTutorialSeen) plus a static help page reachable
    // anytime through the "?" button — see CodeWalkthroughOverlay/
    // CodeHelpOverlay below.
    var showWalkthrough by remember { mutableStateOf(!viewModel.storage.codeTutorialSeen) }
    var showHelpPage by remember { mutableStateOf(false) }
    fun dismissWalkthrough() {
        showWalkthrough = false
        viewModel.storage.codeTutorialSeen = true
    }

    // Real, persistent storage: everything below is a mirror of an actual
    // folder on the phone (picked once via Storage Access Framework), not
    // in-memory state — so backgrounding, the back button, anything, never
    // loses a single file. See CodeStorage for the SAF plumbing.
    var codeFolderUri by remember { mutableStateOf(viewModel.storage.codeFolderUri) }
    var rootDoc by remember { mutableStateOf<DocumentFile?>(null) }
    var folderDocs by remember { mutableStateOf<Map<String, DocumentFile>>(emptyMap()) }
    var entries by remember { mutableStateOf<List<CodeEntry>>(emptyList()) }
    var loadingTree by remember { mutableStateOf(false) }
    var treeError by remember { mutableStateOf<String?>(null) }

    // Scopes what the Code AI (Rules tab) can see/edit to one file or folder
    // instead of the whole project — set via Command's "ai see <name>" or
    // the AI tab's own scope picker. null means unrestricted (today's
    // original heuristic: whole project's paths + whatever's open in Pad +
    // a few heuristically linked files — see CodeRulesBody's send()).
    // aiScopeId is the boundary itself (a file scopes to exactly that file;
    // a folder scopes to everything under it); aiScopeViewId only matters
    // when aiScopeId is a folder — which one file inside it is the
    // "currentFile" sent with full content, the rest ride along as
    // linkedFiles. Shown near the "?" button (AiScopeBanner) so it's
    // visible from every Code tab, not just while on the AI tab.
    var aiScopeId by remember { mutableStateOf<String?>(null) }
    var aiScopeViewId by remember { mutableStateOf<String?>(null) }
    val aiScopeEntry = entries.firstOrNull { it.id == aiScopeId }
    // Dropped the moment its target vanishes (file deleted/renamed under it)
    // rather than silently pointing at a stale id.
    LaunchedEffect(entries) {
        if (aiScopeId != null && aiScopeEntry == null) { aiScopeId = null; aiScopeViewId = null }
    }
    fun setAiScopeToFile(entry: CodeEntry) {
        aiScopeId = entry.id
        aiScopeViewId = entry.id
    }
    fun setAiScopeToFolder(entry: CodeEntry) {
        aiScopeId = entry.id
        aiScopeViewId = (detectEntryFile(entries, entry.id) ?: filesUnder(entries, entry.id).firstOrNull())?.id
    }
    fun clearAiScope() { aiScopeId = null; aiScopeViewId = null }
    // Command's `ai see <name>` resolves against the active tab's current
    // location, same lookup convention as ad/run — a file match wins over a
    // same-named folder (the more common/specific target).
    fun resolveAiSee(locationId: String?, argument: String): CodeEntry? {
        val trimmed = argument.trim()
        if (trimmed.isEmpty()) return null
        return entries.firstOrNull { it.parentId == locationId && !it.isFolder && it.name.equals(trimmed, ignoreCase = true) }
            ?: entries.firstOrNull { it.parentId == locationId && it.isFolder && it.name.equals(trimmed, ignoreCase = true) }
    }

    suspend fun refreshTree() {
        val root = rootDoc ?: return
        val treeResult = CodeStorage.listTree(root)
        entries = treeResult.nodes.map { CodeEntry(it.id, it.parentId, it.name, it.isFolder) }
        folderDocs = treeResult.folderDocs
    }

    LaunchedEffect(codeFolderUri) {
        val uriString = codeFolderUri
        if (uriString != null) {
            loadingTree = true
            treeError = null
            val root = CodeStorage.ensureRoot(context, uriString)
            if (root != null) {
                rootDoc = root
                refreshTree()
            } else {
                treeError = "Couldn't access that folder — choose it again."
                rootDoc = null
            }
            loadingTree = false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val uriString = uri.toString()
            viewModel.storage.codeFolderUri = uriString
            codeFolderUri = uriString
        }
    }

    fun resolveParentDoc(parentId: String?): DocumentFile? = if (parentId == null) rootDoc else folderDocs[parentId]

    // Walks a slash-separated path like "utils/helper.py" (as given by an
    // AI file action) against the real tree, case-insensitively per
    // segment - used to resolve editFile/deleteFile/runFile targets.
    fun resolveEntryByPath(path: String): CodeEntry? {
        val segments = path.trim('/').split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return null
        var parentId: String? = null
        var found: CodeEntry? = null
        for ((index, segment) in segments.withIndex()) {
            val isLast = index == segments.lastIndex
            val match = entries.firstOrNull { it.parentId == parentId && it.name.equals(segment, ignoreCase = true) && (isLast || it.isFolder) } ?: return null
            found = match
            parentId = match.id
        }
        return found
    }

    // Same idea for createFile/editFile targets whose parent folders may
    // not exist yet - creates any missing intermediate folders as real SAF
    // directories, same as Documents' own "create" flow would. Once one
    // segment doesn't exist in `entries`, every segment after it can't
    // exist either (its parent didn't exist a moment ago), so this never
    // needs to re-scan the disk mid-walk.
    suspend fun resolveOrCreateParentDoc(path: String): Triple<DocumentFile, String?, String>? {
        val root = rootDoc ?: return null
        val segments = path.trim('/').split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return null
        var currentDoc = root
        var currentParentId: String? = null
        for (index in 0 until segments.size - 1) {
            val segment = segments[index]
            val existing = entries.firstOrNull { it.parentId == currentParentId && it.isFolder && it.name.equals(segment, ignoreCase = true) }
            if (existing != null) {
                currentDoc = folderDocs[existing.id] ?: return null
                currentParentId = existing.id
            } else {
                val created = CodeStorage.createFolder(currentDoc, segment) ?: return null
                currentDoc = created
                currentParentId = created.uri.toString()
            }
        }
        return Triple(currentDoc, currentParentId, segments.last())
    }

    // Documents <-> the user's OWN GitHub repo - see CodeGithubSyncService
    // server-side. State lives here (not inside CodeDocumentsBody) since
    // the OAuth deep-link handoff below needs to reach it regardless of
    // which sub-tab is active when the browser returns.
    var githubStatus by remember { mutableStateOf<GithubStatusDto?>(null) }
    var githubRepos by remember { mutableStateOf<List<GithubRepoDto>>(emptyList()) }
    var githubRepoPickerOpen by remember { mutableStateOf(false) }
    var githubSyncing by remember { mutableStateOf(false) }
    var githubSyncError by remember { mutableStateOf<String?>(null) }
    var githubSyncNotice by remember { mutableStateOf<String?>(null) }
    var githubConflicts by remember { mutableStateOf<List<SyncConflictDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.getGithubStatus().onSuccess { githubStatus = it }
    }

    // Completes the OAuth flow once MainActivity.onNewIntent catches the
    // browser's redirect back into the app - see CodeGithubOAuthState's own
    // doc comment for why this is a plain ambient object, not a nav arg.
    LaunchedEffect(CodeGithubOAuthState.pendingResult) {
        val result = CodeGithubOAuthState.pendingResult ?: return@LaunchedEffect
        CodeGithubOAuthState.pendingResult = null
        setActiveTab(CodeTab.DOCUMENTS)
        when (result) {
            is CodeGithubOAuthResult.Success -> {
                viewModel.getGithubStatus().onSuccess { githubStatus = it }
                githubSyncNotice = "GitHub connected."
            }
            is CodeGithubOAuthResult.Failure -> {
                githubSyncError = "GitHub connection failed" + (result.reason?.let { " ($it)" } ?: "")
            }
        }
    }

    fun connectGithub() {
        scope.launch {
            githubSyncError = null
            viewModel.getGithubAuthorizeUrl()
                .onSuccess { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { githubSyncError = it.message }
        }
    }

    fun openGithubRepoPicker() {
        scope.launch {
            githubSyncError = null
            viewModel.listGithubRepos()
                .onSuccess { repos -> githubRepos = repos; githubRepoPickerOpen = true }
                .onFailure { githubSyncError = it.message }
        }
    }

    fun selectGithubRepo(repo: GithubRepoDto) {
        scope.launch {
            viewModel.selectGithubRepo(repo.owner, repo.name, repo.defaultBranch)
                .onSuccess { githubStatus = it; githubRepoPickerOpen = false; githubConflicts = emptyList() }
                .onFailure { githubSyncError = it.message }
        }
    }

    fun disconnectGithub() {
        scope.launch {
            viewModel.disconnectGithub()
            githubStatus = GithubStatusDto(connected = false)
            githubConflicts = emptyList()
        }
    }

    // Whole-folder push+pull - walks the already-loaded `entries` tree,
    // skips binaries (CodeStorage.readText/writeText are UTF-8 text only -
    // see that object's own doc comment), starts a server-side job (a real
    // sync is many sequential GitHub API calls - too slow for one blocking
    // request) and polls it, then applies the result locally.
    fun runGithubSync() {
        if (githubSyncing) return
        scope.launch {
            githubSyncing = true
            githubSyncError = null
            githubSyncNotice = null
            try {
                var skippedBinaries = 0
                val filesToSync = mutableListOf<CodeSyncFileEntry>()
                for (entry in entries.filter { !it.isFolder }) {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(Uri.parse(entry.id))?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    } ?: continue
                    val sampleLen = minOf(bytes.size, 8192)
                    val looksBinary = (0 until sampleLen).any { bytes[it] == 0.toByte() }
                    if (looksBinary) {
                        skippedBinaries++
                        continue
                    }
                    filesToSync.add(CodeSyncFileEntry(relativePath(entries, null, entry), String(bytes, Charsets.UTF_8)))
                }

                val jobId = viewModel.startGithubSync(filesToSync).getOrElse {
                    githubSyncError = it.message ?: "Couldn't start sync"
                    return@launch
                }

                var finished: SyncJobDto? = null
                while (true) {
                    delay(1500)
                    val job = viewModel.getGithubSyncJob(jobId).getOrElse {
                        githubSyncError = it.message ?: "Sync failed"
                        return@launch
                    }
                    if (job.status != "running") {
                        finished = job
                        break
                    }
                }
                val job = finished ?: return@launch
                if (job.status == "error") {
                    githubSyncError = job.errorMessage ?: "Sync failed"
                    return@launch
                }

                for (pulled in job.pulled) {
                    val resolved = resolveOrCreateParentDoc(pulled.path) ?: continue
                    val (parentDoc, parentId, fileName) = resolved
                    val existingUri = entries.firstOrNull { it.parentId == parentId && it.name.equals(fileName, ignoreCase = true) }?.id
                    val uri = existingUri?.let { Uri.parse(it) } ?: CodeStorage.createFile(parentDoc, fileName)?.uri
                    if (uri != null) CodeStorage.writeText(context, uri, pulled.content)
                }
                for (path in job.deletedLocal) {
                    resolveEntryByPath(path)?.let { CodeStorage.delete(context, Uri.parse(it.id)) }
                }
                refreshTree()

                githubConflicts = job.conflicts
                val summary = mutableListOf<String>()
                if (job.pushed.isNotEmpty()) summary.add("${job.pushed.size} pushed")
                if (job.pulled.isNotEmpty()) summary.add("${job.pulled.size} pulled")
                val deletedCount = job.deletedRemote.size + job.deletedLocal.size
                if (deletedCount > 0) summary.add("$deletedCount deleted")
                if (job.conflicts.isNotEmpty()) summary.add("${job.conflicts.size} conflicts")
                if (skippedBinaries > 0) summary.add("$skippedBinaries binary files skipped")
                githubSyncNotice = if (summary.isEmpty()) "Already up to date." else summary.joinToString(", ")
            } finally {
                githubSyncing = false
            }
        }
    }

    // "Keep local" / "Keep GitHub" for one conflicted file - a dedicated
    // server call rather than just re-running runGithubSync, since a normal
    // sync compares against the last-synced fingerprint, which is still the
    // stale pre-conflict one right after picking a side - see
    // CodeGithubSyncService.resolveConflict's own doc comment.
    fun resolveGithubConflict(conflict: SyncConflictDto, keepLocal: Boolean) {
        scope.launch {
            viewModel.resolveGithubConflict(conflict.path, keepLocal, conflict.localContent)
                .onSuccess { resolved ->
                    val target = resolveOrCreateParentDoc(resolved.path)
                    if (target != null) {
                        val (parentDoc, parentId, fileName) = target
                        val existingUri = entries.firstOrNull { it.parentId == parentId && it.name.equals(fileName, ignoreCase = true) }?.id
                        val uri = existingUri?.let { Uri.parse(it) } ?: CodeStorage.createFile(parentDoc, fileName)?.uri
                        if (uri != null) CodeStorage.writeText(context, uri, resolved.content)
                    }
                    refreshTree()
                    githubConflicts = githubConflicts.filterNot { it.path == conflict.path }
                }
                .onFailure { githubSyncError = it.message }
        }
    }

    var enteredId by remember { mutableStateOf(viewModel.storage.lastEnteredCodeId) }
    val enteredEntry = entries.firstOrNull { it.id == enteredId }
    var enteredCode by remember { mutableStateOf("") }
    var lastPersistedCode by remember { mutableStateOf("") }
    var guiSessionStarting by remember { mutableStateOf(false) }
    var guiSessionError by remember { mutableStateOf<String?>(null) }
    // Explicit opt-in, comma-separated - most common packages are already
    // pre-baked into the runner image (numpy/pandas/requests/pillow for
    // Python, lodash/axios/dayjs for JS, httparty for Ruby, guzzlehttp/
    // guzzle for PHP) and need nothing here; this is only for anything not
    // already covered, and costs real install time when used.
    var packagesInput by remember { mutableStateOf("") }
    val packagesList = packagesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun enterId(id: String) {
        enteredId = id
        viewModel.storage.lastEnteredCodeId = id
    }

    // Real undo/redo of edits within the currently-open file - ◀/▶ used to be
    // browser-style navigation between different files you'd opened, but
    // that's not what anyone actually reached for them expecting; this is.
    // Every onCodeChange pushes the PREVIOUS value before applying the new
    // one, so one tap of Undo reverts exactly one onValueChange's worth of
    // change (one keystroke if typed normally, one whole paste if pasted in
    // one go) - and starting a fresh edit after undoing clears redo, same as
    // every other undo/redo implementation.
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    fun updateCode(newValue: String) {
        if (newValue != enteredCode) {
            undoStack.add(enteredCode)
            if (undoStack.size > 500) undoStack.removeAt(0)
            redoStack.clear()
        }
        enteredCode = newValue
    }
    fun undoEdit() {
        if (undoStack.isEmpty()) return
        redoStack.add(enteredCode)
        enteredCode = undoStack.removeAt(undoStack.size - 1)
    }
    fun redoEdit() {
        if (redoStack.isEmpty()) return
        undoStack.add(enteredCode)
        enteredCode = redoStack.removeAt(redoStack.size - 1)
    }

    // Loads a freshly-entered file's real content from disk - also resets
    // undo/redo, since neither should ever reach back into a different file.
    LaunchedEffect(enteredId, rootDoc) {
        val entry = entries.firstOrNull { it.id == enteredId }
        val loaded = if (entry != null && !entry.isFolder) CodeStorage.readText(context, Uri.parse(entry.id)) else ""
        enteredCode = loaded
        lastPersistedCode = loaded
        undoStack.clear()
        redoStack.clear()
    }
    // "Call Out" (Settings > Corneal AI, text-based) - same off-by-default
    // privacy pattern as Bot View/CornealBubbleState.currentChatContext: off
    // (default), this never touches currentCodeContext at all, so there's no
    // code path where this file's content leaves this screen.
    LaunchedEffect(enteredId, enteredCode) {
        com.xhacker.cedal.ui.CornealBubbleState.currentCodeContext = if (viewModel.storage.callOutTextEnabled && enteredEntry != null && !enteredEntry.isFolder) {
            com.xhacker.cedal.ui.CodeContext(path = enteredEntry.name, content = enteredCode)
        } else {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose { com.xhacker.cedal.ui.CornealBubbleState.currentCodeContext = null }
    }
    // Actually persists enteredCode to disk right now, if it's ahead of what
    // was last written - shared by the debounce below and the two "flush
    // immediately" hooks further down, since a rebuild/reinstall typically
    // force-kills the app process outright rather than backgrounding it
    // normally, so anything still only sitting in the debounce's queue at
    // that instant never gets a chance to actually write.
    suspend fun flushPendingSave() {
        if (enteredCode == lastPersistedCode) return
        val entry = entries.firstOrNull { it.id == enteredId } ?: return
        if (entry.isFolder) return
        CodeStorage.writeText(context, Uri.parse(entry.id), enteredCode)
        lastPersistedCode = enteredCode
    }

    // Debounce-writes edits back to that same real file while you're still
    // actively typing. Comparing against lastPersistedCode (not just "has
    // enteredCode changed") means the load above never triggers a redundant
    // write of its own content.
    LaunchedEffect(enteredCode) {
        if (enteredCode != lastPersistedCode) {
            delay(350)
            flushPendingSave()
        }
    }

    // Catches the common real case a plain debounce misses: you stop typing,
    // switch away (another Code tab, or the whole app to the background),
    // *then* a rebuild happens - by then the debounce above would already
    // have fired on its own, but this guarantees it regardless of timing.
    LaunchedEffect(activeTab) {
        if (activeTab != CodeTab.PAD) flushPendingSave()
    }
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) scope.launch { flushPendingSave() }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    var justSaved by remember { mutableStateOf(false) }
    fun saveNow() {
        val entry = enteredEntry ?: return
        if (entry.isFolder) return
        scope.launch {
            flushPendingSave()
            justSaved = true
            delay(1500)
            justSaved = false
        }
    }

    // Whatever Command's active tab is currently navigated to — Documents'
    // + button and Paste default to this whenever nothing's explicitly
    // selected there, so moving around in Command actually changes where
    // new things land in Documents, not just a cosmetic prompt. No upfront
    // "pick a root" step: a fresh tab just starts at the top, same as
    // Documents itself does when nothing's selected.
    var activeCommandLocationId by remember { mutableStateOf<String?>(null) }

    // Command's tabs/transcript/in-progress input, hoisted up here (not
    // local to CodeCommandBody) — that composable only exists in the
    // composition tree while activeTab == COMMAND, so anything remembered
    // inside it gets thrown away the instant you switch tabs and come back.
    var commandTabs by remember { mutableStateOf(listOf(CommandTab(id = "1", label = "1"))) }
    var activeCommandTabIndex by remember { mutableStateOf(0) }
    var nextCommandTabNumber by remember { mutableStateOf(2) }
    var commandInputText by remember { mutableStateOf("") }

    // Pure navigation among folders that already exist — "ad" never creates
    // anything, so this is a plain synchronous lookup against the current
    // tree, not a suspend call.
    fun resolveAd(locationId: String?, argument: String): AdResult {
        val trimmed = argument.trim()
        if (trimmed.isEmpty()) return AdResult(locationId, null)
        if (trimmed == "..") {
            val current = entries.firstOrNull { it.id == locationId }
            return if (current?.parentId == null) {
                AdResult(locationId, "Already at the top.")
            } else {
                AdResult(current.parentId, null)
            }
        }
        val target = entries.firstOrNull { it.parentId == locationId && it.isFolder && it.name.equals(trimmed, ignoreCase = true) }
        return if (target != null) AdResult(target.id, null) else AdResult(locationId, "No folder named \"$trimmed\" here.")
    }

    var languages by remember { mutableStateOf<List<CodeLanguageItem>>(emptyList()) }
    var stdin by remember { mutableStateOf("") }
    // These live in CodeRunSession (not remember) so switching away from Code
    // and back doesn't silently throw away what you last ran and its result.
    var result by CodeRunSession::result
    var running by CodeRunSession::running
    var loadingLanguages by remember { mutableStateOf(true) }
    var languagesError by remember { mutableStateOf<String?>(null) }
    var runError by CodeRunSession::runError
    // HTML "runs" never touch the server, so there's no CodeRunResult for
    // View to show — this is what tells View "it worked" for that case.
    var viewMessage by CodeRunSession::viewMessage

    // Kotlin doesn't run as a snippet at all — "Run" instead builds a real
    // installable APK via android-builder (see AndroidBuildService server-side)
    // and this tracks that job's progress instead of a CodeRunResult.
    var androidBuildJob by CodeRunSession::androidBuildJob
    var androidBuildError by CodeRunSession::androidBuildError
    var installing by CodeRunSession::installing
    var installError by CodeRunSession::installError

    // Where a run's output shows up: "webview" (in-app WebView for HTML /
    // View tab for everything else, the default) or "browser" (opens the
    // system browser instead). Every language except Kotlin respects this.
    var outputDestination by remember { mutableStateOf(viewModel.storage.codeOutputDestination) }
    var htmlPreviewHtml by remember { mutableStateOf<String?>(null) }

    fun setOutputDestination(dest: String) {
        outputDestination = dest
        viewModel.storage.codeOutputDestination = dest
    }

    suspend fun loadLanguages() {
        loadingLanguages = true
        languagesError = null
        viewModel.listCodeLanguages()
            .onSuccess { languages = it }
            .onFailure { languagesError = it.message ?: "Couldn't reach the server" }
        loadingLanguages = false
    }

    LaunchedEffect(Unit) { loadLanguages() }

    val resolvedLanguage = enteredEntry?.takeIf { !it.isFolder }?.let { languageFromFilename(it.name) }
    val compilerSlug = languages.firstOrNull { it.language.equals(resolvedLanguage, ignoreCase = true) }?.version

    var backerChecking by CodeRunSession::backerChecking
    var backerReason by CodeRunSession::backerReason
    var backerFixedCode by CodeRunSession::backerFixedCode

    // The actual Kotlin build request / interpreted run - split out from
    // runCode() so Backer's pre-flight check (below) can sit in front of it
    // without duplicating this logic for its "run anyway"/"apply fix and
    // run" outcomes.
    fun proceedWithRun() {
        if (resolvedLanguage == "Kotlin") {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            running = true
            scope.launch {
                viewModel.requestAndroidBuild(enteredCode)
                    .onSuccess { job ->
                        androidBuildJob = job
                        CodeRunSession.startWatchingBuild(
                            jobId = job.jobId,
                            poll = { id -> viewModel.getAndroidBuildJob(id) },
                            onFinished = { finishedJob -> notifyBuildFinished(context, finishedJob) },
                        )
                    }
                    .onFailure { androidBuildError = it.message; running = false }
            }
            return
        }
        // Run is the plain sandboxed runner - no display server, so tkinter
        // (and any other GUI toolkit needing a real X11 display) can never
        // work there. "Open Live View" is the actual path for this (a real
        // virtual display with tkinter preinstalled - see openLiveView()
        // below) - catch the obvious case here instead of letting it fail
        // with a confusing "No module named tkinter" first.
        if (resolvedLanguage == "Python" && (enteredCode.contains("import tkinter") || enteredCode.contains("from tkinter"))) {
            runError = "This looks like a Tkinter GUI app - Run is a headless sandbox with no display, so a Tkinter window can't open there. Use \"Open Live View\" instead (below) - same code, but in a real virtual display with Tkinter already installed."
            return
        }
        val slug = compilerSlug ?: run { runError = "No matching language found for this file."; return }
        running = true
        scope.launch {
            viewModel.runCode(slug, enteredCode, stdin, packages = packagesList)
                .onSuccess { runResult ->
                    if (outputDestination == "browser") {
                        val error = openHtmlInBrowser(context, buildRunResultHtml(runResult))
                        if (error != null) runError = error else viewMessage = "Opened in your browser."
                    } else {
                        result = runResult
                    }
                }
                .onFailure { runError = it.message }
            running = false
        }
    }

    // "Apply Fix" - only ever happens on this explicit tap, never silently.
    // Immediately continues to the real run with the fixed code, matching
    // what Run was already asking for - re-tapping Run again would just be
    // annoying busywork after you've already said yes once.
    fun applyBackerFixAndRun() {
        val fixed = backerFixedCode
        CodeRunSession.clearBackerIssue()
        if (!fixed.isNullOrBlank()) enteredCode = fixed
        proceedWithRun()
    }

    // "Run Anyway" - Backer's opinion isn't the final word; a false
    // positive (or a risk the user's fine with) shouldn't trap them.
    fun runAnywayIgnoringBacker() {
        CodeRunSession.clearBackerIssue()
        proceedWithRun()
    }

    fun runCode() {
        if (enteredEntry == null || enteredEntry.isFolder) return
        // Every run attempt — success, failure, or the HTML no-server path —
        // jumps straight to View so the outcome is always visible without
        // an extra tap.
        setActiveTab(CodeTab.VIEW)
        runError = null
        viewMessage = null
        result = null
        androidBuildJob = null
        androidBuildError = null
        installError = null
        // A fresh Run attempt should never inherit a stale "running" flag or
        // a zombie watch coroutine left behind by an earlier attempt -
        // without this, the Kotlin incomplete-code error below (or the HTML
        // no-server path) could set an error while an old in-flight spinner
        // was still showing (and might even flip back on its own later),
        // rendering both at once. Real async paths set running back to true
        // themselves the moment they actually start. androidBuildJob is
        // already null above, so this has no other side effect here.
        CodeRunSession.cancelWatching()
        CodeRunSession.clearBackerIssue()
        if (resolvedLanguage == HTML_PSEUDO_LANGUAGE) {
            if (outputDestination == "browser") {
                val error = openHtmlInBrowser(context, enteredCode)
                if (error != null) runError = error else viewMessage = "Opened in your browser."
            } else {
                htmlPreviewHtml = enteredCode
            }
            return
        }
        if (resolvedLanguage == "Kotlin") {
            val incompleteReason = kotlinIncompleteReason(enteredCode)
            if (incompleteReason != null) {
                androidBuildError = incompleteReason
                return
            }
        }
        // "Backer" - an automatic pre-flight AI check before every real run,
        // across every language (see CodeBackerService server-side). Runs
        // after the free/instant Kotlin structural gate above, since that
        // one can already reject obviously-incomplete code for free - no
        // point spending an AI call on something that was never going to
        // build anyway. If Backer itself fails outright (network/AI
        // outage), proceeds to the real run same as if it weren't here -
        // it should never make Run less reliable than before it existed.
        backerChecking = true
        scope.launch {
            val review = viewModel.backerReview(resolvedLanguage ?: "unknown", enteredCode)
            backerChecking = false
            val issue = review.getOrNull()
            if (issue?.hasIssue == true) {
                backerReason = issue.reason
                backerFixedCode = issue.fixedCode.takeIf { it.isNotBlank() }
            } else {
                proceedWithRun()
            }
        }
    }

    // "Open Live View" - Python only, a deliberate opt-in (not auto-
    // triggered off code content) since starting a gui-runner session costs
    // real Cloud Run time and has a few seconds of cold-start latency. See
    // GuiSessionService server-side - each account can only have one such
    // session open at a time, so a busy-session error here is a real,
    // expected outcome (from this same account), not a bug.
    fun openLiveView() {
        if (enteredEntry == null || enteredEntry.isFolder || resolvedLanguage != "Python") return
        guiSessionError = null
        guiSessionStarting = true
        scope.launch {
            viewModel.startGuiSession(enteredCode)
                .onSuccess { job ->
                    val url = job.viewUrl
                    if (url != null) onOpenGuiSession(job.sessionId, url, job.affinityCookies) else guiSessionError = "Session started but no view URL came back"
                }
                .onFailure { guiSessionError = it.message ?: "Couldn't start the live session" }
            guiSessionStarting = false
        }
    }

    // The manual "Check Code" button - Backer on demand, without running or
    // building afterward either way. Unlike runCode()'s own automatic check
    // (a plain scope.launch, cancelled the instant you leave Code), this
    // runs on CodeRunSession's own persistent scope and fires a real
    // notification if it finishes after you've navigated away - same
    // reasoning as Kotlin build notifications, just for a much shorter wait.
    fun checkCodeManually() {
        if (enteredEntry == null || enteredEntry.isFolder || resolvedLanguage == null) return
        setActiveTab(CodeTab.VIEW)
        runError = null
        viewMessage = null
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val language = resolvedLanguage
        val code = enteredCode
        CodeRunSession.startManualBackerCheck(
            review = {
                val response = viewModel.backerReview(language, code).getOrNull()
                CodeRunSession.BackerCheckOutcome(
                    hasIssue = response?.hasIssue == true,
                    reason = response?.reason ?: "",
                    fixedCode = response?.fixedCode ?: "",
                )
            },
            onFinished = { hasIssue ->
                if (!hasIssue) {
                    CodeRunSession.viewMessage = "Backer didn't find anything obviously wrong."
                }
                notifyBackerCheckFinished(context, hasIssue)
            },
        )
    }

    // Command's "run <file>" — unlike Pad's Run, this treats the whole
    // current ad-navigated folder as a project: every other file in it
    // (recursive) rides along as supporting code via Wandbox's real "codes"
    // mechanism (or Kotlin Playground's multi-file "files" list), so local
    // imports between files in that folder actually resolve. Output prints
    // straight into that Command tab's own transcript, terminal-style —
    // it never jumps to View, since typing a run command in a real terminal
    // shows its output right there, not in a separate window.
    // Also accepts a subfolder name instead of a file — "run <folder>" runs
    // every file under that folder as its own project, auto-picking the
    // entry file (see detectEntryFile), so folders that only work as a
    // whole don't need an `ad` first.
    fun runProject(tabId: String, folderId: String?, argument: String) {
        fun appendToTab(line: TerminalLine) {
            commandTabs = commandTabs.map { tab -> if (tab.id == tabId) tab.copy(transcript = tab.transcript + line) else tab }
        }
        // Try a direct file in the current folder first (original "run
        // <filename>" behavior) - only if that fails does the argument get
        // treated as a subfolder name instead ("run <folder>" runs every
        // file under it as one project, auto-picking the entry file so the
        // user doesn't have to `ad` in and name it explicitly first).
        val directFileMatch = entries.firstOrNull { it.parentId == folderId && !it.isFolder && it.name.equals(argument, ignoreCase = true) }
        val targetFolderId: String?
        val entryEntry: CodeEntry
        if (directFileMatch != null) {
            targetFolderId = folderId
            entryEntry = directFileMatch
        } else {
            val folderMatch = entries.firstOrNull { it.parentId == folderId && it.isFolder && it.name.equals(argument, ignoreCase = true) }
            if (folderMatch == null) {
                appendToTab(TerminalLine("No file or folder named \"$argument\" here.", isError = true))
                return
            }
            val autoEntry = detectEntryFile(entries, folderMatch.id)
            if (autoEntry == null) {
                val candidates = entries.filter { it.parentId == folderMatch.id && !it.isFolder }.map { it.name }
                appendToTab(
                    TerminalLine(
                        if (candidates.isEmpty()) {
                            "\"$argument\" has no runnable files directly inside it."
                        } else {
                            "Couldn't tell which file to start with in \"$argument\" — try: ad $argument, then run <one of: ${candidates.joinToString(", ")}>"
                        },
                        isError = true,
                    ),
                )
                return
            }
            targetFolderId = folderMatch.id
            entryEntry = autoEntry
        }
        val language = languageFromFilename(entryEntry.name)
        if (language == null || language == HTML_PSEUDO_LANGUAGE) {
            appendToTab(TerminalLine("Can't run this file type as a project — open it in Pad instead.", isError = true))
            return
        }
        val slug = languages.firstOrNull { it.language.equals(language, ignoreCase = true) }?.version
        if (slug == null) {
            appendToTab(TerminalLine("No matching language found for this file.", isError = true))
            return
        }
        scope.launch {
            val entryCode = CodeStorage.readText(context, Uri.parse(entryEntry.id))
            val supportFiles = filesUnder(entries, targetFolderId)
                .filter { it.id != entryEntry.id }
                .map { CodeFile(name = relativePath(entries, targetFolderId, it), content = CodeStorage.readText(context, Uri.parse(it.id))) }
            viewModel.runCode(slug, entryCode, stdin, supportFiles)
                .onSuccess { runResult ->
                    if (runResult.stdout.isNotBlank()) appendToTab(TerminalLine(runResult.stdout.trimEnd('\n')))
                    if (runResult.stderr.isNotBlank()) appendToTab(TerminalLine(runResult.stderr.trimEnd('\n'), isError = true))
                    if (!runResult.compileOutput.isNullOrBlank()) appendToTab(TerminalLine(runResult.compileOutput.trimEnd('\n'), isError = true))
                    if (runResult.stdout.isBlank() && runResult.stderr.isBlank() && runResult.compileOutput.isNullOrBlank()) {
                        appendToTab(TerminalLine("(no output)"))
                    }
                }
                .onFailure { appendToTab(TerminalLine(it.message ?: "Run failed", isError = true)) }
        }
    }

    // What lets the Code AI chat actually create/edit/delete/run files, not
    // just talk about them - reuses the exact same CodeStorage calls
    // Documents' own buttons already use (see onCreate/onDelete above),
    // scoped by construction to the one CedalCode SAF folder rootDoc points
    // at. The server only ever returns a plan (see AiChangeRequestService);
    // this is what actually executes it, since the server has no access to
    // this folder at all.
    // Resolves a destinationFolder path (e.g. "archive/2024", "" for the top
    // level) to a real, existing-or-freshly-created folder id - reuses
    // resolveOrCreateParentDoc's own "walk segments, create any missing
    // intermediate folders" logic by tacking on a throwaway final segment
    // and reading back which folder it would have landed in.
    suspend fun resolveOrCreateFolderId(path: String): String? {
        if (path.isBlank()) return null
        val resolved = resolveOrCreateParentDoc("$path/__placeholder__") ?: return null
        return resolved.second
    }

    // Runs a single AI file action against the on-device Code area,
    // returning a short human-readable result line. Shared by
    // executeAiFileActions (the AI path) below.
    suspend fun executeSingleFileAction(action: AiFileActionDto): String {
        when (action.action) {
            "createFile" -> {
                val resolved = resolveOrCreateParentDoc(action.path)
                    ?: return "Couldn't create that file - the folder path didn't resolve."
                val (parentDoc, parentId, rawName) = resolved
                val finalName = uniqueSiblingName(entries, parentId, rawName)
                val created = CodeStorage.createFile(parentDoc, finalName)
                return if (created != null) {
                    CodeStorage.writeText(context, created.uri, action.content.orEmpty())
                    refreshTree()
                    viewModel.triggerAchievementFireAndForget("first_code")
                    "Created ${action.path}."
                } else {
                    "Couldn't create ${action.path}."
                }
            }
            "createFolder" -> {
                val resolved = resolveOrCreateParentDoc(action.path)
                    ?: return "Couldn't create that folder - the path didn't resolve."
                val (parentDoc, _, rawName) = resolved
                val created = CodeStorage.createFolder(parentDoc, rawName)
                refreshTree()
                return if (created != null) "Created folder ${action.path}." else "Couldn't create folder ${action.path}."
            }
            "editFile" -> {
                val entry = resolveEntryByPath(action.path)
                if (entry == null || entry.isFolder) return "Couldn't find a file at ${action.path} to edit."
                val content = action.content.orEmpty()
                CodeStorage.writeText(context, Uri.parse(entry.id), content)
                if (entry.id == enteredId) {
                    enteredCode = content
                    lastPersistedCode = content
                }
                refreshTree()
                return "Updated ${action.path}."
            }
            "deleteFile" -> {
                val entry = resolveEntryByPath(action.path) ?: return "Couldn't find ${action.path} to delete."
                return if (CodeStorage.delete(context, Uri.parse(entry.id))) {
                    val doomed = entries.subtreeIds(entry.id)
                    if (enteredId in doomed) {
                        enteredId = null
                        viewModel.storage.lastEnteredCodeId = null
                    }
                    if (activeCommandLocationId in doomed) activeCommandLocationId = null
                    refreshTree()
                    "Deleted ${action.path}."
                } else {
                    "Couldn't delete ${action.path}."
                }
            }
            "renameFile" -> {
                val entry = resolveEntryByPath(action.path) ?: return "Couldn't find ${action.path} to rename."
                val newName = action.newName?.trim().orEmpty()
                if (newName.isBlank()) return "No new name given for ${action.path}."
                return if (CodeStorage.rename(context, Uri.parse(entry.id), newName)) {
                    refreshTree()
                    "Renamed ${action.path} to $newName."
                } else {
                    "Couldn't rename ${action.path}."
                }
            }
            "moveFile" -> {
                val entry = resolveEntryByPath(action.path) ?: return "Couldn't find ${action.path} to move."
                val oldParentDoc = resolveParentDoc(entry.parentId) ?: return "Couldn't resolve ${action.path}'s current folder."
                val destination = action.destinationFolder.orEmpty()
                val newParentId = resolveOrCreateFolderId(destination)
                if (newParentId != null && newParentId in entries.subtreeIds(entry.id)) return "Can't move ${action.path} into its own subfolder."
                val newParentDoc = resolveParentDoc(newParentId) ?: return "Couldn't resolve the destination folder."
                return if (CodeStorage.move(context, Uri.parse(entry.id), oldParentDoc.uri, newParentDoc.uri)) {
                    refreshTree()
                    "Moved ${action.path} to ${destination.ifBlank { "the top level" }}."
                } else {
                    "Couldn't move ${action.path}."
                }
            }
            "copyFile" -> {
                val entry = resolveEntryByPath(action.path) ?: return "Couldn't find ${action.path} to copy."
                val destination = action.destinationFolder.orEmpty()
                val newParentId = resolveOrCreateFolderId(destination)
                if (newParentId != null && newParentId in entries.subtreeIds(entry.id)) return "Can't copy ${action.path} into its own subfolder."
                val newParentDoc = resolveParentDoc(newParentId) ?: return "Couldn't resolve the destination folder."
                return if (CodeStorage.copy(context, Uri.parse(entry.id), newParentDoc.uri)) {
                    refreshTree()
                    "Copied ${action.path} to ${destination.ifBlank { "the top level" }}."
                } else {
                    "Couldn't copy ${action.path}."
                }
            }
            "runFile" -> {
                val entry = resolveEntryByPath(action.path)
                if (entry == null || entry.isFolder) return "Couldn't find a file at ${action.path} to run."
                val language = languageFromFilename(entry.name)
                val slug = if (language != null && language != HTML_PSEUDO_LANGUAGE) {
                    languages.firstOrNull { it.language.equals(language, ignoreCase = true) }?.version
                } else {
                    null
                }
                if (slug == null) return "Can't run ${action.path} as a project."
                val entryCode = CodeStorage.readText(context, Uri.parse(entry.id))
                val supportFiles = filesUnder(entries, entry.parentId)
                    .filter { it.id != entry.id }
                    .map { CodeFile(name = relativePath(entries, entry.parentId, it), content = CodeStorage.readText(context, Uri.parse(it.id))) }
                val result = viewModel.runCode(slug, entryCode, stdin, supportFiles)
                return result.fold(
                    onSuccess = { r ->
                        val output = buildString {
                            if (r.stdout.isNotBlank()) appendLine(r.stdout.trimEnd('\n'))
                            if (r.stderr.isNotBlank()) appendLine(r.stderr.trimEnd('\n'))
                            if (!r.compileOutput.isNullOrBlank()) appendLine(r.compileOutput.trimEnd('\n'))
                        }.trim().ifBlank { "(no output)" }
                        "Ran ${action.path}:\n$output"
                    },
                    onFailure = { "Running ${action.path} failed: ${it.message}" },
                )
            }
            else -> return "Unrecognized action."
        }
    }

    // Runs every action in order, aggregates a combined result message, and
    // reports once at the end - used for both a single action and a bulk
    // request like "delete all the test files" (one exchange, N actions).
    fun executeAiFileActions(actions: List<AiFileActionDto>, onResult: (String) -> Unit) {
        scope.launch {
            val results = actions.map { executeSingleFileAction(it) }
            onResult(results.joinToString("\n"))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                CodeTab.PAD -> CodePadBody(
                    onBack = onBack,
                    entries = entries,
                    entered = enteredEntry,
                    code = enteredCode,
                    onCodeChange = { updateCode(it) },
                    resolvedLanguage = resolvedLanguage,
                    running = running,
                    onRun = { runCode() },
                    checkingCode = backerChecking,
                    onCheckCode = { checkCodeManually() },
                    onGoToDocuments = { setActiveTab(CodeTab.DOCUMENTS) },
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onUndo = { undoEdit() },
                    onRedo = { redoEdit() },
                    justSaved = justSaved,
                    onSaveNow = { saveNow() },
                    outputDestination = outputDestination,
                    onChangeOutputDestination = { setOutputDestination(it) },
                    guiSessionStarting = guiSessionStarting,
                    guiSessionError = guiSessionError,
                    onOpenLiveView = { openLiveView() },
                    packagesInput = packagesInput,
                    onPackagesInputChange = { packagesInput = it },
                )
                CodeTab.COMMAND -> CodeCommandBody(
                    stdin = stdin,
                    onStdinChange = { stdin = it },
                    entries = entries,
                    onResolveAd = { locationId, argument -> resolveAd(locationId, argument) },
                    onActiveLocationChanged = { activeCommandLocationId = it },
                    tabs = commandTabs,
                    onTabsChange = { commandTabs = it },
                    activeTabIndex = activeCommandTabIndex,
                    onActiveTabIndexChange = { activeCommandTabIndex = it },
                    nextTabNumber = nextCommandTabNumber,
                    onNextTabNumberChange = { nextCommandTabNumber = it },
                    commandInput = commandInputText,
                    onCommandInputChange = { commandInputText = it },
                    onRunProject = { tabId, folderId, fileName -> runProject(tabId, folderId, fileName) },
                    onResolveAiSee = { locationId, argument -> resolveAiSee(locationId, argument) },
                    onSetAiScope = { entry -> if (entry.isFolder) setAiScopeToFolder(entry) else setAiScopeToFile(entry) },
                )
                CodeTab.DOCUMENTS -> CodeDocumentsBody(
                    hasFolder = codeFolderUri != null,
                    loadingTree = loadingTree,
                    treeError = treeError,
                    onChooseFolder = { folderPickerLauncher.launch(null) },
                    entries = entries,
                    enteredId = enteredId,
                    defaultParentId = activeCommandLocationId,
                    onEnter = { id -> enterId(id); setActiveTab(CodeTab.PAD) },
                    onCreate = { isFolder, parentId, onCreated ->
                        val parent = resolveParentDoc(parentId)
                        if (parent != null) {
                            scope.launch {
                                val name = uniqueSiblingName(entries, parentId, if (isFolder) "folder" else "file")
                                val created = if (isFolder) CodeStorage.createFolder(parent, name) else CodeStorage.createFile(parent, name)
                                refreshTree()
                                if (!isFolder && created != null) viewModel.triggerAchievementFireAndForget("first_code")
                                created?.let { onCreated(it.uri.toString()) }
                            }
                        }
                    },
                    onImport = { name, content, parentId, onImported ->
                        val parent = resolveParentDoc(parentId)
                        if (parent != null) {
                            scope.launch {
                                val finalName = uniqueSiblingName(entries, parentId, name)
                                val created = CodeStorage.createFile(parent, finalName)
                                if (created != null) CodeStorage.writeText(context, created.uri, content)
                                refreshTree()
                                created?.let { onImported(it.uri.toString()) }
                            }
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            if (CodeStorage.delete(context, Uri.parse(id))) {
                                val doomed = entries.subtreeIds(id)
                                if (enteredId in doomed) {
                                    enteredId = null
                                    viewModel.storage.lastEnteredCodeId = null
                                }
                                if (activeCommandLocationId in doomed) activeCommandLocationId = null
                                refreshTree()
                            }
                        }
                    },
                    onRename = { id, newName ->
                        scope.launch {
                            CodeStorage.rename(context, Uri.parse(id), newName)
                            refreshTree()
                        }
                    },
                    onMove = { id, newParentId ->
                        val entry = entries.firstOrNull { it.id == id }
                        val oldParentDoc = resolveParentDoc(entry?.parentId)
                        val newParentDoc = resolveParentDoc(newParentId)
                        if (entry != null && oldParentDoc != null && newParentDoc != null &&
                            (newParentId == null || newParentId !in entries.subtreeIds(id))
                        ) {
                            scope.launch {
                                CodeStorage.move(context, Uri.parse(id), oldParentDoc.uri, newParentDoc.uri)
                                refreshTree()
                            }
                        }
                    },
                    onDuplicate = { id, newParentId ->
                        val newParentDoc = resolveParentDoc(newParentId)
                        if (newParentDoc != null && (newParentId == null || newParentId !in entries.subtreeIds(id))) {
                            scope.launch {
                                CodeStorage.copy(context, Uri.parse(id), newParentDoc.uri)
                                refreshTree()
                            }
                        }
                    },
                    githubStatus = githubStatus,
                    githubRepos = githubRepos,
                    githubRepoPickerOpen = githubRepoPickerOpen,
                    githubSyncing = githubSyncing,
                    githubSyncError = githubSyncError,
                    githubSyncNotice = githubSyncNotice,
                    githubConflicts = githubConflicts,
                    onConnectGithub = ::connectGithub,
                    onOpenGithubRepoPicker = ::openGithubRepoPicker,
                    onCloseGithubRepoPicker = { githubRepoPickerOpen = false },
                    onSelectGithubRepo = ::selectGithubRepo,
                    onDisconnectGithub = ::disconnectGithub,
                    onSyncGithub = ::runGithubSync,
                    onResolveGithubConflict = ::resolveGithubConflict,
                    onDismissGithubNotice = { githubSyncNotice = null; githubSyncError = null },
                )
                CodeTab.VIEW -> CodeViewBody(
                    result = result,
                    runError = runError,
                    running = running,
                    viewMessage = viewMessage,
                    language = resolvedLanguage,
                    onExplain = { errorText -> viewModel.explainError(resolvedLanguage ?: "unknown", errorText) },
                    androidBuildJob = androidBuildJob,
                    androidBuildError = androidBuildError,
                    installing = installing,
                    installError = installError,
                    buildStartedAtMillis = CodeRunSession.buildStartedAtMillis,
                    onInstall = { job ->
                        val url = job.downloadUrl
                        if (url != null) {
                            installing = true
                            installError = null
                            scope.launch {
                                installError = downloadAndInstallApk(context, url, job.jobId)
                                installing = false
                            }
                        }
                    },
                    onCancelBuild = { CodeRunSession.cancelWatching() },
                    backerChecking = backerChecking,
                    backerReason = backerReason,
                    backerFixedCode = backerFixedCode,
                    onApplyBackerFix = { applyBackerFixAndRun() },
                    onRunAnywayIgnoringBacker = { runAnywayIgnoringBacker() },
                )
                CodeTab.RULES -> CodeRulesBody(
                    entries = entries,
                    enteredEntry = enteredEntry,
                    enteredCode = enteredCode,
                    highlightId = highlightId,
                    onFileAction = { actions, onResult -> executeAiFileActions(actions, onResult) },
                    aiScopeId = aiScopeId,
                    aiScopeViewId = aiScopeViewId,
                    onPickAiScopeFile = { entry -> setAiScopeToFile(entry) },
                    onPickAiScopeFolder = { entry -> setAiScopeToFolder(entry) },
                    onChangeAiScopeView = { id -> aiScopeViewId = id },
                    onClearAiScope = { clearAiScope() },
                )
              }
        }
        // Hidden while the keyboard is up (e.g. typing in the AI chat
        // sub-tab) - it would otherwise still reserve its fixed height even
        // though it renders entirely behind/under the keyboard, needlessly
        // shrinking the chat area above it for no visible benefit.
        if (showOwnBottomBar && WindowInsets.ime.getBottom(LocalDensity.current) <= 0) {
            CodeBottomBar(activeTab) { setActiveTab(it) }
        }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, CircleShape)
                .clickable { showHelpPage = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("?", color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // "Coder is presently viewing: X" — floats over every Code tab
        // (same TopEnd row as the ? button) whenever ai see/the AI tab's own
        // picker has scoped the AI to one file/folder, so it's never a
        // surprise what the AI can currently see. Cancel puts it back to
        // normal (whole-project) instead of needing another `ai see`.
        if (aiScopeEntry != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(14.dp))
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    "AI viewing: ${relativePath(entries, null, aiScopeEntry)}${if (aiScopeEntry.isFolder) "/" else ""}",
                    color = CedalColors.AccentCyan, fontSize = 11.sp, maxLines = 1,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { clearAiScope() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = CedalColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showWalkthrough) {
            CodeWalkthroughOverlay(onDone = { dismissWalkthrough() })
        }
        if (showHelpPage) {
            CodeHelpOverlay(
                onClose = { showHelpPage = false },
                onReplayWalkthrough = {
                    showHelpPage = false
                    showWalkthrough = true
                },
            )
        }
        htmlPreviewHtml?.let { html ->
            HtmlPreviewOverlay(html = html, onClose = { htmlPreviewHtml = null })
        }
    }
}

@Composable
private fun CodeBottomBar(active: CodeTab, onNavigateTab: (CodeTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(CedalColors.CardBackground)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        CodeTabItem("Pad", Icons.Outlined.Edit, active == CodeTab.PAD) { onNavigateTab(CodeTab.PAD) }
        CodeTabItem("Command", Icons.Outlined.Terminal, active == CodeTab.COMMAND) { onNavigateTab(CodeTab.COMMAND) }
        CodeTabItem("View", Icons.Outlined.Visibility, active == CodeTab.VIEW) { onNavigateTab(CodeTab.VIEW) }
        CodeTabItem("Explorer", Icons.Outlined.Folder, active == CodeTab.DOCUMENTS) { onNavigateTab(CodeTab.DOCUMENTS) }
        CodeTabItem("AI", Icons.Outlined.SmartToy, active == CodeTab.RULES) { onNavigateTab(CodeTab.RULES) }
    }
}

@Composable
private fun CodeTabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) CedalColors.Success else CedalColors.TextMuted, modifier = Modifier.size(20.dp))
        Text(label, color = if (active) CedalColors.Success else CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

private data class WalkthroughStep(val title: String, val body: String)

private val WALKTHROUGH_STEPS = listOf(
    WalkthroughStep("Explorer", "Your real files, saved for good. Tap + to make a file or folder, tap a file to open it in Pad. Everything here lives in a real folder on your phone, not just in the app."),
    WalkthroughStep("Pad", "Write your code here. Tap the round ▶ button to run it — the result always shows up in View, even if it fails. Every Run first goes through Backer, a quick AI check for likely bugs — if it finds something, you'll see the reason and a choice to apply its fix or run your code as-is anyway."),
    WalkthroughStep("Command", "Not a full shell — just two commands. ad foldername (or ad ..) moves around your files, same idea as cd. run filename runs that file as a project, bundling every other file in the current folder along with it so local imports work. Handy for a folder with lots of files: ad into it once, then just type run and whichever short filename you need — no full path required. STDIN below is optional — only needed if your program asks for typed input."),
    WalkthroughStep("View", "Your code's output — printed text, errors, everything. If something goes wrong, tap \"Explain this in plain English\" and AI will translate the error for you."),
)

// In-app preview for HTML's default ("webview") output destination — a real
// native WebView, not sent to the server at all, same as the "Browser" path
// just rendered in-app instead of handed off to another app.
@Composable
private fun HtmlPreviewOverlay(html: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Preview", color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                DocumentsToolbarButton("Close", onClick = onClose)
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx -> WebView(ctx) },
                update = { webView -> webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
            )
        }
    }
}

// Shown once automatically (see SecureStorage.codeTutorialSeen), replayable
// anytime via the help page's "Replay walkthrough" button.
@Composable
private fun CodeWalkthroughOverlay(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val isLast = step == WALKTHROUGH_STEPS.lastIndex
    val current = WALKTHROUGH_STEPS[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text("Welcome to Code", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
            Text(current.title, color = CedalColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Text(current.body, color = CedalColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                WALKTHROUGH_STEPS.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (i == step) CedalColors.AccentCyan else CedalColors.BorderSlate),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp).fillMaxWidth()) {
                DocumentsToolbarButton("Skip", onClick = onDone)
                Box(modifier = Modifier.weight(1f))
                DocumentsToolbarButton(if (isLast) "Done" else "Next", onClick = { if (isLast) onDone() else step++ })
            }
        }
    }
}

// The static, always-available "How to use Code" page — reachable via the
// "?" button in the corner of every tab, independent of the one-time
// walkthrough above.
@Composable
private fun CodeHelpOverlay(onClose: () -> Unit, onReplayWalkthrough: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CedalColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("How to use Code", color = CedalColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                DocumentsToolbarButton("Close", onClick = onClose)
            }
            Text(
                "Code is a notepad, not a full IDE: write plain text, run it on a real server, see the real output. No installs, no setup.",
                color = CedalColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )

            WALKTHROUGH_STEPS.forEach { step ->
                Text(step.title, color = CedalColors.AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Text(step.body, color = CedalColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }

            Text("Languages", color = CedalColors.AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            Text(
                "Whatever a file's name ends in decides how it runs — script.py, main.rs, App.kt, index.ts, and more. Rename a file in Explorer to fix its language.",
                color = CedalColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp,
            )

            Box(modifier = Modifier.padding(top = 24.dp)) {
                CedalGhostButton(text = "Replay the walkthrough", onClick = onReplayWalkthrough)
            }
        }
    }
}

// id is "{rowId}#user" or "{rowId}#assistant" - AiChangeRequests bundles a
// whole exchange per row, so each half is addressed as its own virtual id
// for reply/pin/report/edit/delete purposes (see AiChangeRequestService).
private data class RulesChatBubble(
    val id: String,
    val rowId: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val replyToId: String? = null,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)
private val RULES_BUBBLE_MINE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
private val RULES_BUBBLE_THEIRS_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
private const val RULES_EDIT_WINDOW_MS = 5 * 60 * 1000L

private fun AiChangeRequestDto.toBubbles(): List<RulesChatBubble> {
    // Media only ever attaches to the user's own half of the row - the
    // assistant's reply is always plain text.
    val userBubble = RulesChatBubble("$id#user", id, "user", requestText.orEmpty(), createdAt, replyToId, requestEditedAt, requestDeleted, mediaUrl, mediaType, fileName)
    val replyText = when (status) {
        "answered" -> answerText
        // "Drafted" would suggest something already shipped - it hasn't.
        // This can genuinely take anywhere from seconds to days since it
        // waits on a real human (the admin) tapping Approve - the chat
        // itself is never blocked on that, so said so explicitly.
        "pending_approval" -> "Drafted and sent to the admin for review: ${summary ?: "a new language"}. This can take anywhere from a few seconds to a few days, since it's waiting on the admin - feel free to keep asking me other things in the meantime. I'll update this message here once it's been decided."
        "deploying" -> "Approved! ${summary ?: "This"} is being deployed now - almost there."
        "deployed" -> "Confirmed - ${summary ?: "this"} was approved and is now live. Try it!"
        "rejected" -> "This request was denied by the admin: ${summary ?: "no reason given"}."
        "error" -> "Something went wrong: ${errorMessage ?: summary ?: "unknown error"}"
        "pending_judgment" -> null
        else -> summary
    }
    return if (replyText != null) listOf(userBubble, RulesChatBubble("$id#assistant", id, "assistant", replyText, createdAt)) else listOf(userBubble)
}

// Was a static "what does this language need to run" reference page - now
// a plain AI chat, styled to match Corneal (CornealChatBody) exactly rather
// than living as a bolted-on reference-plus-chat hybrid. Ask what's
// supported, or ask for a genuinely new language and it drafts real support
// for it (see AiChangeRequestService server-side). Judge-only replies
// ("answered") come back in a few seconds; a real "add a language" request
// takes longer (an AI call plus a GitHub round trip) and always ends up
// waiting on admin approval - nothing ships just from asking.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeRulesBody(
    entries: List<CodeEntry> = emptyList(),
    enteredEntry: CodeEntry? = null,
    enteredCode: String = "",
    highlightId: String? = null,
    onFileAction: (List<AiFileActionDto>, (String) -> Unit) -> Unit = { _, onResult -> onResult("File actions aren't available right now.") },
    aiScopeId: String? = null,
    aiScopeViewId: String? = null,
    onPickAiScopeFile: (CodeEntry) -> Unit = {},
    onPickAiScopeFolder: (CodeEntry) -> Unit = {},
    onChangeAiScopeView: (String) -> Unit = {},
    onClearAiScope: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val greeting = RulesChatBubble("greeting", "greeting", "assistant", "Ask me what's supported, request a new language, or ask me to create/edit/run/delete something in your Code area.", 0)
    val messages = remember { mutableStateOf(listOf(greeting)) }

    // A single AI call (judge, then maybe a build/file-action call) has no
    // real intermediate progress to report - this eases toward ~92% over a
    // typical reply's length and only reaches 100% once the real answer
    // actually lands (sending flips false), rather than a spinner with no
    // sense of how long is left.
    var generationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(sending) {
        if (sending) {
            generationProgress = 0f
            while (sending && generationProgress < 0.92f) {
                delay(300)
                generationProgress = (generationProgress + 0.02f).coerceAtMost(0.92f)
            }
        } else {
            generationProgress = 0f
        }
    }

    var replyTarget by remember { mutableStateOf<RulesChatBubble?>(null) }
    var editingBubble by remember { mutableStateOf<RulesChatBubble?>(null) }
    var actionsFor by remember { mutableStateOf<RulesChatBubble?>(null) }
    var forwardingBubble by remember { mutableStateOf<RulesChatBubble?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var highlightedId by remember { mutableStateOf(highlightId) }
    var rawHistory by remember { mutableStateOf<List<AiChangeRequestDto>>(emptyList()) }
    var headerMenuOpen by remember { mutableStateOf(false) }
    var showRequests by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Attachment picker state - see AttachmentSheetOverlay/CameraChoiceOverlay/
    // EmojiChoiceOverlay/StickerPickerOverlay in ChatActionComponents.kt,
    // shared with friend chat rather than a separate copy.
    var attachSheetOpen by remember { mutableStateOf(false) }
    var cameraChoiceOpen by remember { mutableStateOf(false) }
    var emojiChoiceOpen by remember { mutableStateOf(false) }
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerPickerInitialTab by remember { mutableStateOf(StickerPanelTab.EMOJI) }
    var myStickers by remember { mutableStateOf<List<com.xhacker.cedal.data.StickerDto>>(emptyList()) }
    var uploadingSticker by remember { mutableStateOf(false) }
    var uploadingAttachment by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // Voice notes - empty-composer Send becomes a mic icon (see the input
    // row below); tapping it shows VoiceRecorderPanel instead of the normal
    // row. A finished recording is held here, not sent immediately, so you
    // can keep typing and send it together with any text - see send().
    var voiceRecorderActive by remember { mutableStateOf(false) }
    var pendingVoiceFile by remember { mutableStateOf<java.io.File?>(null) }
    var pendingVoiceDurationMs by remember { mutableStateOf(0L) }
    // Shows an explicit "go to Settings" dialog on a "Don't ask again"
    // denial instead of silently doing nothing forever - see
    // PermissionGate.kt.
    val permissionGate = rememberPermissionGate()
    fun openVoiceRecorder() {
        permissionGate.request(context, android.Manifest.permission.RECORD_AUDIO) { voiceRecorderActive = true }
    }
    // In-memory guard only - prevents the periodic refresh loop and a fresh
    // mount from both trying to execute the same row's file action at once
    // before the server-persisted fileActionExecuted flag round-trips back.
    // The durable guard is that server flag, not this set.
    var executingFileActionIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Hydrates the real saved conversation on open - AiChangeRequests rows
    // already ARE this chat's turn-by-turn history, see
    // AiChangeRequestService.listHistory. Also catches up on any file
    // action the AI already decided but that never actually got applied
    // on-device - happens if the user left this screen (or the app
    // restarted) before the original request's poll loop reached it, since
    // that coroutine dies the moment this composable leaves composition.
    suspend fun refreshHistory() {
        viewModel.getAiRequestHistory().onSuccess { history ->
            rawHistory = history
            if (history.isNotEmpty()) messages.value = history.flatMap { it.toBubbles() }
            history.forEach { row ->
                val actions = row.fileAction
                if (row.status == "answered" && !actions.isNullOrEmpty() && !row.fileActionExecuted && row.id !in executingFileActionIds) {
                    executingFileActionIds = executingFileActionIds + row.id
                    onFileAction(actions) {
                        scope.launch {
                            viewModel.markFileActionExecuted(row.id)
                            refreshHistory()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
        viewModel.listMyStickers().onSuccess { myStickers = it }
    }

    // A "newLanguage" request can resolve (admin approves/rejects) long
    // after this screen was last open - this is what lets the AI actually
    // "say" the outcome later, by periodically re-checking and letting
    // toBubbles() re-derive that row's reply text from its now-current
    // status, without the user needing to ask again.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            refreshHistory()
        }
    }

    LaunchedEffect(highlightedId, messages.value.size) {
        val id = highlightedId ?: return@LaunchedEffect
        val index = messages.value.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(2000)
            highlightedId = null
        }
    }

    suspend fun pollUntilResolved(id: String) {
        while (true) {
            delay(2500)
            val dto = viewModel.getAiRequest(id).getOrNull() ?: continue
            if (dto.status == "pending_judgment") continue
            // refreshHistory() itself now executes any not-yet-applied file
            // action it finds (see its own doc comment) - no need to
            // duplicate that here, which also avoids a race between this
            // and the periodic refresh loop both trying to run it.
            refreshHistory()
            sending = false
            return
        }
    }

    // Builds (treePaths, currentFile, linkedFiles) for the outgoing AI
    // request — unrestricted (today's original heuristic: every file's path
    // + whatever's open in Pad + a few name-matched linked files) when no
    // scope is set, or confined to exactly the scoped file/folder when
    // aiScopeId is set (see MemberCodeBody's doc comment on that state). A
    // folder scope sends every file under it: the one matching
    // aiScopeViewId as the full-content currentFile, the rest as
    // linkedFiles — same DTO/caps the heuristic linking already used, just
    // an explicit list instead of a name-match guess.
    suspend fun buildAiContext(): Triple<List<String>, AiCurrentFileDto?, List<AiCurrentFileDto>> {
        val scopeEntry = aiScopeId?.let { id -> entries.firstOrNull { it.id == id } }
        if (scopeEntry == null) {
            val treePaths = entries.filter { !it.isFolder }.map { relativePath(entries, null, it) }
            val currentFile = enteredEntry?.takeIf { !it.isFolder }?.let { AiCurrentFileDto(relativePath(entries, null, it), enteredCode) }
            val linkedFiles = findLinkedFiles(context, entries, enteredEntry, currentFile)
            return Triple(treePaths, currentFile, linkedFiles)
        }
        if (!scopeEntry.isFolder) {
            val content = if (scopeEntry.id == enteredEntry?.id) enteredCode else CodeStorage.readText(context, Uri.parse(scopeEntry.id))
            val path = relativePath(entries, null, scopeEntry)
            return Triple(listOf(path), AiCurrentFileDto(path, content), emptyList())
        }
        val filesInScope = filesUnder(entries, scopeEntry.id)
        val treePaths = filesInScope.map { relativePath(entries, null, it) }
        val viewEntry = filesInScope.firstOrNull { it.id == aiScopeViewId } ?: filesInScope.firstOrNull()
        val currentFile = viewEntry?.let {
            val content = if (it.id == enteredEntry?.id) enteredCode else CodeStorage.readText(context, Uri.parse(it.id))
            AiCurrentFileDto(relativePath(entries, null, it), content)
        }
        val linkedFiles = filesInScope
            .filter { it.id != viewEntry?.id }
            .take(MAX_LINKED_FILES)
            .map { AiCurrentFileDto(relativePath(entries, null, it), CodeStorage.readText(context, Uri.parse(it.id)).take(MAX_LINKED_FILE_CHARS)) }
        return Triple(treePaths, currentFile, linkedFiles)
    }

    // A picked image/video/file uploads and submits as its own request right
    // away (no caption text needed - see AiChangeRequestRoutes' fallback
    // text), same immediate-send pattern as Corneal/ARC's attachments.
    fun sendMedia(url: String, mediaType: String, fileName: String?) {
        messages.value = messages.value + RulesChatBubble("pending-${System.currentTimeMillis()}", "", "user", "", System.currentTimeMillis(), mediaUrl = url, mediaType = mediaType, fileName = fileName)
        sending = true
        scope.launch {
            val (treePaths, currentFile, _) = buildAiContext()
            viewModel.submitAiRequest("", treePaths, currentFile, null, url, mediaType, fileName)
                .onSuccess { dto -> pollUntilResolved(dto.id) }
                .onFailure {
                    messages.value = messages.value + RulesChatBubble("err-${System.currentTimeMillis()}", "", "assistant", it.message ?: "Couldn't reach the AI", System.currentTimeMillis())
                    sending = false
                }
        }
    }

    fun uploadAndSend(uri: android.net.Uri, kind: String, mediaType: String, fallbackFileName: String? = null) {
        uploadingAttachment = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fName = fallbackFileName ?: queryDisplayName(context, uri)
            if (bytes != null) {
                viewModel.uploadImage(kind, bytes, mimeType)
                    .onSuccess { url -> sendMedia(url, mediaType, fName) }
                    .onFailure { messages.value = messages.value + RulesChatBubble("err-${System.currentTimeMillis()}", "", "assistant", it.message ?: "Upload failed", System.currentTimeMillis()) }
            }
            uploadingAttachment = false
        }
    }

    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        uris.forEach { uri ->
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            uploadAndSend(uri, "chat_media", if (mimeType.startsWith("video/")) "video" else "image")
        }
    }
    val cameraPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
    ) { success -> pendingCameraUri?.let { uri -> if (success) uploadAndSend(uri, "chat_media", "image") } }
    val cameraVideoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo(),
    ) { success -> pendingCameraUri?.let { uri -> if (success) uploadAndSend(uri, "chat_media", "video") } }
    fun newCameraUri(extension: String): android.net.Uri {
        val dir = java.io.File(context.cacheDir, "chat_camera").apply { mkdirs() }
        val file = java.io.File(dir, "capture_${System.currentTimeMillis()}.$extension")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) uploadAndSend(uri, "chat_file", "file") }
    val stickerImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingSticker = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                viewModel.uploadImage("sticker", bytes, mimeType)
                    .onSuccess { url -> viewModel.createSticker(url).onSuccess { sticker -> myStickers = myStickers + sticker } }
                    .onFailure { }
            }
            uploadingSticker = false
        }
    }

    fun send() {
        val text = input.trim()
        val voiceFile = pendingVoiceFile
        if ((text.isBlank() && voiceFile == null) || sending) return
        val editing = editingBubble
        if (editing != null) {
            input = ""
            editingBubble = null
            sending = true
            scope.launch {
                viewModel.editAiRequest(editing.rowId, text)
                    .onSuccess { refreshHistory() }
                    .onFailure { messages.value = messages.value + RulesChatBubble("err-${System.currentTimeMillis()}", "", "assistant", it.message ?: "Couldn't edit that", System.currentTimeMillis()) }
                sending = false
            }
            return
        }
        val replyId = replyTarget?.id
        input = ""
        replyTarget = null
        pendingVoiceFile = null
        pendingVoiceDurationMs = 0L
        messages.value = messages.value + RulesChatBubble("pending-${System.currentTimeMillis()}", "", "user", text, System.currentTimeMillis(), replyId)
        sending = true
        scope.launch {
            val (treePaths, currentFile, linkedFiles) = buildAiContext()
            val result = if (voiceFile != null) {
                viewModel.uploadImage("chat_media", voiceFile.readBytes(), "audio/mp4a-latm")
                    .mapCatching { url ->
                        viewModel.submitAiRequest(text, treePaths, currentFile, replyId, url, "audio", voiceFile.name, linkedFiles).getOrThrow()
                    }
            } else {
                viewModel.submitAiRequest(text, treePaths, currentFile, replyId, linkedFiles = linkedFiles)
            }
            result
                .onSuccess { dto -> pollUntilResolved(dto.id) }
                .onFailure {
                    messages.value = messages.value + RulesChatBubble("err-${System.currentTimeMillis()}", "", "assistant", it.message ?: "Couldn't reach the AI", System.currentTimeMillis())
                    sending = false
                }
        }
    }

    LaunchedEffect(messages.value.size, sending) {
        if (messages.value.isNotEmpty() && highlightedId == null) listState.animateScrollToItem(messages.value.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "⋮", color = CedalColors.TextMuted, fontSize = 18.sp,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { headerMenuOpen = true }
                    .padding(4.dp),
            )
        }
        if (headerMenuOpen) {
            DocumentsMenuPanel {
                DocumentsMenuRow("Requests") { headerMenuOpen = false; showRequests = true }
                DocumentsMenuRow("Delete chat history") {
                    headerMenuOpen = false
                    scope.launch {
                        viewModel.deleteAiRequestHistory().onSuccess {
                            messages.value = listOf(greeting)
                            rawHistory = emptyList()
                        }
                    }
                }
            }
        }

        // What the AI can currently see — mirrors the "AI viewing: X" banner
        // near the ? button (that one's Cancel resets to unrestricted; this
        // row is where you set/change it manually, same effect as typing
        // "ai see name" in Command). A folder scope additionally shows
        // "Switch file" — the dropdown from the feature request — since a
        // folder can hold more than one file and the AI only reads one of
        // them as its full-content "currentFile" at a time (see
        // buildAiContext).
        run {
            val scopeEntry = entries.firstOrNull { it.id == aiScopeId }
            val scopeIsFolder = scopeEntry?.isFolder == true
            var showScopePicker by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    when {
                        scopeEntry == null -> "AI sees the whole project"
                        scopeIsFolder -> "AI sees ${relativePath(entries, null, scopeEntry)}/ — viewing ${entries.firstOrNull { it.id == aiScopeViewId }?.name ?: "nothing yet"}"
                        else -> "AI sees only ${relativePath(entries, null, scopeEntry)}"
                    },
                    color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f),
                )
                Text(
                    if (scopeIsFolder) "Switch file ▾" else "Choose scope ▾",
                    color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showScopePicker = !showScopePicker }
                        .padding(start = 8.dp),
                )
                if (scopeEntry != null) {
                    Text(
                        "Reset",
                        color = CedalColors.TextMuted, fontSize = 11.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showScopePicker = false; onClearAiScope() }
                            .padding(start = 8.dp),
                    )
                }
            }
            if (showScopePicker) {
                val boundary = if (scopeIsFolder) aiScopeId else null
                val startFolderId = when {
                    scopeEntry == null -> null
                    scopeIsFolder -> entries.firstOrNull { it.id == aiScopeViewId }?.parentId ?: aiScopeId
                    else -> scopeEntry.parentId
                }
                AiScopePickerPanel(
                    entries = entries,
                    startFolderId = startFolderId,
                    boundaryRootId = boundary,
                    activeFileId = if (scopeIsFolder) aiScopeViewId else aiScopeId,
                    onPickFile = { entry ->
                        if (boundary != null) onChangeAiScopeView(entry.id) else onPickAiScopeFile(entry)
                        showScopePicker = false
                    },
                    onUseFolder = if (boundary == null) { folder -> onPickAiScopeFolder(folder); showScopePicker = false } else null,
                    onClose = { showScopePicker = false },
                )
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(messages.value, key = { _, bubble -> bubble.id }) { _, bubble ->
                val isMine = bubble.role == "user"
                val replySnippet = bubble.replyToId?.let { rid -> messages.value.firstOrNull { it.id == rid } }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        if (bubble.mediaUrl != null && !bubble.deleted) {
                            ChatMediaOrIcon(bubble.mediaUrl, bubble.mediaType, bubble.fileName, isMine, onLongPress = { if (bubble.rowId.isNotBlank()) actionsFor = bubble })
                            if (bubble.text.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clip(if (isMine) RULES_BUBBLE_MINE_SHAPE else RULES_BUBBLE_THEIRS_SHAPE)
                                        .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    ChatMessageContent(bubble.text, if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 13.sp)
                                }
                            }
                        } else {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(if (isMine) RULES_BUBBLE_MINE_SHAPE else RULES_BUBBLE_THEIRS_SHAPE)
                                .background(
                                    when {
                                        bubble.id == highlightedId -> CedalColors.AccentCyan.copy(alpha = 0.35f)
                                        isMine -> CedalColors.AccentCyan
                                        else -> CedalColors.CardBackground
                                    },
                                )
                                .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, RULES_BUBBLE_THEIRS_SHAPE) }
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                                    onClick = {}, onLongClick = { if (bubble.rowId.isNotBlank() && !bubble.deleted) actionsFor = bubble },
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column {
                                if (replySnippet != null) {
                                    Text(
                                        replySnippet.text.take(80),
                                        color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                                        fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                                if (bubble.deleted) {
                                    Text(
                                        androidx.compose.ui.text.AnnotatedString("Message deleted"),
                                        color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                                        fontSize = 15.sp,
                                        lineHeight = 21.sp,
                                    )
                                } else {
                                    ChatMessageContent(bubble.text, if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 15.sp)
                                }
                            }
                        }
                        }
                        if (bubble.rowId.isNotBlank() && !bubble.deleted) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Text(formatMessageTime(bubble.createdAt), color = CedalColors.TextMuted, fontSize = 9.sp)
                                if (bubble.editedAt != null) Text(" · edited", color = CedalColors.TextMuted, fontSize = 9.sp)
                                Text(
                                    "⋮", color = CedalColors.TextMuted, fontSize = 12.sp,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actionsFor = bubble },
                                )
                            }
                        }
                    }
                }
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        LinearProgressIndicator(
                            progress = { generationProgress },
                            color = CedalColors.AccentCyan,
                            trackColor = CedalColors.CardBackground,
                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)),
                        )
                        Text(
                            "${(generationProgress * 100).toInt()}%",
                            color = CedalColors.TextMuted, fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        replyTarget?.let { target ->
            ComposerContextBanner(label = if (target.role == "user") "Replying to yourself" else "Replying to the AI", snippet = target.text, onCancel = { replyTarget = null })
        }
        editingBubble?.let {
            ComposerContextBanner(label = "Editing message", snippet = it.text, onCancel = { editingBubble = null; input = "" })
        }

        if (voiceRecorderActive) {
            VoiceRecorderPanel(
                onReady = { file, durationMs ->
                    voiceRecorderActive = false
                    pendingVoiceFile = file
                    pendingVoiceDurationMs = durationMs
                },
                onCancel = { voiceRecorderActive = false },
            )
        } else {
            pendingVoiceFile?.let { file ->
                PendingVoiceChip(pendingVoiceDurationMs, onRemove = { pendingVoiceFile = null; pendingVoiceDurationMs = 0L })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("›", color = CedalColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = CedalColors.AccentCyan,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { attachSheetOpen = true },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (input.isEmpty()) {
                        Text("Ask or request a language…", color = CedalColors.TextMuted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val canSend = (input.isNotBlank() || pendingVoiceFile != null) && !sending
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .let {
                            if (canSend) {
                                it.shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFF00FF41), ambientColor = Color(0xFF00FF41))
                            } else {
                                it
                            }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) Color(0xFF00FF41) else CedalColors.CardBackground)
                        .border(1.dp, if (canSend) Color(0xFF00FF41) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                        .clickable(
                            enabled = !sending,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = if (canSend) { { send() } } else { { openVoiceRecorder() } },
                        ),
                ) {
                    Icon(
                        if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                        contentDescription = if (canSend) "Send" else "Record a voice note",
                        tint = if (canSend) CedalColors.Background else CedalColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    actionsFor?.let { bubble ->
        val isMine = bubble.role == "user"
        ChatBubbleActionsOverlay(
            canEdit = isMine && System.currentTimeMillis() - bubble.createdAt < RULES_EDIT_WINDOW_MS,
            canDelete = isMine,
            onReply = { replyTarget = bubble; editingBubble = null; actionsFor = null },
            onCopy = { actionsFor = null; clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bubble.text)) },
            onForward = { forwardingBubble = bubble; actionsFor = null },
            onPin = {
                actionsFor = null
                scope.launch { viewModel.pinMessage(chatType = "code", messageId = bubble.id, messageText = bubble.text).onSuccess { actionNotice = "Pinned" } }
            },
            onReport = {
                actionsFor = null
                scope.launch { viewModel.reportMessage("code", bubble.id, bubble.text).onSuccess { actionNotice = "Reported for admin review" } }
            },
            onEdit = { editingBubble = bubble; replyTarget = null; input = bubble.text; actionsFor = null },
            onDelete = {
                actionsFor = null
                scope.launch { viewModel.deleteAiRequest(bubble.rowId).onSuccess { refreshHistory() } }
            },
            onDismiss = { actionsFor = null },
        )
    }

    forwardingBubble?.let { bubble ->
        ForwardMessageSheet(
            viewModel = viewModel,
            onDismiss = { forwardingBubble = null },
            onPick = { label, key ->
                forwardingBubble = null
                scope.launch {
                    when {
                        key == "corneal" -> viewModel.cornealChat(bubble.text)
                        key == "arc" -> viewModel.arcChat(bubble.text)
                        key == "code" -> viewModel.submitAiRequest(bubble.text)
                        key.startsWith("friend:") -> viewModel.sendMessage(key.removePrefix("friend:"), bubble.text)
                    }
                    actionNotice = "Forwarded to $label"
                }
            },
        )
    }

    actionNotice?.let { notice ->
        LaunchedEffect(notice) { delay(2000); actionNotice = null }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(notice, color = CedalColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }

    if (showRequests) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showRequests = false },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .padding(16.dp),
            ) {
                Text("YOUR LANGUAGE REQUESTS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp))
                // Only rows that actually went through the newLanguage
                // pipeline (a PR was opened) - plain Q&A/file-action turns
                // aren't "requests" in this sense.
                val languageRequests = rawHistory.filter { it.prUrl != null }
                if (languageRequests.isEmpty()) {
                    Text("You haven't requested a new language yet.", color = CedalColors.TextMuted, fontSize = 12.sp)
                } else {
                    LazyColumn {
                        items(languageRequests, key = { it.id }) { req ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    req.summary ?: "Language request", color = CedalColors.TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                )
                                when (req.status) {
                                    "deployed" -> Text("✓", color = CedalColors.Success, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    "rejected" -> Text("✕", color = CedalColors.Error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    // Still pending_approval/deploying/error - no verdict yet, so no mark at all.
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (attachSheetOpen) {
        AttachmentSheetOverlay(
            onCamera = { attachSheetOpen = false; cameraChoiceOpen = true },
            onEmoji = { attachSheetOpen = false; emojiChoiceOpen = true },
            onFolder = { attachSheetOpen = false; filePicker.launch(arrayOf("*/*")) },
            onDismiss = { attachSheetOpen = false },
        )
    }
    if (cameraChoiceOpen) {
        CameraChoiceOverlay(
            onPhoto = { cameraChoiceOpen = false; pendingCameraUri = newCameraUri("jpg"); pendingCameraUri?.let { cameraPhotoLauncher.launch(it) } },
            onVideo = { cameraChoiceOpen = false; pendingCameraUri = newCameraUri("mp4"); pendingCameraUri?.let { cameraVideoLauncher.launch(it) } },
            onGallery = { cameraChoiceOpen = false; galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            onDismiss = { cameraChoiceOpen = false },
        )
    }
    if (emojiChoiceOpen) {
        EmojiChoiceOverlay(
            onEmoji = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.EMOJI; stickerPickerOpen = true },
            onSticker = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.STICKERS; stickerPickerOpen = true },
            onIcon = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.ICON; stickerPickerOpen = true },
            onDismiss = { emojiChoiceOpen = false },
        )
    }
    if (stickerPickerOpen) {
        StickerPickerOverlay(
            myStickers = myStickers,
            uploading = uploadingSticker,
            initialTab = stickerPickerInitialTab,
            onPickEmoji = { emoji -> input += emoji; stickerPickerOpen = false },
            onPickSticker = { url ->
                stickerPickerOpen = false
                // An Icon-pack pick is "icon:<Name>" (see ChatMediaOrIcon) -
                // not a real image, so it must NOT be sent as mediaType
                // "image" or the server would try to hand this fake URL to
                // an AI vision call and every provider would fail on it.
                sendMedia(url, if (url.startsWith("icon:")) "icon" else "sticker", null)
            },
            onUploadSticker = { stickerImagePicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDismiss = { stickerPickerOpen = false },
        )
    }
    permissionGate.PermissionBlockedDialog()
}

// --- Pad ---

@Composable
private fun CodePadBody(
    onBack: () -> Unit,
    entries: List<CodeEntry>,
    entered: CodeEntry?,
    code: String,
    onCodeChange: (String) -> Unit,
    resolvedLanguage: String?,
    running: Boolean,
    onRun: () -> Unit,
    checkingCode: Boolean,
    onCheckCode: () -> Unit,
    onGoToDocuments: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    justSaved: Boolean,
    onSaveNow: () -> Unit,
    outputDestination: String,
    onChangeOutputDestination: (String) -> Unit,
    guiSessionStarting: Boolean,
    guiSessionError: String?,
    onOpenLiveView: () -> Unit,
    packagesInput: String,
    onPackagesInputChange: (String) -> Unit,
) {
    // Nav row, language/Check Code/Run row, and the output-destination row
    // are all fixed here (not scrolled) - only the editor below scrolls, and
    // it scrolls within its own bounded box rather than as part of one page
    // scroll. That page-scroll approach (still used by Command) used to hold
    // this too, but BasicTextField auto-scrolls its scrollable ancestor to
    // keep the cursor visible above the keyboard - with one shared scroll,
    // pressing Enter enough times to push the cursor down dragged the whole
    // toolbar (Check Code included) up and off the top of the screen with it.
    // Giving the editor its own weight(1f) box means that auto-scroll only
    // ever moves within the editor's own bounds, and the toolbar above it
    // never scrolls at all.
    // imePadding(): MainActivity uses enableEdgeToEdge(), which means
    // windowSoftInputMode="adjustResize" alone doesn't shrink content for the
    // keyboard anymore - Compose has to explicitly reserve space for it, or
    // the on-screen keyboard just covers the editor while typing.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
    ) {
        MemberBackBar(title = "Code", onBack = onBack)

        if (entered == null) {
            Text(
                "You haven't chosen a file or folder yet.",
                color = CedalColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp),
            )
            CedalGhostButton(text = "Pick one in Explorer", onClick = onGoToDocuments)
            return@Column
        }

        // Undo/Redo, the address, and Save all share one compact row now
        // (previously two separate rows, one of them wrapping the chunky
        // Explorer-style DocumentsToolbarButton - fine for Explorer's bigger
        // touch targets, way oversized for what's just breadcrumb navigation
        // here). Address is read-only - naming/renaming happens in Explorer.
        // ◀/▶ used to be browser-style navigation between different files -
        // replaced with real edit undo/redo, which is what people actually
        // reached for them expecting.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            PadNavButton("◀ Undo", enabled = canUndo, onClick = onUndo)
            PadNavButton("Redo ▶", modifier = Modifier.padding(start = 4.dp), enabled = canRedo, onClick = onRedo)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    addressFor(entries, entered),
                    color = CedalColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (!entered.isFolder) {
                PadNavButton(if (justSaved) "Saved ✓" else "Save", onClick = onSaveNow)
            }
        }

        if (entered.isFolder) {
            Text(
                "That's a folder — folders don't have code. Pick or create a file inside it in Explorer.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
            CedalGhostButton(text = "Go to Explorer", onClick = onGoToDocuments)
            return@Column
        }

        val isHtml = resolvedLanguage == HTML_PSEUDO_LANGUAGE

        // Language status + the small Run/start button share one row up top,
        // so the whole rest of the screen below is nothing but the editor.
        // No sub-hint text here anymore for Kotlin/HTML specifics - Rules
        // (its own tab) already covers all of this, repeating it here just
        // pushed the actual editor further down for no real benefit.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                if (resolvedLanguage != null) {
                    Text("Detected language: $resolvedLanguage", color = CedalColors.Success, fontSize = 11.sp)
                } else {
                    Text(
                        "No recognized extension — rename in Explorer (e.g. add .py) to run.",
                        color = CedalColors.TextMuted, fontSize = 11.sp,
                    )
                }
            }
            // On-demand Backer scan - separate from Run's own automatic
            // check, for when you want to know if something's wrong without
            // actually running/building it (Kotlin builds especially aren't
            // free to keep retrying just to check). PadNavButton, not
            // CedalGhostButton - that one fills its whole row by design
            // (fine for a standalone Column button, breaks badly placed
            // inline next to other Row siblings like this).
            if (!isHtml) {
                PadNavButton(
                    text = if (checkingCode) "Checking…" else "Check Code",
                    enabled = !checkingCode && !running && resolvedLanguage != null,
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onCheckCode,
                )
            }
            RunStartButton(
                loading = running,
                enabled = !running && !checkingCode && resolvedLanguage != null,
                isHtml = isHtml,
                modifier = Modifier.padding(start = 10.dp),
                onClick = onRun,
            )
        }

        // Every language except Kotlin (which always installs to the phone
        // instead) can show its output either in-app or in the system browser.
        if (resolvedLanguage != null && resolvedLanguage != "Kotlin") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Text("SHOW OUTPUT IN", color = CedalColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.padding(end = 8.dp))
                OutputDestinationPill("In-app", outputDestination == "webview") { onChangeOutputDestination("webview") }
                OutputDestinationPill("Browser", outputDestination == "browser") { onChangeOutputDestination("browser") }
            }
        }

        // Python's Run just captures stdout/stderr like every other
        // interpreted language - a GUI toolkit (tkinter, etc.) has nowhere
        // to draw in that headless runner. This is the only way to actually
        // see and click a window it opens - see gui-runner/server.js.
        if (resolvedLanguage == "Python") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                PadNavButton(
                    text = if (guiSessionStarting) "Starting…" else "Open Live View",
                    enabled = !guiSessionStarting && !running && !checkingCode,
                    onClick = onOpenLiveView,
                )
            }
            CedalErrorText(guiSessionError)
        }

        // Most common packages are already pre-baked into the runner image
        // (see runner/Dockerfile) and need nothing here - this is only for
        // anything not already covered, explicit so it never silently
        // costs install time without the user asking for it.
        if (resolvedLanguage in setOf("Python", "JavaScript", "TypeScript", "Ruby", "PHP")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (packagesInput.isEmpty()) {
                    Text("Packages (comma-separated, e.g. emoji, colorama)", color = CedalColors.TextMuted, fontSize = 12.sp)
                }
                BasicTextField(
                    value = packagesInput,
                    onValueChange = onPackagesInputChange,
                    singleLine = true,
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 12.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 200.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            if (code.isEmpty()) {
                Text("print(\"hello world\")", color = CedalColors.TextSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// A small round "start" button instead of a big full-width Run bar — frees
// up the entire rest of Pad for the editor. Shows a spinner mid-run instead
// of swapping its own label, since there's no room here for button text.
@Composable
private fun RunStartButton(loading: Boolean, enabled: Boolean, isHtml: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) CedalColors.AccentCyan else CedalColors.AccentCyan.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !loading, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(color = CedalColors.Background, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(if (isHtml) "🌐" else "▶", color = CedalColors.Background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// A much more compact stand-in for Explorer's DocumentsToolbarButton,
// specifically for Pad's breadcrumb row - that one's sized for Explorer's
// bigger touch targets (18dp horizontal/10dp vertical padding, 16sp bold
// text), which made a simple back/forward/save row far taller than it
// needed to be here.
@Composable
private fun PadNavButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = if (enabled) CedalColors.TextPrimary else CedalColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// --- Command ---

private data class TerminalLine(val text: String, val isError: Boolean = false)

private data class CommandTab(
    val id: String,
    val label: String,
    val locationId: String? = null,
    val transcript: List<TerminalLine> = emptyList(),
)

private val TerminalGreen = Color(0xFF33FF66)

@Composable
private fun CodeCommandBody(
    stdin: String,
    onStdinChange: (String) -> Unit,
    entries: List<CodeEntry>,
    onResolveAd: (locationId: String?, argument: String) -> AdResult,
    onActiveLocationChanged: (String?) -> Unit,
    tabs: List<CommandTab>,
    onTabsChange: (List<CommandTab>) -> Unit,
    activeTabIndex: Int,
    onActiveTabIndexChange: (Int) -> Unit,
    nextTabNumber: Int,
    onNextTabNumberChange: (Int) -> Unit,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    onRunProject: (tabId: String, folderId: String?, fileName: String) -> Unit,
    onResolveAiSee: (locationId: String?, argument: String) -> CodeEntry?,
    onSetAiScope: (CodeEntry) -> Unit,
) {
    val deviceLabel = remember { Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() } ?: "Phone" }
    var showNewTabMenu by remember { mutableStateOf(false) }

    val activeTab = tabs[activeTabIndex]

    fun promptFor(locationId: String?): String {
        val entry = entries.firstOrNull { it.id == locationId }
        val suffix = entry?.let { "\\" + addressFor(entries, it) }.orEmpty()
        return "A:\\$deviceLabel$suffix>"
    }

    // Whatever the ACTIVE tab is pointed at is what Documents' + button and
    // Paste default to — switching tabs or navigating within one propagates
    // up immediately.
    LaunchedEffect(activeTab.locationId, activeTabIndex) { onActiveLocationChanged(activeTab.locationId) }

    fun updateActiveTab(transform: (CommandTab) -> CommandTab) {
        onTabsChange(tabs.mapIndexed { index, tab -> if (index == activeTabIndex) transform(tab) else tab })
    }

    fun submitCommand() {
        val trimmed = commandInput.trim()
        if (trimmed.isEmpty()) return
        val promptLine = TerminalLine("${promptFor(activeTab.locationId)} $trimmed")
        val adArgument = when {
            trimmed.equals("ad", ignoreCase = true) -> ""
            trimmed.startsWith("ad ", ignoreCase = true) -> trimmed.substring(3)
            else -> null
        }
        val runArgument = if (trimmed.startsWith("run ", ignoreCase = true)) trimmed.substring(4).trim() else null
        val aiSeeArgument = if (trimmed.startsWith("ai see ", ignoreCase = true)) trimmed.substring(7).trim() else null
        when {
            adArgument != null -> {
                val result = onResolveAd(activeTab.locationId, adArgument)
                val resultLine = if (result.error != null) {
                    TerminalLine(result.error, isError = true)
                } else {
                    TerminalLine("Now in ${promptFor(result.newLocationId)}")
                }
                updateActiveTab { it.copy(locationId = result.newLocationId, transcript = it.transcript + promptLine + resultLine) }
            }
            !runArgument.isNullOrBlank() -> {
                updateActiveTab { it.copy(transcript = it.transcript + promptLine) }
                onRunProject(activeTab.id, activeTab.locationId, runArgument)
            }
            !aiSeeArgument.isNullOrBlank() -> {
                val match = onResolveAiSee(activeTab.locationId, aiSeeArgument)
                val resultLine = if (match == null) {
                    TerminalLine("No file or folder named \"$aiSeeArgument\" here.", isError = true)
                } else {
                    onSetAiScope(match)
                    TerminalLine("AI is now scoped to ${match.name}${if (match.isFolder) "/" else ""} — see the banner near the ? button, or Cancel there to go back to normal.")
                }
                updateActiveTab { it.copy(transcript = it.transcript + promptLine + resultLine) }
            }
            else -> {
                updateActiveTab { it.copy(transcript = it.transcript + promptLine + TerminalLine("'$trimmed' is not recognized. Try: ad foldername, run filename, run foldername, or ai see name", isError = true)) }
            }
        }
        onCommandInputChange("")
    }

    fun createNewTab() {
        val label = nextTabNumber.toString()
        val newTabs = tabs + CommandTab(id = label, label = label)
        onTabsChange(newTabs)
        onActiveTabIndexChange(newTabs.size - 1)
        onNextTabNumberChange(nextTabNumber + 1)
        showNewTabMenu = false
    }

    fun closeTab(index: Int) {
        if (tabs.size <= 1) return
        val newTabs = tabs.filterIndexed { i, _ -> i != index }
        onTabsChange(newTabs)
        onActiveTabIndexChange(
            when {
                activeTabIndex > index -> activeTabIndex - 1
                activeTabIndex >= newTabs.size -> newTabs.size - 1
                else -> activeTabIndex
            },
        )
    }

    // One shared scroll for the whole tab (terminal + stdin together) instead
    // of the terminal owning its own separate internal scroll with stdin
    // sitting outside any scroll below it - otherwise stdin's input box could
    // end up covered by the keyboard with no way to scroll down to it.
    // imePadding(): MainActivity uses enableEdgeToEdge(), so the keyboard
    // doesn't auto-shrink content without this.
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(pageScrollState),
    ) {
        Text("COMMAND", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "Three commands: ad foldername (or ad ..) moves where Explorer/Pad's + button lands next, same idea as cd. run filename runs that file plus every other file in this folder as one project, so local imports between them work — or run foldername runs every file inside that folder as one project, auto-picking the entry file. ai see name scopes the AI tab to just that file or folder (see the banner near the ? button). No shell, so things like npm install don't run here.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                tabs.forEachIndexed { index, tab ->
                    CommandTabChip(
                        label = "Tab ${tab.label}",
                        selected = index == activeTabIndex,
                        onClick = { onActiveTabIndexChange(index) },
                        onClose = if (tabs.size > 1) ({ closeTab(index) }) else null,
                    )
                }
            }
            DocumentsToolbarButton("+", modifier = Modifier.padding(start = 8.dp), onClick = { showNewTabMenu = !showNewTabMenu })
        }
        if (showNewTabMenu) {
            DocumentsMenuPanel {
                DocumentsMenuRow("New Tab") { createNewTab() }
            }
        }

        // The terminal itself — a real console box: black background,
        // monospace, transcript and the live input line flowing together
        // (scrolling happens on the shared page scroll above, not its own) —
        // auto-scrolled to the bottom whenever a new line lands or the tab
        // switches.
        LaunchedEffect(activeTab.transcript.size, activeTabIndex) {
            pageScrollState.animateScrollTo(pageScrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            activeTab.transcript.forEach { line ->
                Text(
                    line.text,
                    color = if (line.isError) CedalColors.Error else TerminalGreen,
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 15.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${promptFor(activeTab.locationId)} ",
                    color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                )
                BasicTextField(
                    value = commandInput,
                    onValueChange = onCommandInputChange,
                    textStyle = TextStyle(color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(TerminalGreen),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitCommand() }),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        var stdinExpanded by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 6.dp)
                .clickable { stdinExpanded = !stdinExpanded },
        ) {
            Text(
                if (stdinExpanded) "▼" else "▶",
                color = CedalColors.TextSecondary, fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text("STDIN", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        }
        if (stdinExpanded) {
            Text(
                "Anything your program reads via input() goes here — like typing at a command prompt before it runs. Optional.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            ) {
                if (stdin.isEmpty()) {
                    Text("Anything your program reads as input", color = CedalColors.TextSecondary, fontSize = 12.sp)
                }
                BasicTextField(
                    value = stdin,
                    onValueChange = onStdinChange,
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
            }
        }
    }
}

@Composable
private fun OutputDestinationPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) CedalColors.Background else CedalColors.TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) CedalColors.AccentCyan else CedalColors.CardBackground)
            .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun CommandTabChip(label: String, selected: Boolean, onClick: () -> Unit, onClose: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) CedalColors.AccentCyan else CedalColors.CardBackground)
            .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (selected) CedalColors.Background else CedalColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        if (onClose != null) {
            Text(
                " ×",
                color = if (selected) CedalColors.Background else CedalColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            )
        }
    }
}

// --- Documents ---

// Copy queues a clone-on-paste; Cut queues a move-on-paste. Held as a single
// slot (not a real multi-item clipboard) — simple, and enough for "grab one
// thing, drop it somewhere else."
private data class Clipboard(val entryId: String, val entryName: String, val isCut: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeDocumentsBody(
    hasFolder: Boolean,
    loadingTree: Boolean,
    treeError: String?,
    onChooseFolder: () -> Unit,
    entries: List<CodeEntry>,
    enteredId: String?,
    defaultParentId: String?,
    onEnter: (String) -> Unit,
    onCreate: (isFolder: Boolean, parentId: String?, onCreated: (String) -> Unit) -> Unit,
    onImport: (name: String, content: String, parentId: String?, onImported: (String) -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onMove: (String, String?) -> Unit,
    onDuplicate: (String, String?) -> Unit,
    githubStatus: GithubStatusDto?,
    githubRepos: List<GithubRepoDto>,
    githubRepoPickerOpen: Boolean,
    githubSyncing: Boolean,
    githubSyncError: String?,
    githubSyncNotice: String?,
    githubConflicts: List<SyncConflictDto>,
    onConnectGithub: () -> Unit,
    onOpenGithubRepoPicker: () -> Unit,
    onCloseGithubRepoPicker: () -> Unit,
    onSelectGithubRepo: (GithubRepoDto) -> Unit,
    onDisconnectGithub: () -> Unit,
    onSyncGithub: () -> Unit,
    onResolveGithubConflict: (SyncConflictDto, Boolean) -> Unit,
    onDismissGithubNotice: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportingEntry by remember { mutableStateOf<CodeEntry?>(null) }
    var clipboard by remember { mutableStateOf<Clipboard?>(null) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showGithubMenu by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    // Long-press any row to enter this mode - subsequent taps (on that row
    // or others) toggle membership instead of the normal select/expand
    // behavior. Empty set = not in multi-select mode at all.
    var multiSelectIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val entry = exportingEntry
        if (uri != null && entry != null) {
            scope.launch {
                val content = CodeStorage.readText(context, Uri.parse(entry.id))
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            }
        }
        exportingEntry = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val name = queryDisplayName(context, uri) ?: "imported.txt"
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    onImport(name, content, targetParentFor(entries, selectedId)) { newId -> selectedId = newId }
                    actionError = null
                } catch (e: Exception) {
                    actionError = e.message ?: "Couldn't import that file"
                }
            }
        }
    }

    if (!hasFolder) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("EXPLORER", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                "Pick a real folder on your phone (like Documents) to store Code files in. Everything you make lives there for real — it'll survive closing the app, and you can find it in your phone's own Files app.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
            )
            CedalGhostButton(text = "Choose Folder", onClick = onChooseFolder)
        }
        return
    }
    if (loadingTree) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.padding(top = 40.dp))
        }
        return
    }
    if (treeError != null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("EXPLORER", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(treeError, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
            CedalGhostButton(text = "Choose a Different Folder", onClick = onChooseFolder)
        }
        return
    }

    val selectedEntry = entries.firstOrNull { it.id == selectedId }
    // No explicit selection in Documents falls back to Command's "ad"
    // current folder instead of always meaning root — that's what makes
    // navigating in Command actually change where new things land here.
    val targetParentId = if (selectedId == null) defaultParentId else targetParentFor(entries, selectedId)
    val rows = visibleRows(entries, expandedFolders)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        Text("EXPLORER", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "A real file explorer, saved to your phone. Tap + to add where your selection is, tap ⋮ to cut/copy/paste/enter/export/import. Double-tap a name to rename it.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp),
        )

        if (multiSelectIds.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    "${multiSelectIds.size} selected", color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                DocumentsToolbarButton("All", onClick = { multiSelectIds = entries.map { it.id }.toSet() })
                DocumentsToolbarButton("Copy", modifier = Modifier.padding(start = 8.dp), onClick = {
                    multiSelectIds.forEach { id -> entries.firstOrNull { it.id == id }?.let { onDuplicate(it.id, it.parentId) } }
                    multiSelectIds = emptySet()
                })
                DocumentsToolbarButton("Delete", modifier = Modifier.padding(start = 8.dp), onClick = {
                    multiSelectIds.forEach { onDelete(it) }
                    multiSelectIds = emptySet()
                })
                DocumentsToolbarButton("✕", modifier = Modifier.padding(start = 8.dp), onClick = { multiSelectIds = emptySet() })
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                DocumentsToolbarButton("+", onClick = { showCreateMenu = !showCreateMenu; showActionsMenu = false; showGithubMenu = false })
                DocumentsToolbarButton("⋮", modifier = Modifier.padding(start = 8.dp), onClick = { showActionsMenu = !showActionsMenu; showCreateMenu = false; showGithubMenu = false })
                DocumentsToolbarButton("GitHub", modifier = Modifier.padding(start = 8.dp), onClick = { showGithubMenu = !showGithubMenu; showCreateMenu = false; showActionsMenu = false })
            }
        }

        if (showCreateMenu) {
            DocumentsMenuPanel {
                DocumentsMenuRow("Folder") { onCreate(true, targetParentId) { id -> selectedId = id }; showCreateMenu = false }
                DocumentsMenuRow("File") { onCreate(false, targetParentId) { id -> selectedId = id }; showCreateMenu = false }
            }
        }
        if (showActionsMenu) {
            DocumentsMenuPanel {
                DocumentsMenuRow("Cut", enabled = selectedEntry != null) {
                    selectedEntry?.let { clipboard = Clipboard(it.id, it.name, isCut = true) }; showActionsMenu = false
                }
                DocumentsMenuRow("Copy", enabled = selectedEntry != null) {
                    selectedEntry?.let { clipboard = Clipboard(it.id, it.name, isCut = false) }; showActionsMenu = false
                }
                DocumentsMenuRow("Paste", enabled = clipboard != null) {
                    clipboard?.let { clip -> if (clip.isCut) onMove(clip.entryId, targetParentId) else onDuplicate(clip.entryId, targetParentId) }
                    clipboard = null
                    showActionsMenu = false
                }
                DocumentsMenuRow("Edit", enabled = selectedEntry != null) {
                    selectedEntry?.let { renamingId = it.id; renameText = it.name }
                    showActionsMenu = false
                }
                DocumentsMenuRow("Enter", enabled = selectedEntry != null) {
                    selectedEntry?.let { onEnter(it.id) }; showActionsMenu = false
                }
                DocumentsMenuRow("Export", enabled = selectedEntry?.isFolder == false) {
                    selectedEntry?.let { exportingEntry = it; createDocumentLauncher.launch(it.name.ifBlank { "untitled.txt" }) }
                    showActionsMenu = false
                }
                // Hands off to Android's normal share sheet - any app that
                // accepts a shared file (messaging apps, email, etc.), same
                // idea as the chat thread's own "Export Chat" share flow.
                // Files only (same restriction Export already has) - a
                // folder has no single blob to hand over without zipping it.
                DocumentsMenuRow("Share", enabled = selectedEntry?.isFolder == false) {
                    val entry = selectedEntry
                    showActionsMenu = false
                    if (entry != null) {
                        scope.launch {
                            try {
                                val content = CodeStorage.readText(context, Uri.parse(entry.id))
                                val dir = java.io.File(context.cacheDir, "code_shares").apply { mkdirs() }
                                val file = java.io.File(dir, entry.name.ifBlank { "untitled.txt" })
                                file.writeText(content)
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share ${entry.name}"))
                            } catch (e: Exception) {
                                actionError = e.message ?: "Couldn't share that file"
                            }
                        }
                    }
                }
                DocumentsMenuRow("Import") { importLauncher.launch("*/*"); showActionsMenu = false }
                DocumentsMenuRow("Delete", enabled = selectedEntry != null) {
                    selectedEntry?.let { onDelete(it.id) }; showActionsMenu = false
                }
            }
        }

        if (showGithubMenu) {
            DocumentsMenuPanel {
                if (githubRepoPickerOpen) {
                    Text(
                        "Choose a repo", color = CedalColors.TextMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (githubRepos.isEmpty()) {
                        Text(
                            "No repos found on your GitHub account.", color = CedalColors.TextMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    githubRepos.forEach { repo ->
                        DocumentsMenuRow("${repo.owner}/${repo.name}") { onSelectGithubRepo(repo) }
                    }
                    DocumentsMenuRow("Cancel") { onCloseGithubRepoPicker(); showGithubMenu = false }
                } else {
                    val status = githubStatus
                    if (status == null || !status.connected) {
                        DocumentsMenuRow("Connect GitHub") { onConnectGithub(); showGithubMenu = false }
                    } else if (status.selectedRepo == null) {
                        Text(
                            "Connected as ${status.githubLogin}", color = CedalColors.TextMuted, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                        )
                        DocumentsMenuRow("Choose Repo") { onOpenGithubRepoPicker() }
                        DocumentsMenuRow("Disconnect") { onDisconnectGithub(); showGithubMenu = false }
                    } else {
                        DocumentsMenuRow("Sync with ${status.selectedOwner}/${status.selectedRepo}", enabled = !githubSyncing) {
                            onSyncGithub(); showGithubMenu = false
                        }
                        DocumentsMenuRow("Choose a Different Repo") { onOpenGithubRepoPicker() }
                        DocumentsMenuRow("Disconnect") { onDisconnectGithub(); showGithubMenu = false }
                    }
                }
            }
        }

        if (githubSyncing) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CedalColors.AccentCyan)
                Text("Syncing with GitHub…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
        (githubSyncNotice ?: githubSyncError)?.let { message ->
            Text(
                message, color = if (githubSyncError != null) CedalColors.Error else CedalColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissGithubNotice),
            )
        }
        githubConflicts.forEach { conflict ->
            GithubConflictRow(conflict, onKeepLocal = { onResolveGithubConflict(conflict, true) }, onKeepGithub = { onResolveGithubConflict(conflict, false) })
        }

        clipboard?.let { clip ->
            Text(
                "${if (clip.isCut) "Cut" else "Copied"}: ${clip.entryName} — Paste via ⋮ wherever you want it.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        CedalErrorText(actionError)

        if (rows.isEmpty()) {
            Text("Nothing here yet — tap + to add a file or folder.", color = CedalColors.TextMuted, fontSize = 12.sp)
        }

        // weight(1f) so this bounds itself to whatever's actually left on
        // screen (shrunk further by imePadding() above while the keyboard's
        // up for a rename) instead of a plain, unbounded LazyColumn just
        // extending past the visible area with no way to scroll the rest of
        // it into view - which is exactly what made the bottom row(s)
        // unreachable while renaming with the keyboard open.
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows, key = { it.first.id }) { (entry, depth) ->
                val language = if (!entry.isFolder) languageFromFilename(entry.name) else null
                val isExpanded = entry.id in expandedFolders
                val isEmpty = entry.isFolder && entries.none { it.parentId == entry.id }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 20).dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (entry.id == selectedId || entry.id in multiSelectIds) CedalColors.Background else CedalColors.CardBackground)
                        .border(
                            1.dp,
                            when {
                                entry.id in multiSelectIds -> CedalColors.AccentCyan
                                entry.id == selectedId -> CedalColors.Success
                                entry.id == enteredId -> CedalColors.AccentCyan
                                else -> CedalColors.BorderSlate
                            },
                            RoundedCornerShape(14.dp),
                        )
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (multiSelectIds.isNotEmpty()) {
                                    multiSelectIds = if (entry.id in multiSelectIds) multiSelectIds - entry.id else multiSelectIds + entry.id
                                    return@combinedClickable
                                }
                                selectedId = if (selectedId == entry.id) null else entry.id
                                if (entry.isFolder) {
                                    expandedFolders = if (isExpanded) expandedFolders - entry.id else expandedFolders + entry.id
                                }
                            },
                            onDoubleClick = { renamingId = entry.id; renameText = entry.name },
                            // Long-press any row to enter multi-select mode -
                            // that row becomes the first selected one.
                            onLongClick = { multiSelectIds = multiSelectIds + entry.id },
                        )
                        .padding(14.dp),
                ) {
                    if (renamingId == entry.id) {
                        val focusRequester = remember { FocusRequester() }
                        fun commitRename() {
                            if (renameText.isNotBlank()) onRename(entry.id, renameText.trim())
                            renamingId = null
                        }
                        BasicTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                            cursorBrush = SolidColor(CedalColors.AccentCyan),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            // No separate "Save" button — hitting the
                            // keyboard's Done/OK commits the rename directly.
                            keyboardActions = KeyboardActions(onDone = { commitRename() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (entry.isFolder) {
                                Text(
                                    if (isExpanded) "▾" else "▸",
                                    color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            Icon(
                                if (entry.isFolder) Icons.Outlined.Folder else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = CedalColors.TextMuted,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                entry.name.ifBlank { "(untitled)" },
                                color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                        }
                        if (!entry.isFolder) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                LanguagePill(language ?: "No language set")
                            }
                        } else if (isExpanded && isEmpty) {
                            Text(
                                "(empty)", color = CedalColors.TextMuted, fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp, start = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsToolbarButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, color = if (enabled) CedalColors.TextPrimary else CedalColors.TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// A small inline expanding panel styled to match the rest of the app
// (CardBackground + BorderSlate, same as every other custom-built box here)
// instead of Material3's default DropdownMenu, whose colors and shape didn't
// match Cedal's look. Appears inline below the toolbar, the same "expands in
// place" idea as tapping a folder — no floating popup positioning to fuss over.
@Composable
private fun DocumentsMenuPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp)),
        content = content,
    )
}

// Inline expanding panel (same convention as DocumentsMenuPanel, not a
// Dialog) for picking what the Code AI can see. One picker, two jobs
// depending on boundaryRootId: null browses the whole tree from
// startFolderId (picking a brand-new file/folder scope — a file selects
// immediately via onPickFile, a folder can be entered then confirmed via
// "Use this folder"); non-null keeps browsing bounded inside that folder
// (Up stops at the boundary) — this is the "switch which file a folder
// scope is currently focused on" dropdown, since a tapped file there just
// calls onPickFile too but the caller routes it to onChangeAiScopeView
// instead of a whole new scope (see CodeRulesBody's showScopePicker block).
@Composable
private fun AiScopePickerPanel(
    entries: List<CodeEntry>,
    startFolderId: String?,
    boundaryRootId: String?,
    activeFileId: String?,
    onPickFile: (CodeEntry) -> Unit,
    onUseFolder: ((CodeEntry) -> Unit)?,
    onClose: () -> Unit,
) {
    var browsingFolderId by remember { mutableStateOf(startFolderId) }
    val browsingFolder = entries.firstOrNull { it.id == browsingFolderId }
    val rows = entries.filter { it.parentId == browsingFolderId }.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    val canGoUp = browsingFolderId != boundaryRootId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            browsingFolder?.let { relativePath(entries, null, it) }?.ifBlank { "Top level" } ?: "Top level",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp),
        )
        if (canGoUp) {
            Text(
                "⬆ Back",
                color = CedalColors.AccentCyan, fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { browsingFolderId = browsingFolder?.parentId }
                    .padding(vertical = 6.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(rows, key = { it.id }) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (entry.isFolder) browsingFolderId = entry.id else onPickFile(entry)
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text(if (entry.isFolder) "📁 " else "📄 ", fontSize = 13.sp)
                    Text(entry.name, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (!entry.isFolder && entry.id == activeFileId) {
                        Text("✓", color = CedalColors.AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (rows.isEmpty()) {
                item { Text("Nothing here.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            }
        }
        if (onUseFolder != null && browsingFolder != null) {
            DocumentsMenuRow("Use this folder (${browsingFolder.name})") { onUseFolder(browsingFolder) }
        }
        DocumentsMenuRow("Close") { onClose() }
    }
}

@Composable
private fun DocumentsMenuRow(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        color = if (enabled) CedalColors.TextPrimary else CedalColors.TextMuted,
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// A file changed both locally and on GitHub since the last sync - whole-
// file "keep local"/"keep GitHub" choice, no diff/merge view (matches this
// codebase's Documents tab convention of inline expanding panels, not
// Dialogs). Tapping the path expands both versions; only one of them is
// shown at a time by default so a long file doesn't dominate the screen.
@Composable
private fun GithubConflictRow(conflict: SyncConflictDto, onKeepLocal: () -> Unit, onKeepGithub: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.Error, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .padding(14.dp),
    ) {
        Text("Conflict: ${conflict.path}", color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Changed both on this phone and on GitHub since the last sync.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        if (expanded) {
            Text("YOUR VERSION", color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                conflict.localContent.take(2000), color = CedalColors.TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(8.dp))
                    .background(CedalColors.Background)
                    .padding(8.dp),
            )
            Text("GITHUB VERSION", color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Text(
                conflict.remoteContent.take(2000), color = CedalColors.TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(8.dp))
                    .background(CedalColors.Background)
                    .padding(8.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            DocumentsToolbarButton("Keep Local", modifier = Modifier.weight(1f), onClick = onKeepLocal)
            DocumentsToolbarButton("Keep GitHub", modifier = Modifier.weight(1f).padding(start = 8.dp), onClick = onKeepGithub)
        }
    }
}

@Composable
private fun LanguagePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = CedalColors.TextMuted, fontSize = 10.sp)
    }
}

// --- View ---

@Composable
private fun CodeViewBody(
    result: CodeRunResult?,
    runError: String?,
    running: Boolean,
    viewMessage: String?,
    language: String?,
    onExplain: suspend (String) -> Result<String>,
    androidBuildJob: AndroidBuildJob?,
    androidBuildError: String?,
    installing: Boolean,
    installError: String?,
    buildStartedAtMillis: Long?,
    onInstall: (AndroidBuildJob) -> Unit,
    onCancelBuild: () -> Unit,
    backerChecking: Boolean,
    backerReason: String?,
    backerFixedCode: String?,
    onApplyBackerFix: () -> Unit,
    onRunAnywayIgnoringBacker: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("VIEW", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "What your last run in Pad printed out.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp),
        )

        // "Backer" - runs before anything else below, across every language,
        // since it gates whether the real run/build even starts.
        if (backerChecking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                Text("Backer is checking your code…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
            }
            return@Column
        }
        if (backerReason != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text("BACKER FOUND SOMETHING", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
                Text(backerReason, color = CedalColors.TextPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 14.dp))
                if (!backerFixedCode.isNullOrBlank()) {
                    CedalPrimaryButton(text = "Apply Fix & Run", onClick = onApplyBackerFix, modifier = Modifier.padding(bottom = 8.dp))
                }
                CedalGhostButton(text = "Run Anyway", onClick = onRunAnywayIgnoringBacker)
            }
            return@Column
        }

        if (language == "Kotlin" && (running || androidBuildJob != null || androidBuildError != null)) {
            AndroidBuildStatus(
                running = running,
                job = androidBuildJob,
                buildError = androidBuildError,
                installing = installing,
                installError = installError,
                buildStartedAtMillis = buildStartedAtMillis,
                onInstall = onInstall,
                onCancel = onCancelBuild,
                scope = scope,
                onExplain = onExplain,
            )
            return@Column
        }

        if (running) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                Text("Running…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
            }
            return@Column
        }

        CedalErrorText(runError)
        if (viewMessage != null) {
            Text(viewMessage, color = CedalColors.Success, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        val r = result
        // Whichever error surface is most relevant right now is what
        // "Explain this" sends to the AI — our own failure message first,
        // otherwise the program's own stderr/compile error if it ran but
        // didn't behave.
        val errorToExplain = runError
            ?: r?.stderr?.takeIf { it.isNotBlank() }
            ?: r?.compileOutput?.takeIf { it.isNotBlank() }
        if (errorToExplain != null) {
            ExplainButton(language = language ?: "unknown", errorText = errorToExplain, scope = scope, onExplain = onExplain)
        }

        if (r == null) {
            if (runError == null && viewMessage == null) {
                Text("Nothing run yet — head to Pad and hit Run.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("OUTPUT", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("${r.language} (${r.version}) · exit ${r.exitCode ?: "?"}", color = CedalColors.TextMuted, fontSize = 10.sp)
            }
            r.compileOutput?.takeIf { it.isNotBlank() }?.let { compileOutput ->
                Text("COMPILE", color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
                MonospaceOutput(compileOutput, CedalColors.TextSecondary)
            }
            // Horizontally scrollable, unwrapped monospace — plain stdout
            // reads fine either way, but this is what actually matters for
            // anything that prints block-character ASCII art (a QR code
            // from a login-flow script, banners, tables, ...): wrapping or
            // proportional-width rendering would mangle the grid, making it
            // unreadable/unscannable.
            if (r.stdout.isNotBlank()) {
                Box(modifier = Modifier.padding(top = 10.dp)) { MonospaceOutput(r.stdout, CedalColors.TextPrimary) }
            }
            if (r.stderr.isNotBlank()) {
                Box(modifier = Modifier.padding(top = 10.dp)) { MonospaceOutput(r.stderr, CedalColors.Error) }
            }
            if (r.stdout.isBlank() && r.stderr.isBlank()) {
                Text("(no output)", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

// Kotlin's "Run" builds a real installable APK server-side (see
// AndroidBuildService) instead of executing a snippet — this shows that
// job's queued/building/done/error progress and, once done, a button that
// downloads the APK and hands it to Android's own install-confirmation flow.
// android-builder doesn't report any real incremental progress from Gradle
// itself, so this is a plain elapsed-time estimate against a typical build's
// duration - not a measurement of actual task completion. It's honest about
// that in the UI ("~" and "estimated"), but still turns "nothing happening
// for two minutes" into something that visibly moves the whole time, which
// is the actual problem it's solving.
private const val ESTIMATED_BUILD_DURATION_MS = 90_000L

@Composable
private fun AndroidBuildStatus(
    running: Boolean,
    job: AndroidBuildJob?,
    buildError: String?,
    installing: Boolean,
    installError: String?,
    buildStartedAtMillis: Long?,
    onInstall: (AndroidBuildJob) -> Unit,
    onCancel: () -> Unit,
    scope: CoroutineScope,
    onExplain: suspend (String) -> Result<String>,
) {
    // Ticks once a second only while there's actually an estimate to show -
    // no point recomposing on a timer once the build is done/errored/gone.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(buildStartedAtMillis, job?.status) {
        while (buildStartedAtMillis != null && (job?.status == "queued" || job?.status == "building")) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val estimatedPercent = buildStartedAtMillis?.let { started ->
        (((now - started).toFloat() / ESTIMATED_BUILD_DURATION_MS) * 100f).coerceIn(0f, 95f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("ANDROID BUILD", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))

        CedalErrorText(buildError)

        when (job?.status) {
            null -> if (running) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                    Text("Requesting build…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
            "queued", "building" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                    Text(
                        if (job.status == "queued") "Queued…" else "Building your app…",
                        color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp),
                    )
                }
                if (estimatedPercent != null) {
                    LinearProgressIndicator(
                        progress = { estimatedPercent / 100f },
                        color = CedalColors.AccentCyan,
                        trackColor = CedalColors.BorderSlate,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        "~${estimatedPercent.toInt()}% (estimated, not a real measurement of build progress)",
                        color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                } else {
                    Box(modifier = Modifier.padding(bottom = 10.dp))
                }
                // Client-side only: stops watching/notifying. The build
                // itself may keep running server-side a little longer -
                // there's no way yet to actually kill an in-flight Gradle
                // build on android-builder.
                CedalGhostButton(text = "Cancel", onClick = onCancel)
            }
            "error" -> {
                CedalErrorText(job.errorMessage ?: "Build failed")
                ExplainButton(language = "Kotlin", errorText = job.errorMessage ?: "Build failed", scope = scope, onExplain = onExplain)
            }
            "done" -> {
                Text("Build complete.", color = CedalColors.Success, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                CedalGhostButton(
                    text = if (installing) "Preparing install…" else "Install on this phone",
                    enabled = !installing,
                    onClick = { onInstall(job) },
                )
                CedalErrorText(installError)
            }
        }
    }
}

// On-demand only — never called automatically, since each tap is a real AI
// API call (cost + a second or two of latency). Translates whatever raw
// error/output is currently showing into a plain-English explanation.
@Composable
private fun ExplainButton(
    language: String,
    errorText: String,
    scope: CoroutineScope,
    onExplain: suspend (String) -> Result<String>,
) {
    var explanation by remember(errorText) { mutableStateOf<String?>(null) }
    var explaining by remember(errorText) { mutableStateOf(false) }
    var failure by remember(errorText) { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        CedalGhostButton(
            text = if (explaining) "Asking AI…" else "Explain this in plain English",
            enabled = !explaining,
            onClick = {
                explaining = true
                failure = null
                scope.launch {
                    onExplain(errorText)
                        .onSuccess { explanation = it }
                        .onFailure { failure = it.message }
                    explaining = false
                }
            },
        )
        explanation?.let {
            Text(
                it, color = CedalColors.TextPrimary, fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                    .padding(12.dp)
                    .fillMaxWidth(),
            )
        }
        CedalErrorText(failure)
    }
}

@Composable
private fun MonospaceOutput(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Text(
            text,
            color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp, softWrap = false,
        )
    }
}
