package com.maxrave.domain.data.entities.analytics.query

import androidx.room.ColumnInfo
import kotlinx.datetime.LocalDateTime

/**
 * One track's row count and the span of its plays, straight off `playback_event`.
 *
 * [first] and [last] are `LocalDateTime?` for the same reason [PlaybackSample.timestamp] is: the
 * column holds the local wall clock encoded as UTC, and only Room's converter — chosen by target
 * type — decodes it without re-applying the zone. Nullable because an aggregate over zero rows
 * still returns one row, with `MIN`/`MAX` as NULL.
 */
data class TrackPlayStats(
    @ColumnInfo(name = "playCount") val count: Int,
    @ColumnInfo(name = "firstPlayed") val first: LocalDateTime?,
    @ColumnInfo(name = "lastPlayed") val last: LocalDateTime?,
)
