package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY isFavorite DESC, addedAt DESC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE category = :category ORDER BY addedAt DESC")
    fun getChannelsByCategory(category: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: Int)

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int
}
