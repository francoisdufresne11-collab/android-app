package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val db = IptvDatabase.getDatabase(application)
    private val repository = IptvRepository(db.playlistDao(), db.channelDao())

    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId = _selectedPlaylistId.asStateFlow()

    private val _playingChannel = MutableStateFlow<Channel?>(null)
    val playingChannel = _playingChannel.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tous")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    private val _importSuccess = MutableStateFlow(false)
    val importSuccess = _importSuccess.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories = _selectedPlaylistId.flatMapLatest { playlistId ->
        if (playlistId != null) {
            repository.getCategoriesForPlaylist(playlistId)
                .map { listOf("Tous", "Favoris") + it }
        } else {
            flowOf(listOf("Tous"))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Tous")
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredChannels = combine(
        _selectedPlaylistId,
        _selectedCategory,
        _searchQuery
    ) { playlistId, category, query ->
        Triple(playlistId, category, query)
    }.flatMapLatest { (playlistId, category, query) ->
        if (playlistId == null) return@flatMapLatest flowOf(emptyList<Channel>())

        val baseFlow = when (category) {
            "Favoris" -> repository.getFavoritesForPlaylist(playlistId)
            else -> repository.getChannelsForPlaylist(playlistId)
        }

        baseFlow.map { list ->
            list.filter { channel ->
                val matchesCategory = category == "Tous" || category == "Favoris" || channel.groupTitle == category
                val matchesSearch = query.isEmpty() || channel.name.contains(query, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
        _selectedCategory.value = "Tous"
        _searchQuery.value = ""
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playChannel(channel: Channel?) {
        _playingChannel.value = channel
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlist.id) {
                selectPlaylist(null)
                if (_playingChannel.value?.playlistId == playlist.id) {
                    playChannel(null)
                }
            }
            repository.deletePlaylist(playlist)
        }
    }

    fun resetImportStatus() {
        _importError.value = null
        _importSuccess.value = false
    }

    fun importPlaylistUrl(name: String, url: String) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _importSuccess.value = false
            
            val result = repository.importPlaylistFromUrl(name, url)
            _isImporting.value = false
            if (result.isSuccess) {
                _importSuccess.value = true
                selectPlaylist(result.getOrNull()?.id)
            } else {
                _importError.value = result.exceptionOrNull()?.message ?: "Erreur de chargement. Veuillez vérifier l'URL et votre connexion."
            }
        }
    }

    fun importPlaylistText(name: String, content: String) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _importSuccess.value = false
            
            val result = repository.importPlaylistFromRawText(name, null, content)
            _isImporting.value = false
            if (result.isSuccess) {
                _importSuccess.value = true
                selectPlaylist(result.getOrNull()?.id)
            } else {
                _importError.value = result.exceptionOrNull()?.message ?: "Échec de l'importation. Assurez-vous d'utiliser un format M3U valide."
            }
        }
    }
}
