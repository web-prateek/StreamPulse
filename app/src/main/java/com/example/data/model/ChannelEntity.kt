package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["title"]),
        Index(value = ["playlistId"]),
        Index(value = ["groupTitle"]),
        Index(value = ["isFavorite"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val title: String,
    val streamUrl: String,
    val groupTitle: String = "General",
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0,
    val playbackPositionMs: Long = 0
)
