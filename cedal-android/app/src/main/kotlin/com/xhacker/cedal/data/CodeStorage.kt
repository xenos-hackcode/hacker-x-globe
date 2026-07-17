package com.xhacker.cedal.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CodeNode(
    val id: String,
    val parentId: String?,
    val name: String,
    val isFolder: Boolean,
)

// folderDocs caches a real DocumentFile reference for every folder found —
// needed because DocumentFile.createFile()/createDirectory() only work on a
// tree-aware DocumentFile obtained via .listFiles(), not one reconstructed
// from a bare uri string (DocumentFile.fromSingleUri() can't create
// children). The UI resolves "create inside folder X" by looking X up here.
data class CodeTreeResult(val nodes: List<CodeNode>, val folderDocs: Map<String, DocumentFile>)

// Backs the Code work mode's Documents tab with a real folder on the
// phone's storage (via Storage Access Framework) instead of in-memory
// state, so files genuinely persist — across backgrounding, back-button
// navigation, anything. Everything lives inside a dedicated "CedalCode"
// subfolder of whatever folder the user grants access to, so this never
// scans or touches their other files.
object CodeStorage {
    private const val ROOT_FOLDER_NAME = "CedalCode"

    suspend fun ensureRoot(context: Context, treeUriString: String): DocumentFile? = withContext(Dispatchers.IO) {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return@withContext null
        if (!tree.isDirectory) return@withContext null
        tree.findFile(ROOT_FOLDER_NAME) ?: tree.createDirectory(ROOT_FOLDER_NAME)
    }

    // Recursively mirrors the real folder tree into the flat id/parentId
    // list the UI already speaks — the CedalCode root's own contents get
    // parentId = null (the UI's "top level"); everything else's parentId
    // is its real parent's uri string.
    suspend fun listTree(root: DocumentFile): CodeTreeResult = withContext(Dispatchers.IO) {
        val nodes = mutableListOf<CodeNode>()
        val folderDocs = mutableMapOf<String, DocumentFile>()
        fun walk(doc: DocumentFile, parentId: String?) {
            doc.listFiles().forEach { child ->
                val id = child.uri.toString()
                nodes.add(CodeNode(id, parentId, child.name ?: "untitled", child.isDirectory))
                if (child.isDirectory) {
                    folderDocs[id] = child
                    walk(child, id)
                }
            }
        }
        walk(root, null)
        CodeTreeResult(nodes, folderDocs)
    }

    suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun writeText(context: Context, uri: Uri, text: String): Unit = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
        } catch (e: Exception) {
            // A debounced auto-save failing silently is preferable to
            // crashing the editor over a transient I/O hiccup.
        }
    }

    // application/octet-stream, not text/plain: SAF providers normalize a
    // created file's extension to match its declared MIME type, and
    // "text/plain" canonically maps to .txt - asking for "index.html" with
    // mimeType "text/plain" silently comes back as "index.html.txt". Since
    // every extension here is decided purely by `name`, declaring the
    // generic/unknown-binary type is what actually lets it stick.
    suspend fun createFile(parent: DocumentFile, name: String): DocumentFile? = withContext(Dispatchers.IO) {
        parent.createFile("application/octet-stream", name)
    }

    suspend fun createFolder(parent: DocumentFile, name: String): DocumentFile? = withContext(Dispatchers.IO) {
        parent.createDirectory(name)
    }

    suspend fun rename(context: Context, uri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.renameDocument(context.contentResolver, uri, newName) != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun move(context: Context, uri: Uri, oldParentUri: Uri, newParentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.moveDocument(context.contentResolver, uri, oldParentUri, newParentUri) != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun copy(context: Context, uri: Uri, newParentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.copyDocument(context.contentResolver, uri, newParentUri) != null
        } catch (e: Exception) {
            false
        }
    }
}
