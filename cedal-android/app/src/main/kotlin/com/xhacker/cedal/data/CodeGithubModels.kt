package com.xhacker.cedal.data

import kotlinx.serialization.Serializable

// Code area "Documents" <-> GitHub sync - byte-for-byte mirrors of
// cedal-server's models/Models.kt DTOs of the same name (this codebase's
// established convention for client/server DTOs, confirmed across every
// other feature's data/Models.kt entries).

@Serializable
data class GithubAuthorizeUrlDto(val url: String)

@Serializable
data class GithubStatusDto(
    val connected: Boolean,
    val githubLogin: String? = null,
    val selectedOwner: String? = null,
    val selectedRepo: String? = null,
    val selectedBranch: String? = null,
)

@Serializable
data class GithubRepoDto(val owner: String, val name: String, val defaultBranch: String, val private: Boolean)

@Serializable
data class SelectGithubRepoRequest(val owner: String, val repo: String, val branch: String)

@Serializable
data class CodeSyncFileEntry(val path: String, val content: String)

@Serializable
data class SyncStartRequest(val files: List<CodeSyncFileEntry>)

@Serializable
data class SyncStartResponseDto(val jobId: String)

@Serializable
data class SyncConflictDto(val path: String, val localContent: String, val remoteContent: String)

@Serializable
data class ResolveConflictRequest(val path: String, val keepLocal: Boolean, val localContent: String)

@Serializable
data class ResolveConflictResponseDto(val path: String, val content: String)

@Serializable
data class SyncJobDto(
    val id: String,
    val status: String, // running | done | error
    val totalFiles: Int,
    val processedFiles: Int,
    val pushed: List<String> = emptyList(),
    val pulled: List<CodeSyncFileEntry> = emptyList(),
    val deletedRemote: List<String> = emptyList(),
    val deletedLocal: List<String> = emptyList(),
    val conflicts: List<SyncConflictDto> = emptyList(),
    val errorMessage: String? = null,
)
