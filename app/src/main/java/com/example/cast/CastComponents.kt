package com.example.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.mediarouter.app.MediaRouteButton
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun SystemCastButton(
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            MediaRouteButton(ctx).apply {
                try {
                    CastButtonFactory.setUpMediaRouteButton(ctx, this)
                } catch (e: Exception) {
                    // Ignored if Google Play Services is missing
                }
            }
        },
        modifier = modifier.size(36.dp)
    )
}

@Composable
fun CastActionButton(
    castUiState: CastUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag("cast_action_button")
    ) {
        Icon(
            imageVector = if (castUiState.isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
            contentDescription = if (castUiState.isConnected) "Casting to ${castUiState.deviceName}" else "Cast stream to TV",
            tint = if (castUiState.isConnected) NeonCyan else TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun CastDeviceDialog(
    castUiState: CastUiState,
    onDismiss: () -> Unit,
    onTogglePlayPauseCast: () -> Unit,
    onDisconnect: () -> Unit,
    onCastCurrentStream: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("cast_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (castUiState.isConnected) NeonCyan.copy(alpha = 0.2f) else SurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (castUiState.isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = null,
                                tint = if (castUiState.isConnected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Cast",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (castUiState.isConnected) "Connected to display" else "Cast stream to display",
                                color = if (castUiState.isConnected) EmeraldGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (castUiState.isConnected) {
                    // Connected Device Card
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = castUiState.deviceName ?: "Living Room Display",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (castUiState.isPlaying) "Playing M3U/M3U8 Stream" else "Paused on Display",
                                        color = EmeraldGreen,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(onClick = onTogglePlayPauseCast) {
                                    Icon(
                                        imageVector = if (castUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Toggle play pause",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            if (castUiState.activeCastChannel != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Current Stream: ${castUiState.activeCastChannel.title}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect")
                        }

                        Button(
                            onClick = onCastCurrentStream,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF00363D)
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Cast Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Not connected: System route picker helper
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "Select Cast Display Device",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the Google Cast button below to discover Chromecast, Android TV, Google TV, or smart display devices on your local Wi-Fi.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Standard Android MediaRouteButton
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, CircleShape)
                            ) {
                                SystemCastButton()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
