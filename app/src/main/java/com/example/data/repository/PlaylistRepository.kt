package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.PlaylistEntity
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val database: AppDatabase) {

    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels()
    val distinctGroups: Flow<List<String>> = channelDao.getDistinctGroups()
    val recentlyPlayed: Flow<List<ChannelEntity>> = channelDao.getRecentlyPlayed(15)
    val totalChannelCount: Flow<Int> = channelDao.getTotalChannelCount()

    fun searchChannels(query: String): Flow<List<ChannelEntity>> {
        return if (query.isBlank()) {
            channelDao.getAllChannels()
        } else {
            channelDao.searchChannels(query.trim())
        }
    }

    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>> {
        return channelDao.getChannelsByGroup(group)
    }

    suspend fun getChannelById(id: Long): ChannelEntity? {
        return channelDao.getChannelById(id)
    }

    suspend fun toggleFavorite(channelId: Long, currentIsFavorite: Boolean) {
        channelDao.setFavorite(channelId, !currentIsFavorite)
    }

    suspend fun updateLastPlayed(channelId: Long, positionMs: Long) {
        channelDao.updateLastPlayed(channelId, System.currentTimeMillis(), positionMs)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            channelDao.deleteChannelsByPlaylistId(playlistId)
            playlistDao.deletePlaylistById(playlistId)
        }
    }

    /**
     * Loads the default M3U playlist (index.category.m3u) if no playlists are present
     */
    suspend fun ensureInitialDataLoaded() {
        withContext(Dispatchers.IO) {
            // Remove any legacy dummy starter data
            val dummy = playlistDao.getPlaylistByUrl("starter://default")
            if (dummy != null) {
                channelDao.deleteChannelsByPlaylistId(dummy.id)
                playlistDao.deletePlaylistById(dummy.id)
            }

            val existingDefault = playlistDao.getPlaylistByUrl(com.example.data.iptv.IptvDirectory.DEFAULT_M3U_URL)
            if (existingDefault == null) {
                importPlaylistFromUrl(
                    com.example.data.iptv.IptvDirectory.DEFAULT_M3U_NAME,
                    com.example.data.iptv.IptvDirectory.DEFAULT_M3U_URL
                )
            }
        }
    }

    /**
     * Loads the master category IPTV M3U link provided by user
     */
    suspend fun loadDefaultM3u(): Result<Int> {
        return importPlaylistFromUrl(
            com.example.data.iptv.IptvDirectory.DEFAULT_M3U_NAME,
            com.example.data.iptv.IptvDirectory.DEFAULT_M3U_URL
        )
    }

    /**
     * Imports an M3U playlist from a remote HTTP/HTTPS URL
     */
    suspend fun importPlaylistFromUrl(
        name: String,
        url: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        // Try up to 3 times (1 initial attempt + 2 retries)
        for (attempt in 1..3) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "StreamPulse/1.0 (Android)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Empty playlist response body")
                val inputStream = body.byteStream()

                val existing = playlistDao.getPlaylistByUrl(url)
                val playlistId = if (existing != null) {
                    channelDao.deleteChannelsByPlaylistId(existing.id)
                    existing.id
                } else {
                    playlistDao.insertPlaylist(
                        PlaylistEntity(
                            name = name.ifBlank { "Remote Playlist" },
                            sourceUrl = url,
                            isLocal = false
                        )
                    )
                }

                val channels = M3uParser.parse(inputStream, playlistId)
                if (channels.isEmpty()) {
                    throw Exception("No valid stream entries parsed from playlist")
                }

                // Chunk channel inserts for fast and robust batch insertion
                channels.chunked(400).forEach { chunk ->
                    channelDao.insertChannels(chunk)
                }

                val updated = PlaylistEntity(
                    id = playlistId,
                    name = name.ifBlank { "Remote Playlist" },
                    sourceUrl = url,
                    isLocal = false,
                    channelCount = channels.size,
                    lastSyncedAt = System.currentTimeMillis()
                )
                playlistDao.updatePlaylist(updated)

                return@withContext Result.success(channels.size)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 3) {
                    // Backoff delay before retry
                    kotlinx.coroutines.delay(1200L * attempt)
                }
            }
        }

        Result.failure(Exception("Failed after 2 retries: ${lastError?.localizedMessage}"))
    }

    /**
     * Imports a playlist from a local InputStream (e.g., from local storage or file picker)
     */
    suspend fun importPlaylistFromStream(
        name: String,
        sourceIdentifier: String,
        inputStream: InputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val playlistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name.ifBlank { "Local Playlist" },
                    sourceUrl = sourceIdentifier,
                    isLocal = true
                )
            )

            val channels = M3uParser.parse(inputStream, playlistId)
            channels.chunked(400).forEach { chunk ->
                channelDao.insertChannels(chunk)
            }

            val updated = PlaylistEntity(
                id = playlistId,
                name = name.ifBlank { "Local Playlist" },
                sourceUrl = sourceIdentifier,
                isLocal = true,
                channelCount = channels.size,
                lastSyncedAt = System.currentTimeMillis()
            )
            playlistDao.updatePlaylist(updated)

            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a single direct stream URL as an entry
     */
    suspend fun addDirectStream(
        title: String,
        streamUrl: String,
        groupTitle: String = "Direct Stream"
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var directPlaylist = playlistDao.getPlaylistByUrl("direct://streams")
            val playlistId = if (directPlaylist != null) {
                directPlaylist.id
            } else {
                playlistDao.insertPlaylist(
                    PlaylistEntity(
                        name = "Custom Streams",
                        sourceUrl = "direct://streams",
                        isLocal = true
                    )
                )
            }

            val channel = ChannelEntity(
                playlistId = playlistId,
                title = title.ifBlank { "Custom Stream" },
                streamUrl = streamUrl.trim(),
                groupTitle = groupTitle.ifBlank { "Direct Stream" },
                isFavorite = true
            )
            channelDao.insertChannels(listOf(channel))

            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaylistRepository? = null

        fun getInstance(context: Context): PlaylistRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = PlaylistRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
