package com.example.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.ChannelEntity
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CastUiState(
    val isCastAvailable: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isPlaying: Boolean = false,
    val deviceName: String? = null,
    val activeCastChannel: ChannelEntity? = null
)

class CastManager(private val context: Context) {

    private val TAG = "CastManager"
    private var castContext: CastContext? = null
    private var currentCastSession: CastSession? = null

    private val _castUiState = MutableStateFlow(CastUiState())
    val castUiState: StateFlow<CastUiState> = _castUiState.asStateFlow()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val client = currentCastSession?.remoteMediaClient
            val isPlaying = client?.isPlaying == true
            _castUiState.update { it.copy(isPlaying = isPlaying) }
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _castUiState.update { it.copy(isConnecting = true) }
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            onSessionConnected(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _castUiState.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    deviceName = null,
                    isPlaying = false
                )
            }
        }

        override fun onSessionEnding(session: CastSession) {
            _castUiState.update { it.copy(isConnecting = true) }
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            onSessionDisconnected(session)
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _castUiState.update { it.copy(isConnecting = true) }
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            onSessionConnected(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            onSessionDisconnected(session)
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _castUiState.update { it.copy(isPlaying = false) }
        }
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )

            // Check if already in active session
            val currentSession = castContext?.sessionManager?.currentCastSession
            if (currentSession != null && currentSession.isConnected) {
                onSessionConnected(currentSession)
            } else {
                _castUiState.update { it.copy(isCastAvailable = true) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CastContext not available in this environment: ${e.message}")
            _castUiState.update { it.copy(isCastAvailable = false) }
        }
    }

    private fun onSessionConnected(session: CastSession) {
        currentCastSession = session
        val deviceName = session.castDevice?.friendlyName ?: "Cast Display"
        session.remoteMediaClient?.registerCallback(remoteMediaClientCallback)

        _castUiState.update {
            it.copy(
                isCastAvailable = true,
                isConnected = true,
                isConnecting = false,
                deviceName = deviceName
            )
        }

        // If a channel was playing locally when cast connected, load it on the cast device
        val activeChannel = _castUiState.value.activeCastChannel
        if (activeChannel != null) {
            loadMedia(activeChannel)
        }
    }

    private fun onSessionDisconnected(session: CastSession) {
        session.remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        currentCastSession = null
        _castUiState.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                deviceName = null,
                isPlaying = false
            )
        }
    }

    fun loadMedia(channel: ChannelEntity) {
        _castUiState.update { it.copy(activeCastChannel = channel) }

        val remoteMediaClient = currentCastSession?.remoteMediaClient ?: return

        try {
            val isHls = channel.streamUrl.contains(".m3u8", ignoreCase = true) ||
                    channel.streamUrl.contains("live", ignoreCase = true)

            val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, channel.title)
                putString(MediaMetadata.KEY_SUBTITLE, channel.groupTitle)
                if (!channel.logoUrl.isNullOrBlank()) {
                    addImage(WebImage(Uri.parse(channel.logoUrl)))
                }
            }

            val mediaInfo = MediaInfo.Builder(channel.streamUrl.trim())
                .setStreamType(if (isHls) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(if (isHls) "application/x-mpegURL" else "video/mp4")
                .setMetadata(movieMetadata)
                .build()

            val loadOptions = MediaLoadOptions.Builder()
                .setAutoplay(true)
                .build()

            remoteMediaClient.load(mediaInfo, loadOptions)
            _castUiState.update { it.copy(isPlaying = true) }
        } catch (e: Exception) {
            Log.e(TAG, "Error casting media to display device: ${e.message}")
        }
    }

    fun togglePlayPause() {
        val client = currentCastSession?.remoteMediaClient ?: return
        if (client.isPlaying) {
            client.pause()
        } else {
            client.play()
        }
    }

    fun seek(positionMs: Long) {
        currentCastSession?.remoteMediaClient?.seek(positionMs)
    }

    fun disconnect() {
        try {
            castContext?.sessionManager?.endCurrentSession(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting cast: ${e.message}")
        }
    }
}
