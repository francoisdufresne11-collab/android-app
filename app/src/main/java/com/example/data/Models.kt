package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String?, // Peut être nul si le texte brut M3U a été collé
    val lastUpdated: Long = System.currentTimeMillis(),
    val channelCount: Int = 0
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val groupTitle: String, // Catégorie (ex: "Général", "Sports", etc.)
    val logoUrl: String?,
    val isFavorite: Boolean = false
)
