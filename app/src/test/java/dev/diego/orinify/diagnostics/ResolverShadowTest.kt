package dev.diego.orinify.diagnostics

import dev.diego.orinify.audio.AudioFormatInfo
import dev.diego.orinify.audio.AudioQualityPreference
import dev.diego.orinify.audio.CodecPreference
import dev.diego.orinify.audio.FormatResolver
import dev.diego.orinify.audio.LegacyFormatPolicy
import dev.diego.orinify.audio.NetworkProfile
import dev.diego.orinify.audio.QualityTarget
import dev.diego.orinify.audio.TestAudioFormats.aac
import dev.diego.orinify.audio.TestAudioFormats.opus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverShadowTest {
    private val shadow = ResolverShadow(maxSamples = 2)

    @Test
    fun `agreement is counted without a sample`() {
        record(listOf(opus(251, 251_600), aac(140, 128_000)))

        val snapshot = shadow.snapshot.value
        assertEquals(1, snapshot.comparisons)
        assertEquals(0, snapshot.divergences)
        assertTrue(snapshot.samples.isEmpty())
    }

    @Test
    fun `disagreement is counted and sampled`() {
        record(listOf(opus(251, 141_000), aac(140, 144_000)))

        val snapshot = shadow.snapshot.value
        assertEquals(1, snapshot.comparisons)
        assertEquals(1, snapshot.divergences)
        assertEquals(251, snapshot.samples.single().legacy.itag)
        assertEquals(140, snapshot.samples.single().resolved.itag)
    }

    @Test
    fun `samples are bounded and keep the most recent first`() {
        val candidates = listOf(opus(251, 141_000), aac(140, 144_000))

        record(candidates, videoId = "first")
        record(candidates, videoId = "second")
        record(candidates, videoId = "third")

        val snapshot = shadow.snapshot.value
        assertEquals(3, snapshot.comparisons)
        assertEquals(3, snapshot.divergences)
        assertEquals(listOf("third", "second"), snapshot.samples.map { it.videoId })
    }

    @Test
    fun `an empty response is counted but never a disagreement`() {
        record(emptyList())

        val snapshot = shadow.snapshot.value
        assertEquals(1, snapshot.comparisons)
        assertEquals(0, snapshot.divergences)
    }

    @Test
    fun `reset clears the counters`() {
        record(listOf(opus(251, 141_000), aac(140, 144_000)))

        shadow.reset()

        assertEquals(ResolverShadow.Snapshot(), shadow.snapshot.value)
    }

    @Test
    fun `a divergence sample carries no transport data`() {
        record(listOf(opus(251, 141_000), aac(140, 144_000)))

        val sample = shadow.snapshot.value.samples.single().toString()

        assertTrue(sample, !sample.contains("http"))
        assertTrue(sample, !sample.contains("cookie", ignoreCase = true))
    }

    private fun record(candidates: List<AudioFormatInfo>, videoId: String = "video") {
        shadow.record(
            videoId = videoId,
            candidates = candidates,
            legacyIndex = LegacyFormatPolicy.select(
                candidates,
                AudioQualityPreference.HIGH,
                NetworkProfile.UNMETERED,
            ),
            selection = FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_OPUS),
        )
    }
}
