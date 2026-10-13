package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChannelEntity
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.CinemaPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    LIBRARY("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_library"),
    PLAYLISTS("Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic, "nav_playlists")
}

class MainActivity : ComponentActivity() {

    private val isInPipModeState = mutableStateOf(false)
    private var isCinemaVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                StreamPulseApp(
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = { enterPipMode() },
                    onCinemaVisibilityChanged = { isVisible ->
                        isCinemaVisible = isVisible
                    }
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isCinemaVisible) {
            enterPipMode()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Device may not support PiP
            }
        }
    }
}

@Composable
fun StreamPulseApp(
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    onCinemaVisibilityChanged: (Boolean) -> Unit = {},
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(AppDestination.LIBRARY) }
    var isCinemaPlayerVisible by remember { mutableStateOf(false) }

    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val filteredChannels by libraryViewModel.filteredChannels.collectAsStateWithLifecycle()
    val activePlayingChannel = playbackState.currentChannel

    val isActuallyInCinema = (isCinemaPlayerVisible || isInPipMode) && activePlayingChannel != null

    LaunchedEffect(isActuallyInCinema) {
        onCinemaVisibilityChanged(isActuallyInCinema)
    }

    fun handleChannelSelect(channel: ChannelEntity) {
        playerViewModel.playChannel(channel, filteredChannels)
        isCinemaPlayerVisible = true
    }

    fun handleDirectPlay(url: String, title: String) {
        playerViewModel.playDirectUrl(url, title)
        isCinemaPlayerVisible = true
    }

    // Cinema Fullscreen Player Layer
    if (isActuallyInCinema && activePlayingChannel != null) {
        CinemaPlayerScreen(
            playerViewModel = playerViewModel,
            onBack = { isCinemaPlayerVisible = false },
            isInPipMode = isInPipMode,
            onEnterPip = onEnterPip
        )
    } else {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(SurfaceDark)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Mini player bar if playing and full screen player is minimized
                    AnimatedVisibility(
                        visible = activePlayingChannel != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        val castState by playerViewModel.castUiState.collectAsStateWithLifecycle()
                        MiniPlayerBar(
                            playbackState = playbackState,
                            isCasting = castState.isConnected,
                            castDeviceName = castState.deviceName,
                            onExpandToFullPlayer = { isCinemaPlayerVisible = true },
                            onTogglePlayPause = { playerViewModel.togglePlayPause() },
                            onCloseStream = { playerViewModel.player.exoPlayer.stop() }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        AppDestination.values().forEach { destination ->
                            val isSelected = currentDestination == destination

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(destination.tag)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            viewModel = libraryViewModel,
                            activePlayingChannelId = activePlayingChannel?.id,
                            onChannelSelect = { handleChannelSelect(it) },
                            onDirectPlayUrl = { url, title -> handleDirectPlay(url, title) },
                            onNavigateToCategory = { category ->
                                libraryViewModel.selectCategory(category)
                                currentDestination = AppDestination.LIBRARY
                            }
                        )
                    }

                    AppDestination.LIBRARY -> {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            activePlayingChannelId = activePlayingChannel?.id,
                            onChannelSelect = { handleChannelSelect(it) },
                            onDirectPlayUrl = { url, title -> handleDirectPlay(url, title) }
                        )
                    }

                    AppDestination.PLAYLISTS -> {
                        PlaylistsScreen(
                            viewModel = libraryViewModel,
                            onPlaylistClick = { playlist ->
                                libraryViewModel.selectCategory(null)
                                currentDestination = AppDestination.LIBRARY
                            }
                        )
                    }
                }
            }
        }
    }
}
