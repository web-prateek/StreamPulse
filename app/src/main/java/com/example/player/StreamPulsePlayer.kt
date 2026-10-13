package com.example.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.ChannelEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AspectRatioMode(val label: String, val resizeMode: Int) {
    FIT("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("16:9 Cinema", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLiveStream: Boolean = false,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val retryAttempt: Int = 0,
    val currentChannel: ChannelEntity? = null
)

class StreamPulsePlayer(private val context: Context) {

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var retryJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 2

    var onAutoSkipNextListener: (() -> Unit)? = null

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    // Playback successfully playing - reset retry count
                    retryCount = 0
                    retryJob?.cancel()
                    _uiState.update { it.copy(retryAttempt = 0, statusMessage = null, errorMessage = null) }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isLive = exoPlayer.isCurrentMediaItemLive
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L

                _uiState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        isLiveStream = isLive,
                        durationMs = duration,
                        errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage
                    )
                }

                if (playbackState == Player.STATE_READY) {
                    retryCount = 0
                    retryJob?.cancel()
                    _uiState.update { it.copy(retryAttempt = 0, statusMessage = null) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                retryJob?.cancel()

                if (retryCount < maxRetries) {
                    retryCount++
                    val currentAttempt = retryCount
                    _uiState.update {
                        it.copy(
                            isBuffering = true,
                            isPlaying = false,
                            retryAttempt = currentAttempt,
                            statusMessage = "Stream connection failed. Retrying ($currentAttempt/$maxRetries)..."
                        )
                    }

                    retryJob = playerScope.launch {
                        delay(1800) // 1.8 second delay before retry attempt
                        try {
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } catch (e: Exception) {
                            // Handler will receive another onPlayerError
                        }
                    }
                } else {
                    // Failed after 2 retries -> auto change to next stream
                    val channelTitle = _uiState.value.currentChannel?.title ?: "Stream"
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            retryAttempt = maxRetries,
                            statusMessage = "$channelTitle offline after 2 retries. Switching to next channel...",
                            errorMessage = "Channel unavailable"
                        )
                    }

                    retryJob = playerScope.launch {
                        delay(2200) // Give user visual feedback that it's switching
                        _uiState.update { it.copy(statusMessage = null) }
                        onAutoSkipNextListener?.invoke()
                    }
                }
            }
        })
    }

    fun playChannel(channel: ChannelEntity) {
        retryJob?.cancel()
        retryCount = 0

        _uiState.update {
            it.copy(
                currentChannel = channel,
                errorMessage = null,
                statusMessage = null,
                retryAttempt = 0,
                isBuffering = true
            )
        }

        try {
            val mediaItem = MediaItem.fromUri(channel.streamUrl.trim())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = "Invalid stream URL: ${e.message}"
                )
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0)
        exoPlayer.seekTo(newPos)
    }

    fun cycleAspectRatio(): AspectRatioMode {
        val current = _uiState.value.aspectRatioMode
        val values = AspectRatioMode.values()
        val next = values[(current.ordinal + 1) % values.size]
        _uiState.update { it.copy(aspectRatioMode = next) }
        return next
    }

    fun retry() {
        val channel = _uiState.value.currentChannel
        if (channel != null) {
            playChannel(channel)
        } else {
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun release() {
        retryJob?.cancel()
        playerScope.cancel()
        exoPlayer.release()
    }
}
