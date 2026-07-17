package com.xhacker.cedal.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xhacker.cedal.data.ConversationSummary
import com.xhacker.cedal.data.FriendSummary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Read-only last-known-good cache for friends/conversations - written every
// time a live fetch succeeds, read only as a fallback when a live fetch
// fails (see AuthViewModel.listFriends/listConversations). Not a real sync
// engine: nothing composed offline queues up to send later, this only ever
// makes existing, already-fetched-once data still visible without a
// connection instead of the screen just going blank.
//
// ownerUserId scopes every row to whichever account fetched it - Switch
// Account means multiple accounts' data can sit in this same on-device
// database at once, and account B must never see account A's cached rows.
@Entity(tableName = "cached_friends", primaryKeys = ["ownerUserId", "id"])
data class CachedFriendEntity(
    val ownerUserId: String,
    val id: String,
    val name: String,
    val email: String?,
    val avatarUrl: String?,
)

@Entity(tableName = "cached_conversations", primaryKeys = ["ownerUserId", "friendId"])
data class CachedConversationEntity(
    val ownerUserId: String,
    val friendId: String,
    val name: String,
    val email: String?,
    val avatarUrl: String?,
    val lastMessage: String?,
    val lastMessageAt: Long?,
    val lastMessageFromMe: Boolean?,
    val lastMessageViewOnce: Boolean = false,
)

fun FriendSummary.toEntity(ownerUserId: String) = CachedFriendEntity(ownerUserId, id, name, email, avatarUrl)
fun CachedFriendEntity.toSummary() = FriendSummary(id, name, email, avatarUrl)

fun ConversationSummary.toEntity(ownerUserId: String) =
    CachedConversationEntity(ownerUserId, friendId, name, email, avatarUrl, lastMessage, lastMessageAt, lastMessageFromMe, lastMessageViewOnce)
fun CachedConversationEntity.toSummary() =
    ConversationSummary(friendId, name, email, avatarUrl, lastMessage, lastMessageAt, lastMessageFromMe, lastMessageViewOnce)

@Dao
interface FriendCacheDao {
    @Query("SELECT * FROM cached_friends WHERE ownerUserId = :ownerUserId")
    suspend fun getAll(ownerUserId: String): List<CachedFriendEntity>

    // Returns generated row IDs rather than Unit - any suspend Room DAO
    // method (Insert/Query/Delete/Update alike) with a Unit/void return
    // trips a known Room+KSP2 bug ("unexpected jvm signature V"); giving it
    // a real return type sidesteps it entirely. Never read at the call site.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedFriendEntity>): List<Long>

    @Query("DELETE FROM cached_friends WHERE ownerUserId = :ownerUserId")
    suspend fun clearFor(ownerUserId: String): Int
}

@Dao
interface ConversationCacheDao {
    @Query("SELECT * FROM cached_conversations WHERE ownerUserId = :ownerUserId ORDER BY lastMessageAt DESC")
    suspend fun getAll(ownerUserId: String): List<CachedConversationEntity>

    // See FriendCacheDao's comment - same Room+KSP2 workaround.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedConversationEntity>): List<Long>

    @Query("DELETE FROM cached_conversations WHERE ownerUserId = :ownerUserId")
    suspend fun clearFor(ownerUserId: String): Int
}

// version 2: cached_conversations gained lastMessageViewOnce - bumping
// forces the destructive-migration fallback below to wipe any row cached
// before that column existed, which could still hold a view-once message's
// real text (see ConversationSummary/ChatService.listConversations).
@Database(entities = [CachedFriendEntity::class, CachedConversationEntity::class], version = 2, exportSchema = false)
abstract class CedalLocalDatabase : RoomDatabase() {
    abstract fun friendCacheDao(): FriendCacheDao
    abstract fun conversationCacheDao(): ConversationCacheDao
}

@Module
@InstallIn(SingletonComponent::class)
object LocalCacheModule {
    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): CedalLocalDatabase =
        Room.databaseBuilder(context, CedalLocalDatabase::class.java, "cedal_local_cache.db")
            // Fallback wipes the cache tables on a future schema bump instead
            // of crashing - acceptable since this is disposable, re-fetched
            // data, never a source of truth.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFriendCacheDao(db: CedalLocalDatabase): FriendCacheDao = db.friendCacheDao()

    @Provides
    fun provideConversationCacheDao(db: CedalLocalDatabase): ConversationCacheDao = db.conversationCacheDao()
}
