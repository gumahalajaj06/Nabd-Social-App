package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NabdDao {

    @Query("SELECT * FROM cached_chats ORDER BY lastMessageTimestampMs DESC")
    fun observeChats(): Flow<List<CachedChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChats(chats: List<CachedChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(chat: CachedChatEntity)

    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY createdAtMs ASC")
    fun observeMessages(chatId: String): Flow<List<CachedMessageEntity>>

    @Query("SELECT * FROM cached_messages WHERE isQueuedOffline = 1 ORDER BY createdAtMs ASC")
    suspend fun getQueuedOfflineMessages(): List<CachedMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: CachedMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<CachedMessageEntity>)

    @Query("UPDATE cached_messages SET isQueuedOffline = 0, status = :status WHERE messageId = :messageId")
    suspend fun markMessageSynced(messageId: String, status: String)

    @Query("SELECT * FROM cached_posts ORDER BY createdAtMs DESC LIMIT 100")
    fun observeCachedPosts(): Flow<List<CachedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<CachedPostEntity>)

    @Query("SELECT COUNT(*) FROM cached_posts")
    suspend fun countCachedPosts(): Int

    @Query("SELECT COUNT(*) FROM cached_messages")
    suspend fun countCachedMessages(): Int

    @Query("SELECT COUNT(*) FROM cached_chats")
    suspend fun countCachedChats(): Int

    @Query("DELETE FROM cached_chats")
    suspend fun clearChats()

    @Query("DELETE FROM cached_messages")
    suspend fun clearMessages()

    @Query("DELETE FROM cached_posts")
    suspend fun clearPosts()
}
