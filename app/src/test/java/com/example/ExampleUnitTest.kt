package com.example

import com.example.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ExampleUnitTest {
    @Test
    fun testM3uParser_parsesExtinfAndUrlsCorrectly() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN US" tvg-logo="https://example.com/cnn.png" group-title="News",CNN News Live
            https://example.com/cnn/live.m3u8
            #EXTINF:-1 group-title="Sports",Sky Sports HD
            https://example.com/sports.m3u8
            #EXTINF:0,Direct MP4 Stream
            http://example.com/video.mp4
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8))
        val channels = M3uParser.parse(inputStream, playlistId = 1L)

        assertEquals(3, channels.size)

        // Channel 1
        assertEquals("CNN News Live", channels[0].title)
        assertEquals("https://example.com/cnn/live.m3u8", channels[0].streamUrl)
        assertEquals("News", channels[0].groupTitle)
        assertEquals("https://example.com/cnn.png", channels[0].logoUrl)
        assertEquals("cnn.us", channels[0].tvgId)

        // Channel 2
        assertEquals("Sky Sports HD", channels[1].title)
        assertEquals("Sports", channels[1].groupTitle)

        // Channel 3
        assertEquals("Direct MP4 Stream", channels[2].title)
        assertEquals("General", channels[2].groupTitle)
    }

    @Test
    fun testM3uParser_extractsTitleFromUrlWhenExtinfMissing() {
        val rawUrls = """
            http://example.com/live/breaking-news-stream.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rawUrls.toByteArray(Charsets.UTF_8))
        val channels = M3uParser.parse(inputStream, playlistId = 2L)

        assertEquals(1, channels.size)
        assertEquals("breaking news stream", channels[0].title)
    }

    @Test
    fun testQueueAdvancement_cyclesNextProperly() {
        val queue = listOf("Channel 1", "Channel 2", "Channel 3")
        var currentIndex = 0

        // Advance 1 -> Channel 2
        currentIndex = (currentIndex + 1) % queue.size
        assertEquals(1, currentIndex)
        assertEquals("Channel 2", queue[currentIndex])

        // Advance 2 -> Channel 3
        currentIndex = (currentIndex + 1) % queue.size
        assertEquals(2, currentIndex)
        assertEquals("Channel 3", queue[currentIndex])

        // Advance 3 -> Wrap to Channel 1
        currentIndex = (currentIndex + 1) % queue.size
        assertEquals(0, currentIndex)
        assertEquals("Channel 1", queue[currentIndex])
    }

    @Test
    fun testCastStreamTypeResolution() {
        val hlsUrl = "https://example.com/live/playlist.m3u8"
        val mp4Url = "https://example.com/video.mp4"

        fun getMimeType(url: String) = if (url.contains(".m3u8")) "application/x-mpegURL" else "video/mp4"

        assertEquals("application/x-mpegURL", getMimeType(hlsUrl))
        assertEquals("video/mp4", getMimeType(mp4Url))
    }
}
