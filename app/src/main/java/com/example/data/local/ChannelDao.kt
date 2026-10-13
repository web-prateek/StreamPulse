package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY isFavorite DESC, id ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE title LIKE '%' || :query || '%' ORDER BY isFavorite DESC, title ASC")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY isFavorite DESC, id ASC")
    fun getChannelsByPlaylistId(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND title LIKE '%' || :query || '%' ORDER BY isFavorite DESC, title ASC")
    fun searchChannelsInPlaylist(playlistId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE groupTitle = :group ORDER BY title ASC")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE groupTitle IS NOT NULL AND groupTitle != '' ORDER BY groupTitle ASC")
    fun getDistinctGroups(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 15): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylistId(playlistId: Long)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE channels SET lastPlayedAt = :timestamp, playbackPositionMs = :positionMs WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long, positionMs: Long)

    @Query("SELECT COUNT(*) FROM channels")
    fun getTotalChannelCount(): Flow<Int>
}
