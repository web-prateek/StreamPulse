package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChannelEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ChannelCard(
    channel: ChannelEntity,
    isPlaying: Boolean,
    onChannelClick: (ChannelEntity) -> Unit,
    onFavoriteToggle: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLiveHls = channel.streamUrl.contains(".m3u8", ignoreCase = true) ||
            channel.streamUrl.contains("live", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) SurfaceElevated.copy(alpha = 0.95f) else SurfaceElevated
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                color = if (isPlaying) NeonCyan else SurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onChannelClick(channel) }
            .testTag("channel_item_${channel.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Channel Logo / Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1520))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${channel.title} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(38.dp)
                            .padding(2.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = "Default TV icon",
                        tint = NeonCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel Title, Group and Status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.title,
                        color = if (isPlaying) NeonCyan else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isPlaying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Group Badge
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = channel.groupTitle,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Stream format / Live badge
                    Surface(
                        color = if (isLiveHls) LiveRed.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isLiveHls) "LIVE HLS" else "STREAM",
                            color = if (isLiveHls) LiveRed else EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorite button
            IconButton(
                onClick = { onFavoriteToggle(channel) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("favorite_toggle_${channel.id}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (channel.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (channel.isFavorite) Color(0xFFFFD700) else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Quick Play Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) NeonCyan else Color(0xFF0F172A))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play stream",
                    tint = if (isPlaying) Color(0xFF00363D) else NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
