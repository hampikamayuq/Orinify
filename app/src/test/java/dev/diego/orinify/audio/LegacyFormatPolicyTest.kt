package dev.diego.orinify.audio

import dev.diego.orinify.audio.TestAudioFormats.aac
import dev.diego.orinify.audio.TestAudioFormats.opus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyFormatPolicyTest {
    @Test
    fun `empty list has no selection`() {
        assertNull(LegacyFormatPolicy.select(emptyList(), AudioQualityPreference.HIGH, NetworkProfile.UNMETERED))
    }

    @Test
    fun `auto on an unmetered network takes the highest bitrate`() {
        val candidates = listOf(aac(139, 48_000), aac(140, 128_000))

        val index = LegacyFormatPolicy.select(candidates, AudioQualityPreference.AUTO, NetworkProfile.UNMETERED)

        assertEquals(140, candidates[index!!].itag)
    }

    @Test
    fun `auto on a metered network takes the lowest bitrate`() {
        val candidates = listOf(aac(140, 128_000), aac(139, 48_000))

        val index = LegacyFormatPolicy.select(candidates, AudioQualityPreference.AUTO, NetworkProfile.METERED)

        assertEquals(139, candidates[index!!].itag)
    }

    @Test
    fun `high and low ignore the network`() {
        val candidates = listOf(aac(139, 48_000), aac(140, 128_000))

        assertEquals(
            140,
            candidates[LegacyFormatPolicy.select(candidates, AudioQualityPreference.HIGH, NetworkProfile.METERED)!!].itag,
        )
        assertEquals(
            139,
            candidates[LegacyFormatPolicy.select(candidates, AudioQualityPreference.LOW, NetworkProfile.UNMETERED)!!].itag,
        )
    }

    /**
     * Documents the behaviour being replaced: the flat WebM bonus lets a lower bitrate win whenever
     * the gap is under 10240, which is why the two policies have to be compared in the shadow before
     * the resolver takes over.
     */
    @Test
    fun `the webm bonus beats a higher bitrate when the gap is under the bonus`() {
        val candidates = listOf(opus(251, 141_000), aac(140, 144_000))

        val legacy = LegacyFormatPolicy.select(candidates, AudioQualityPreference.HIGH, NetworkProfile.UNMETERED)
        val resolved = FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_OPUS)

        assertEquals(251, candidates[legacy!!].itag)
        assertEquals(140, resolved?.info?.itag)
        assertNotEquals(legacy, resolved?.index)
    }

    @Test
    fun `a gap wider than the bonus is decided by bitrate alone`() {
        val candidates = listOf(opus(251, 130_000), aac(140, 144_000))

        val legacy = LegacyFormatPolicy.select(candidates, AudioQualityPreference.HIGH, NetworkProfile.UNMETERED)
        val resolved = FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_OPUS)

        assertEquals(140, candidates[legacy!!].itag)
        assertEquals(legacy, resolved?.index)
    }

    /** The bonus is added regardless of sign, so it also skews the metered and LOW cases. */
    @Test
    fun `the webm bonus also skews the low quality choice`() {
        val candidates = listOf(opus(250, 55_000), aac(139, 48_000))

        val legacy = LegacyFormatPolicy.select(candidates, AudioQualityPreference.LOW, NetworkProfile.UNMETERED)
        val resolved = FormatResolver.resolve(candidates, QualityTarget.LOWEST, CodecPreference.PREFER_OPUS)

        assertEquals(250, candidates[legacy!!].itag)
        assertEquals(139, resolved?.info?.itag)
    }
}
