package dev.diego.orinify.diagnostics

import dev.diego.orinify.audio.AudioFormatInfo
import dev.diego.orinify.audio.FormatSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Counts how often the new resolver would choose a different format than the rule that actually
 * ships, without changing what plays.
 *
 * Step 3 of the integration order in docs/AUDIO_PIPELINE.md: run both policies over the same
 * response, record where they disagree, and only switch once the disagreements are understood.
 * Everything stored here comes from [AudioFormatInfo], which carries no URL, header or account
 * data by construction.
 */
class ResolverShadow(
    private val maxSamples: Int = DEFAULT_MAX_SAMPLES,
) {
    data class Divergence(
        val videoId: String,
        val legacy: AudioFormatInfo,
        val resolved: AudioFormatInfo,
        val rationale: String,
    )

    data class Snapshot(
        val comparisons: Int = 0,
        val divergences: Int = 0,
        val samples: List<Divergence> = emptyList(),
    )

    private val state = MutableStateFlow(Snapshot())

    val snapshot: StateFlow<Snapshot> = state

    /**
     * Records one comparison. A comparison where either side had nothing to choose is counted but
     * never treated as a disagreement: there is no second opinion to disagree with.
     */
    fun record(
        videoId: String,
        candidates: List<AudioFormatInfo>,
        legacyIndex: Int?,
        selection: FormatSelection?,
    ) {
        val legacy = legacyIndex?.let(candidates::getOrNull)
        val resolved = selection?.info
        val diverged = legacy != null && resolved != null && legacyIndex != selection.index
        state.update { current ->
            Snapshot(
                comparisons = current.comparisons + 1,
                divergences = current.divergences + if (diverged) 1 else 0,
                samples = if (!diverged) {
                    current.samples
                } else {
                    val sample = Divergence(
                        videoId = videoId,
                        legacy = requireNotNull(legacy),
                        resolved = requireNotNull(resolved),
                        rationale = selection.rationale,
                    )
                    (listOf(sample) + current.samples).take(maxSamples)
                },
            )
        }
    }

    fun reset() {
        state.value = Snapshot()
    }

    companion object {
        const val DEFAULT_MAX_SAMPLES = 10
    }
}
