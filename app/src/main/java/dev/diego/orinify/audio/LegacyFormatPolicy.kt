package dev.diego.orinify.audio

/**
 * The selection rule inherited from InnerTune, kept so the new [FormatResolver] can be compared
 * against exactly what ships today.
 *
 * This is not a recommendation. It scores a format as `bitrate * sign` plus a flat 10240 bonus for
 * any WebM container, which is an indirect way of preferring Opus: it lets a lower bitrate win
 * whenever the gap is under the bonus, and the bonus has no relation to how the two codecs
 * actually compare. Replacing it is the point of phase 2, and keeping it here is what makes the
 * shadow comparison honest.
 */
object LegacyFormatPolicy {
    const val WEBM_BITRATE_BONUS = 10_240L

    /** Index of the format the inherited rule would choose, or null when there is nothing to pick. */
    fun select(
        candidates: List<AudioFormatInfo>,
        preference: AudioQualityPreference,
        network: NetworkProfile,
    ): Int? {
        if (candidates.isEmpty()) return null
        val sign = when (preference) {
            AudioQualityPreference.HIGH -> 1L
            AudioQualityPreference.LOW -> -1L
            AudioQualityPreference.AUTO -> if (network == NetworkProfile.METERED) -1L else 1L
        }
        // maxByOrNull keeps the first of equal scores, which is what the inherited code did.
        return candidates.withIndex().maxByOrNull { (_, info) ->
            (info.bitrate ?: 0L) * sign + if (info.mimeType.startsWith("audio/webm")) WEBM_BITRATE_BONUS else 0L
        }?.index
    }
}
