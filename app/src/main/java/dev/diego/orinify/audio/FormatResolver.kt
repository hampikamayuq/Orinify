package dev.diego.orinify.audio

/** What the user asked for, mirrored here so the domain does not depend on how it is stored. */
enum class AudioQualityPreference { AUTO, HIGH, LOW }

/** Whether the active network charges for data. */
enum class NetworkProfile { UNMETERED, METERED }

/** Which codec family to favour when two formats are otherwise equivalent. */
enum class CodecPreference { PREFER_OPUS, PREFER_AAC, NONE }

/** Whether to take the best or the cheapest format the server actually offered. */
enum class QualityTarget { HIGHEST, LOWEST }

enum class CodecFamily { OPUS, AAC, VORBIS, UNKNOWN }

/**
 * The chosen format, the reason it won, and its position in the list handed to the resolver so the
 * caller can recover its own object without matching on itag.
 */
data class FormatSelection(
    val index: Int,
    val info: AudioFormatInfo,
    val rationale: String,
)

fun codecFamilyOf(codec: String?): CodecFamily {
    val normalized = codec?.trim()?.lowercase() ?: return CodecFamily.UNKNOWN
    return when {
        normalized.startsWith("opus") -> CodecFamily.OPUS
        normalized.startsWith("mp4a") || normalized.startsWith("aac") -> CodecFamily.AAC
        normalized.startsWith("vorbis") -> CodecFamily.VORBIS
        else -> CodecFamily.UNKNOWN
    }
}

/**
 * Picks one audio format out of what the player response actually offered.
 *
 * The order is total and stable, so the same response always yields the same choice:
 *
 * 1. formats whose bitrate the server reported, before those it did not;
 * 2. bitrate, highest or lowest depending on the target;
 * 3. the preferred codec family;
 * 4. itag ascending;
 * 5. the original position, which only matters if a response repeats an itag.
 *
 * No bitrate is invented and no format is ranked above one the server rated higher, so
 * `HIGHEST` always means the best item present in the response, per docs/AUDIO_PIPELINE.md.
 * Codec preference only breaks ties: it never overrides the quality target, which is the
 * difference from the inherited rule in [LegacyFormatPolicy].
 */
object FormatResolver {
    fun qualityTargetFor(preference: AudioQualityPreference, network: NetworkProfile): QualityTarget =
        when (preference) {
            AudioQualityPreference.HIGH -> QualityTarget.HIGHEST
            AudioQualityPreference.LOW -> QualityTarget.LOWEST
            AudioQualityPreference.AUTO -> when (network) {
                NetworkProfile.METERED -> QualityTarget.LOWEST
                NetworkProfile.UNMETERED -> QualityTarget.HIGHEST
            }
        }

    fun resolve(
        candidates: List<AudioFormatInfo>,
        target: QualityTarget,
        codecPreference: CodecPreference = CodecPreference.PREFER_OPUS,
    ): FormatSelection? {
        val best = candidates.withIndex().minWithOrNull(comparatorFor(target, codecPreference)) ?: return null
        return FormatSelection(
            index = best.index,
            info = best.value,
            rationale = rationaleFor(target, codecPreference, best.value, candidates.size),
        )
    }

    private fun comparatorFor(
        target: QualityTarget,
        codecPreference: CodecPreference,
    ): Comparator<IndexedValue<AudioFormatInfo>> =
        compareBy<IndexedValue<AudioFormatInfo>> { if (it.value.bitrate == null) 1 else 0 }
            .thenBy { bitrateKey(it.value.bitrate, target) }
            .thenBy { codecRank(it.value.codec, codecPreference) }
            .thenBy { it.value.itag ?: Int.MAX_VALUE }
            .thenBy { it.index }

    private fun bitrateKey(bitrate: Long?, target: QualityTarget): Long {
        val known = bitrate ?: 0L
        return if (target == QualityTarget.HIGHEST) -known else known
    }

    private fun codecRank(codec: String?, codecPreference: CodecPreference): Int {
        val preferred = preferredFamily(codecPreference) ?: return 0
        return if (codecFamilyOf(codec) == preferred) 0 else 1
    }

    private fun preferredFamily(codecPreference: CodecPreference): CodecFamily? =
        when (codecPreference) {
            CodecPreference.PREFER_OPUS -> CodecFamily.OPUS
            CodecPreference.PREFER_AAC -> CodecFamily.AAC
            CodecPreference.NONE -> null
        }

    private fun rationaleFor(
        target: QualityTarget,
        codecPreference: CodecPreference,
        chosen: AudioFormatInfo,
        candidateCount: Int,
    ): String = buildString {
        append(if (target == QualityTarget.HIGHEST) "highest" else "lowest")
        append(" bitrate of ")
        append(candidateCount)
        append(" audio formats; itag=")
        append(chosen.itag ?: -1)
        append(" codec=")
        append(chosen.codec ?: "unknown")
        append(" bitrate=")
        append(chosen.bitrate ?: -1L)
        val preferred = preferredFamily(codecPreference)
        if (preferred != null) {
            append("; codec preference ")
            append(codecPreference.name)
            append(if (codecFamilyOf(chosen.codec) == preferred) " matched" else " not matched")
        }
    }
}
