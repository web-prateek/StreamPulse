package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChannelEntity
import com.example.ui.components.ChannelCard
import com.example.ui.components.PasteUrlDialog
import com.example.ui.components.TopSearchBar
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    activePlayingChannelId: Long?,
    onChannelSelect: (ChannelEntity) -> Unit,
    onDirectPlayUrl: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val distinctGroups by viewModel.distinctGroups.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalChannelCount.collectAsStateWithLifecycle()

    var showPasteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // File picker launcher for offline M3U file selection from device storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Local Playlist"
                    viewModel.importFromInputStream(
                        name = fileName,
                        sourceName = uri.toString(),
                        inputStream = inputStream
                    )
                }
            } catch (e: Exception) {
                // Handled in ViewModel
            }
        }
    }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Search Bar (Requested Feature: allows filtering through saved entries by title)
            TopSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClearQuery = { viewModel.clearSearchQuery() },
                totalResultCount = channels.size,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Filter Chips Row
            CategoryChipsRow(
                selectedCategory = selectedCategory,
                distinctGroups = distinctGroups,
                onCategorySelect = { viewModel.selectCategory(it) }
            )

            // Content Area: Channel List or Empty State
            if (channels.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    // Empty search result
                    EmptySearchState(
                        query = searchQuery,
                        onClearSearch = { viewModel.clearSearchQuery() }
                    )
                } else {
                    // Empty library zero state
                    EmptyLibraryState(
                        onPasteUrlClick = { showPasteDialog = true },
                        onPickFileClick = { filePickerLauncher.launch("*/*") }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("channel_list")
                ) {
                    items(
                        items = channels,
                        key = { it.id }
                    ) { channel ->
                        ChannelCard(
                            channel = channel,
                            isPlaying = channel.id == activePlayingChannelId,
                            onChannelClick = { onChannelSelect(channel) },
                            onFavoriteToggle = { viewModel.toggleFavorite(channel) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to add playlist or stream URL
        FloatingActionButton(
            onClick = { showPasteDialog = true },
            containerColor = NeonCyan,
            contentColor = Color(0xFF00363D),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
                .testTag("add_playlist_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add M3U playlist",
                modifier = Modifier.size(26.dp)
            )
        }

        // Snackbar Host for status notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    if (showPasteDialog) {
        PasteUrlDialog(
            isLoading = uiState.isLoading,
            onDismiss = { showPasteDialog = false },
            onImportUrl = { name, url ->
                viewModel.importFromUrl(name, url) { success ->
                    if (success) showPasteDialog = false
                }
            },
            onPlayDirectly = { url, title ->
                onDirectPlayUrl(url, title)
                showPasteDialog = false
            }
        )
    }
}

@Composable
private fun CategoryChipsRow(
    selectedCategory: String?,
    distinctGroups: List<String>,
    onCategorySelect: (String?) -> Unit
) {
    val allOptions = remember(distinctGroups) {
        listOf("All", "Favorites") + distinctGroups.filter { it != "General" && it.isNotBlank() }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(allOptions) { option ->
            val isSelected = when (option) {
                "All" -> selectedCategory == null || selectedCategory == "All"
                else -> selectedCategory == option
            }

            Surface(
                color = if (isSelected) NeonCyan else SurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) NeonCyan else SurfaceBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        onCategorySelect(if (option == "All") null else option)
                    }
                    .testTag("filter_chip_$option")
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color(0xFF00363D) else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onClearSearch: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, SurfaceBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No channels found",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "No saved M3U entries match \"$query\". Check your spelling or clear search filter.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClearSearch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceElevated,
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_search_clear_button")
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Search Query", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    onPasteUrlClick: () -> Unit,
    onPickFileClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Library is Empty",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add your remote M3U / M3U8 stream URL or import a local playlist file from your device to start streaming.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPasteUrlClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF00363D)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_library_paste_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Paste M3U / M3U8 Stream URL", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickFileClick,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_library_local_file_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Local M3U File")
            }
        }
    }
}
