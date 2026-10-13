package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChannelEntity
import com.example.data.model.PlaylistEntity
import com.example.data.repository.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

data class LibraryUiState(
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null) // null = All
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctGroups: StateFlow<List<String>> = repository.distinctGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<ChannelEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalChannelCount: StateFlow<Int> = repository.totalChannelCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredChannels: StateFlow<List<ChannelEntity>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchChannels(query)
        }
        .combine(_selectedCategory) { channels, category ->
            when (category) {
                null, "All" -> channels
                "Favorites" -> channels.filter { it.isFavorite }
                else -> channels.filter { it.groupTitle.equals(category, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureInitialDataLoaded()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _uiState.value = _uiState.value.copy(infoMessage = "Playlist deleted")
        }
    }

    fun importFromUrl(name: String, url: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid URL")
            onComplete?.invoke(false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            val result = repository.importPlaylistFromUrl(name, url)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Imported $count channels successfully!"
                )
                onComplete?.invoke(true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load playlist: ${err.localizedMessage}"
                )
                onComplete?.invoke(false)
            }
        }
    }

    fun loadDefaultM3u(onComplete: ((Boolean) -> Unit)? = null) {
        importFromUrl(
            name = com.example.data.iptv.IptvDirectory.DEFAULT_M3U_NAME,
            url = com.example.data.iptv.IptvDirectory.DEFAULT_M3U_URL,
            onComplete = onComplete
        )
    }

    fun importFromInputStream(name: String, sourceName: String, inputStream: InputStream, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.importPlaylistFromStream(name, sourceName, inputStream)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Imported $count channels from local file!"
                )
                onComplete?.invoke(true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to parse local file: ${err.localizedMessage}"
                )
                onComplete?.invoke(false)
            }
        }
    }

    fun addDirectStream(title: String, url: String, group: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Stream URL cannot be empty")
            onComplete?.invoke(false)
            return
        }

        viewModelScope.launch {
            val result = repository.addDirectStream(title, url, group)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(infoMessage = "Stream added to library!")
                onComplete?.invoke(true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = "Error adding stream: ${err.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }
}
