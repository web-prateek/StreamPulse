package com.example.data.parser

import com.example.data.model.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object M3uParser {

    private val TVG_ID_REGEX = Pattern.compile("tvg-id=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_REGEX = Pattern.compile("tvg-name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)

    fun parse(inputStream: InputStream, playlistId: Long): List<ChannelEntity> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val channels = mutableListOf<ChannelEntity>()

        var currentTitle: String? = null
        var currentGroup = "General"
        var currentLogo: String? = null
        var currentTvgId: String? = null

        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                val tvgIdMatcher = TVG_ID_REGEX.matcher(trimmed)
                if (tvgIdMatcher.find()) {
                    currentTvgId = tvgIdMatcher.group(1)?.trim()
                }

                val logoMatcher = TVG_LOGO_REGEX.matcher(trimmed)
                if (logoMatcher.find()) {
                    currentLogo = logoMatcher.group(1)?.trim()
                }

                val groupMatcher = GROUP_TITLE_REGEX.matcher(trimmed)
                if (groupMatcher.find()) {
                    val group = groupMatcher.group(1)?.trim()
                    if (!group.isNullOrEmpty()) {
                        currentGroup = group
                    }
                }

                // Extract title after the comma
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    currentTitle = trimmed.substring(commaIndex + 1).trim()
                } else {
                    // Fallback to tvg-name if title missing after comma
                    val tvgNameMatcher = TVG_NAME_REGEX.matcher(trimmed)
                    if (tvgNameMatcher.find()) {
                        currentTitle = tvgNameMatcher.group(1)?.trim()
                    }
                }
            } else if (!trimmed.startsWith("#")) {
                // This is a stream URL
                val url = trimmed
                if (url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("https://", ignoreCase = true) ||
                    url.startsWith("rtsp://", ignoreCase = true) ||
                    url.startsWith("content://", ignoreCase = true) ||
                    url.startsWith("file://", ignoreCase = true) ||
                    url.startsWith("/")
                ) {
                    val title = currentTitle?.ifBlank { null }
                        ?: extractTitleFromUrl(url)

                    channels.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            title = title,
                            streamUrl = url,
                            groupTitle = currentGroup,
                            logoUrl = currentLogo?.ifBlank { null },
                            tvgId = currentTvgId?.ifBlank { null },
                            isFavorite = false
                        )
                    )

                    // Reset channel metadata for next item
                    currentTitle = null
                    currentGroup = "General"
                    currentLogo = null
                    currentTvgId = null
                }
            }

            line = reader.readLine()
        }

        return channels
    }

    private fun extractTitleFromUrl(url: String): String {
        val cleanUrl = url.substringBefore('?')
        val lastSegment = cleanUrl.substringAfterLast('/')
        return if (lastSegment.isNotBlank()) {
            lastSegment.substringBeforeLast('.')
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('+', ' ')
                .trim()
                .ifEmpty { "Stream Channel" }
        } else {
            "Stream Channel"
        }
    }
}
