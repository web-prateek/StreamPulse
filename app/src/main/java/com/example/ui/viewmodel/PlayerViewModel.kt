package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChannelEntity
import com.example.data.repository.PlaylistRepository
import com.example.player.AspectRatioMode
import com.example.player.PlaybackUiState
import com.example.player.StreamPulsePlayer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository.getInstance(application)
    val player = StreamPulsePlayer(application)
    val playbackState: StateFlow<PlaybackUiState> = player.uiState

    val castManager = com.example.cast.CastManager(application)
    val castUiState = castManager.castUiState

    private var currentQueue: List<ChannelEntity> = emptyList()
    private var currentQueueIndex: Int = -1

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _sleepTimerRemainingSeconds = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            // Sleep timer reached 0 -> Pause stream
            player.exoPlayer.pause()
            _sleepTimerRemainingSeconds.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
    }

    init {
        player.onAutoSkipNextListener = {
            viewModelScope.launch {
                skipToNextChannel()
            }
        }
    }

    fun playChannel(channel: ChannelEntity, queue: List<ChannelEntity> = emptyList()) {
        if (queue.isNotEmpty()) {
            currentQueue = queue
            currentQueueIndex = queue.indexOfFirst { it.id == channel.id }
        } else if (currentQueue.isEmpty() || !currentQueue.any { it.id == channel.id }) {
            currentQueue = listOf(channel)
            currentQueueIndex = 0
        } else {
            currentQueueIndex = currentQueue.indexOfFirst { it.id == channel.id }
        }

        player.playChannel(channel)
        if (castUiState.value.isConnected) {
            castManager.loadMedia(channel)
        }
        viewModelScope.launch {
            repository.updateLastPlayed(channel.id, 0L)
        }
    }

    fun castCurrentChannel() {
        val current = playbackState.value.currentChannel ?: return
        castManager.loadMedia(current)
    }

    fun toggleCastPlayPause() = castManager.togglePlayPause()

    fun disconnectCast() = castManager.disconnect()

    fun skipToNextChannel() {
        viewModelScope.launch {
            if (currentQueue.isEmpty() || currentQueue.size <= 1) {
                val allChannels = repository.allChannels.first()
                if (allChannels.isNotEmpty()) {
                    currentQueue = allChannels
                    val current = playbackState.value.currentChannel
                    val currentIdx = if (current != null) allChannels.indexOfFirst { it.id == current.id } else -1
                    currentQueueIndex = (currentIdx + 1) % allChannels.size
                    val nextChannel = currentQueue[currentQueueIndex]
                    playChannel(nextChannel, currentQueue)
                }
            } else {
                currentQueueIndex = (currentQueueIndex + 1) % currentQueue.size
                val next = currentQueue[currentQueueIndex]
                playChannel(next, currentQueue)
            }
        }
    }

    fun skipToPreviousChannel(): ChannelEntity? {
        if (currentQueue.isEmpty()) return null
        currentQueueIndex = if (currentQueueIndex - 1 < 0) currentQueue.size - 1 else currentQueueIndex - 1
        val prev = currentQueue[currentQueueIndex]
        playChannel(prev, currentQueue)
        return prev
    }

    fun playDirectUrl(url: String, title: String = "Direct Stream") {
        val tempChannel = ChannelEntity(
            playlistId = 0,
            title = title,
            streamUrl = url,
            groupTitle = "Direct Stream"
        )
        currentQueue = listOf(tempChannel)
        currentQueueIndex = 0
        player.playChannel(tempChannel)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun seekBy(deltaMs: Long) = player.seekBy(deltaMs)

    fun cycleAspectRatio(): AspectRatioMode = player.cycleAspectRatio()

    fun retry() = player.retry()

    fun toggleCurrentFavorite() {
        val channel = playbackState.value.currentChannel ?: return
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite)
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
