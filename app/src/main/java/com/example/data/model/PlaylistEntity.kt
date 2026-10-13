package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sourceUrl: String, // Can be remote HTTP/HTTPS URL or local file path
    val isLocal: Boolean = false,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)
