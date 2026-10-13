package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.formatSleepTimer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.example.player.AspectRatioMode
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun CinemaPlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val castUiState by playerViewModel.castUiState.collectAsStateWithLifecycle()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()

    var areControlsVisible by remember { mutableStateOf(!isInPipMode) }
    var showCastDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Intercept hardware/system back button
    BackHandler(enabled = !isInPipMode) {
        onBack()
    }

    // Auto-hide HUD overlay controls after 4.5 seconds of user inactivity
    LaunchedEffect(areControlsVisible, playbackState.isPlaying, isInPipMode) {
        if (areControlsVisible && playbackState.isPlaying && !isInPipMode) {
            delay(4500)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                enabled = !isInPipMode,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
            .testTag("cinema_player_screen")
    ) {
        // Media3 ExoPlayer Native Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerViewModel.player.exoPlayer
                    useController = false
                    resizeMode = playbackState.aspectRatioMode.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playerViewModel.player.exoPlayer
                playerView.resizeMode = playbackState.aspectRatioMode.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator in Center
        if (playbackState.isBuffering) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Buffering Stream...",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error Banner in Center
        if (playbackState.errorMessage != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to Play Stream",
                        color = LiveRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackState.errorMessage ?: "Stream connection timed out",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { playerViewModel.retry() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF00363D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Connection", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Animated HUD Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Top Gradient Scrim & Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = playbackState.currentChannel?.title ?: "Live Channel",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = LiveRed,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = playbackState.currentChannel?.groupTitle ?: "General",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (castUiState.isConnected) {
                        Surface(
                            color = NeonCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { showCastDialog = true }
                        ) {
                            Text(
                                text = "CAST: ${castUiState.deviceName ?: "TV"}",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Google Cast button
                    com.example.cast.CastActionButton(
                        castUiState = castUiState,
                        onClick = { showCastDialog = true }
                    )

                    // Active Sleep Timer pill indicator
                    if (sleepTimerRemaining != null && sleepTimerRemaining!! > 0) {
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .clickable { showSleepTimerDialog = true }
                                .testTag("active_sleep_timer_pill")
                        ) {
                            Text(
                                text = "⏱️ ${formatSleepTimer(sleepTimerRemaining!!)}",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Sleep Timer button
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining != null) EmeraldGreen else TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Picture-in-Picture (PiP) button
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("pip_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture-in-Picture",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Aspect ratio toggle button
                    IconButton(
                        onClick = { playerViewModel.cycleAspectRatio() },
                        modifier = Modifier.testTag("aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Favorite toggle
                    val isFav = playbackState.currentChannel?.isFavorite == true
                    IconButton(
                        onClick = { playerViewModel.toggleCurrentFavorite() },
                        modifier = Modifier.testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFFD700) else TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Auto-Retry & Auto-Next Status Pill Banner
                AnimatedVisibility(
                    visible = playbackState.statusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 90.dp)
                ) {
                    Surface(
                        color = if (playbackState.retryAttempt >= 2) Color(0xFF7F1D1D).copy(alpha = 0.95f) else Color(0xFF78350F).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (playbackState.retryAttempt >= 2) LiveRed else Color(0xFFFFB300)
                        ),
                        tonalElevation = 6.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = playbackState.statusMessage ?: "",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Center Play/Pause, Channel Skip & Seek Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    // Skip Previous Channel
                    IconButton(
                        onClick = { playerViewModel.skipToPreviousChannel() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("skip_prev_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Channel",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Rewind 10s
                    IconButton(
                        onClick = { playerViewModel.seekBy(-10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("seek_back_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Primary Play/Pause Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .clickable { playerViewModel.togglePlayPause() }
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Forward 10s
                    IconButton(
                        onClick = { playerViewModel.seekBy(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("seek_forward_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Skip Next Channel
                    IconButton(
                        onClick = { playerViewModel.skipToNextChannel() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("skip_next_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Channel",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom HUD Info (Aspect Ratio Label & Stream Source)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Mode: ${playbackState.aspectRatioMode.label}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        color = Color(0xFF131A26),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HLS Low Latency",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCastDialog) {
        com.example.cast.CastDeviceDialog(
            castUiState = castUiState,
            onDismiss = { showCastDialog = false },
            onTogglePlayPauseCast = { playerViewModel.toggleCastPlayPause() },
            onDisconnect = {
                playerViewModel.disconnectCast()
                showCastDialog = false
            },
            onCastCurrentStream = {
                playerViewModel.castCurrentChannel()
                showCastDialog = false
            }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingSeconds = sleepTimerRemaining,
            onSetTimerMinutes = { minutes -> playerViewModel.setSleepTimer(minutes) },
            onCancelTimer = { playerViewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}
