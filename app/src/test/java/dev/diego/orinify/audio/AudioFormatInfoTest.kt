package dev.diego.orinify.audio

import com.zionhuang.innertube.models.response.PlayerResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AudioFormatInfoTest {
    @Test
    fun `codec parser handles quoted codec parameter`() {
        assertEquals("opus", codecFromMimeType("audio/webm; codecs=\"opus\""))
    }

    @Test
    fun `mapper excludes transport data and keeps technical metadata`() {
        val format = PlayerResponse.StreamingData.Format(
            itag = 251,
            url = "https://example.invalid/secret-url",
            mimeType = "audio/webm; codecs=\"opus\"",
            bitrate = 251_600,
            width = null,
            height = null,
            contentLength = 8_400_000,
            quality = "tiny",
            fps = null,
            qualityLabel = null,
            averageBitrate = 250_100,
            audioQuality = "AUDIO_QUALITY_HIGH",
            approxDurationMs = "272000",
            audioSampleRate = 48_000,
            audioChannels = 2,
            loudnessDb = -7.2,
            lastModified = null,
        )

        val info = format.toAudioFormatInfo(
            sourceClient = "ANDROID_MUSIC",
            isAuthenticated = true,
        )

        assertEquals(251, info.itag)
        assertEquals("audio/webm", info.mimeType)
        assertEquals("WebM", info.container)
        assertEquals("opus", info.codec)
        assertEquals(251_600L, info.bitrate)
        assertEquals(48_000, info.sampleRate)
        assertEquals(2, info.channels)
        assertEquals("ANDROID_MUSIC", info.sourceClient)
        assertFalse(info.toString().contains("secret-url"))
    }
}
