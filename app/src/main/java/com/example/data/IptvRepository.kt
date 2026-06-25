package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class IptvRepository(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val client: OkHttpClient = OkHttpClient()
) {
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>> =
        channelDao.getChannelsForPlaylist(playlistId)

    fun getFavoritesForPlaylist(playlistId: Long): Flow<List<Channel>> =
        channelDao.getFavoritesForPlaylist(playlistId)

    val allFavorites: Flow<List<Channel>> = channelDao.getAllFavorites()

    fun getCategoriesForPlaylist(playlistId: Long): Flow<List<String>> =
        channelDao.getCategoriesForPlaylist(playlistId)

    suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            channelDao.updateFavorite(channelId, isFavorite)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            channelDao.deleteChannelsForPlaylist(playlist.id)
            playlistDao.deletePlaylist(playlist)
        }
    }

    suspend fun importPlaylistFromUrl(name: String, url: String): Result<Playlist> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Impossible de télécharger la playlist. Code d'erreur : ${response.code}"))
                    }
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.trim().isEmpty()) {
                        return@withContext Result.failure(IOException("La playlist téléchargée est vide."))
                    }
                    importPlaylistFromRawText(name, url, bodyString)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun importPlaylistFromRawText(name: String, url: String?, rawText: String): Result<Playlist> {
        return withContext(Dispatchers.IO) {
            try {
                // Étape 1 : Créer l'entité playlist temporaire
                val initialPlaylist = Playlist(name = name, url = url, channelCount = 0)
                val playlistId = playlistDao.insertPlaylist(initialPlaylist)
                
                // Étape 2 : Parser le contenu m3u
                val channels = M3uParser.parse(rawText, playlistId)
                if (channels.isEmpty()) {
                    playlistDao.deletePlaylist(initialPlaylist.copy(id = playlistId))
                    return@withContext Result.failure(IOException("Aucune chaîne de streaming valide trouvée dans la playlist."))
                }
                
                // Étape 3 : Insérer toutes les chaînes
                channelDao.insertChannels(channels)
                
                // Étape 4 : Mettre à jour avec le compte réel de chaînes
                val finalPlaylist = initialPlaylist.copy(id = playlistId, channelCount = channels.size)
                playlistDao.updatePlaylist(finalPlaylist)
                
                Result.success(finalPlaylist)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
