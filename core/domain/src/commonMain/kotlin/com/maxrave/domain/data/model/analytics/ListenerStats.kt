package com.maxrave.domain.data.model.analytics

import kotlinx.datetime.LocalDateTime

/**
 * What this listener's own history says about one track: how many times it has been played,
 * and when — the first and the most recent time.
 *
 * Read from `playback_event`, so it exists only while local tracking is on. `playCount`
 * includes the play in progress once that play has been logged; the caller decides whether
 * "Play 1" or "your first play" is the honest label.
 */
data class ListenerStats(
    val playCount: Int,
    val firstPlayedAt: LocalDateTime,
    val lastPlayedAt: LocalDateTime,
)
