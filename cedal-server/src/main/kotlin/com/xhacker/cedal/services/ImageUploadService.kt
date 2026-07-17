package com.xhacker.cedal.services

import com.google.cloud.storage.Acl
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import java.util.UUID

// Backs avatar uploads, personal custom stickers, and chat camera/gallery/
// file attachments - one reusable piece of real object storage instead of
// building it separately for each. Reuses the project's existing Firebase
// Storage bucket rather than provisioning a new one; Cloud Run's default
// service account already has project Editor, which covers writing here.
object ImageUploadService {
    private const val BUCKET = "cedal-fd4a2.firebasestorage.app"
    private const val MAX_BYTES = 25 * 1024 * 1024 // 25MB - covers a short video/PDF, not a huge file dump

    // "avatar"/"sticker" stay image-only (that's all they're ever used for);
    // "chat_media" (camera/gallery, image or video) and "chat_file" (the
    // system file picker, any type) are intentionally open - the client
    // already only offers relevant pickers, this is just the floor check.
    private val IMAGE_ONLY_KINDS = setOf("avatar", "sticker")
    val VALID_KINDS = IMAGE_ONLY_KINDS + setOf("chat_media", "chat_file")

    private val storage by lazy { StorageOptions.getDefaultInstance().service }

    fun upload(userId: String, kind: String, bytes: ByteArray, contentType: String): String {
        if (kind !in VALID_KINDS) throw AuthException("Unknown upload kind")
        if (bytes.isEmpty()) throw AuthException("Empty file")
        if (bytes.size > MAX_BYTES) throw AuthException("File too large (max 25MB)")
        if (kind in IMAGE_ONLY_KINDS && !contentType.startsWith("image/")) {
            throw AuthException("Only image uploads are allowed")
        }

        val ext = extensionFor(contentType)
        val path = "uploads/$kind/$userId/${UUID.randomUUID()}$ext"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(BUCKET, path))
            .setContentType(contentType)
            // Fine-grained ACLs (not uniform bucket access) are active on
            // this bucket - granting AllUsers READER here is what makes the
            // returned URL actually loadable by other users' devices.
            .setAcl(listOf(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
            .build()
        storage.create(blobInfo, bytes)
        return "https://storage.googleapis.com/$BUCKET/$path"
    }

    private fun extensionFor(contentType: String): String = when {
        contentType == "image/png" -> ".png"
        contentType == "image/webp" -> ".webp"
        contentType == "image/gif" -> ".gif"
        contentType.startsWith("image/") -> ".jpg"
        contentType == "video/mp4" -> ".mp4"
        contentType.startsWith("video/") -> ".webm"
        contentType == "audio/mp4" || contentType == "audio/m4a" -> ".m4a"
        contentType.startsWith("audio/") -> ".m4a"
        contentType == "application/pdf" -> ".pdf"
        else -> "" // generic files (docs, zips, etc.) keep whatever name the client sends separately
    }
}
