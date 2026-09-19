package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/**
 * The Wrapped reel for one calendar year.
 *
 * The year travels in the route because the entry card can offer LAST year's reel — through
 * January, or whenever the current year is still too thin to say anything — and the reel must
 * open on the year the card showed, not on whatever the view model would pick again. A year no
 * `playback_event` row can fill is not a crash: the view model composes it to `NotEnoughData`.
 */
@Serializable
data class WrappedDestination(
    val year: Int,
)
