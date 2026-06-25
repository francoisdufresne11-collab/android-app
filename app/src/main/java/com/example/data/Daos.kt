package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): Playlist?
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1 ORDER BY name ASC")
    fun getFavoritesForPlaylist(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getAllFavorites(): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: Long)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavorite(channelId: Long, isFavorite: Boolean)

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId ORDER BY groupTitle ASC")
    fun getCategoriesForPlaylist(playlistId: Long): Flow<List<String>>
}
