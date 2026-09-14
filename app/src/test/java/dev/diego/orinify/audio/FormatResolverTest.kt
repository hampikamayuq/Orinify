package dev.diego.orinify.audio

import dev.diego.orinify.audio.TestAudioFormats.aac
import dev.diego.orinify.audio.TestAudioFormats.audioFormat
import dev.diego.orinify.audio.TestAudioFormats.opus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatResolverTest {
    @Test
    fun `empty candidate list has no selection`() {
        assertNull(FormatResolver.resolve(emptyList(), QualityTarget.HIGHEST))
    }

    @Test
    fun `highest target takes the best bitrate the server offered`() {
        val candidates = listOf(opus(249, 50_000), opus(251, 251_600), aac(140, 128_000))

        val selection = FormatResolver.resolve(candidates, QualityTarget.HIGHEST)

        assertEquals(251, selection?.info?.itag)
    }

    @Test
    fun `lowest target takes the cheapest bitrate`() {
        val candidates = listOf(opus(251, 251_600), opus(249, 50_000), aac(140, 128_000))

        val selection = FormatResolver.resolve(candidates, QualityTarget.LOWEST)

        assertEquals(249, selection?.info?.itag)
    }

    /**
     * The difference from [LegacyFormatPolicy]: a codec preference never promotes a format the
     * server rated lower, so HIGHEST still means the best item present.
     */
    @Test
    fun `codec preference does not override the quality target`() {
        val candidates = listOf(opus(251, 141_000), aac(140, 144_000))

        val selection = FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_OPUS)

        assertEquals(140, selection?.info?.itag)
    }

    @Test
    fun `codec preference decides when bitrates are equal`() {
        val candidates = listOf(aac(140, 128_000), opus(251, 128_000))

        assertEquals(251, FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_OPUS)?.info?.itag)
        assertEquals(140, FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.PREFER_AAC)?.info?.itag)
    }

    @Test
    fun `without a codec preference equal formats fall through to itag`() {
        val candidates = listOf(opus(251, 128_000), aac(140, 128_000))

        val selection = FormatResolver.resolve(candidates, QualityTarget.HIGHEST, CodecPreference.NONE)

        assertEquals(140, selection?.info?.itag)
    }

    @Test
    fun `formats without a reported bitrate are a last resort`() {
        val candidates = listOf(opus(251, null), aac(140, 96_000))

        assertEquals(140, FormatResolver.resolve(candidates, QualityTarget.HIGHEST)?.info?.itag)
        assertEquals(140, FormatResolver.resolve(candidates, QualityTarget.LOWEST)?.info?.itag)
    }

    @Test
    fun `a format without a bitrate is still playable when it is all there is`() {
        val candidates = listOf(opus(251, null))

        assertEquals(251, FormatResolver.resolve(candidates, QualityTarget.HIGHEST)?.info?.itag)
    }

    @Test
    fun `the choice does not depend on the order of the response`() {
        val candidates = listOf(opus(249, 50_000), opus(251, 251_600), aac(140, 128_000))

        val forward = FormatResolver.resolve(candidates, QualityTarget.HIGHEST)
        val reversed = FormatResolver.resolve(candidates.reversed(), QualityTarget.HIGHEST)

        assertEquals(forward?.info?.itag, reversed?.info?.itag)
    }

    @Test
    fun `the selected index points back at the caller's own list`() {
        val candidates = listOf(opus(249, 50_000), opus(251, 251_600))

        val selection = FormatResolver.resolve(candidates, QualityTarget.HIGHEST)

        assertEquals(1, selection?.index)
        assertEquals(candidates[1], selection?.info)
    }

    @Test
    fun `quality target follows the preference and the network`() {
        assertEquals(
            QualityTarget.HIGHEST,
            FormatResolver.qualityTargetFor(AudioQualityPreference.HIGH, NetworkProfile.METERED),
        )
        assertEquals(
            QualityTarget.LOWEST,
            FormatResolver.qualityTargetFor(AudioQualityPreference.LOW, NetworkProfile.UNMETERED),
        )
        assertEquals(
            QualityTarget.HIGHEST,
            FormatResolver.qualityTargetFor(AudioQualityPreference.AUTO, NetworkProfile.UNMETERED),
        )
        assertEquals(
            QualityTarget.LOWEST,
            FormatResolver.qualityTargetFor(AudioQualityPreference.AUTO, NetworkProfile.METERED),
        )
    }

    @Test
    fun `codec families are read from the codec string`() {
        assertEquals(CodecFamily.OPUS, codecFamilyOf("opus"))
        assertEquals(CodecFamily.AAC, codecFamilyOf("mp4a.40.2"))
        assertEquals(CodecFamily.VORBIS, codecFamilyOf("vorbis"))
        assertEquals(CodecFamily.UNKNOWN, codecFamilyOf(null))
        assertEquals(CodecFamily.UNKNOWN, codecFamilyOf("flac"))
    }

    @Test
    fun `the rationale names the chosen format and carries no transport data`() {
        val candidates = listOf(opus(251, 251_600), aac(140, 128_000))

        val rationale = FormatResolver.resolve(candidates, QualityTarget.HIGHEST)?.rationale.orEmpty()

        assertTrue(rationale, rationale.contains("itag=251"))
        assertTrue(rationale, rationale.contains("bitrate=251600"))
        assertTrue(rationale, rationale.contains("highest"))
        assertTrue(rationale, !rationale.contains("http"))
    }

    @Test
    fun `an unknown container is still selectable`() {
        val candidates = listOf(audioFormat(itag = 774, mimeType = "audio/ogg", codec = "flac", bitrate = 900_000))

        assertEquals(774, FormatResolver.resolve(candidates, QualityTarget.HIGHEST)?.info?.itag)
    }
}
