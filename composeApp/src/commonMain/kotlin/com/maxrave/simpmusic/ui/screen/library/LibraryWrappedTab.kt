package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.WrappedEntryCard
import com.maxrave.simpmusic.ui.navigation.destination.home.WrappedDestination
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.WrappedUiState
import com.maxrave.simpmusic.viewModel.WrappedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_not_enough_body
import simpmusic.composeapp.generated.resources.wrapped_not_enough_days
import simpmusic.composeapp.generated.resources.wrapped_not_enough_progress
import simpmusic.composeapp.generated.resources.wrapped_not_enough_title
import simpmusic.composeapp.generated.resources.wrapped_recap_empty

/** The page margin the card sits in, matching the gutter the playlist tiles below it carry. */
private val CARD_GUTTER = 10.dp

/**
 * The Wrapped filter's page: the way in to the reel, and a recap playlist per month beneath it.
 *
 * The recaps are drawn by [GridLibraryPlaylist], the same component every other playlist list in
 * Library uses — a recap is a playlist, so it gets a playlist tile, and pull-to-refresh, the empty
 * state and the top-bar hiding all come from there rather than from a second implementation here.
 *
 * The entry card rides in that component's `header` slot, as a real grid item spanning every
 * column. It was previously drawn in a `Box` over the grid with its height reserved in
 * `contentPadding` and its position translated by the scroll offset — the measured height included
 * the top inset that was then added to it again, which is what left a screen-tall hole above the
 * tiles. A grid item cannot be double-counted.
 *
 * The header exists in every state the reel can be in, not only [WrappedUiState.Ready]: a year too
 * thin to open gets the same "not enough of the year yet" answer the reel itself gives, in a card,
 * so the tab never reads as a feature that quietly failed. Only [WrappedUiState.Loading] draws
 * nothing, because there is nothing true to say until the count is in.
 *
 * A month with no plays is absent rather than shown empty, which is decided upstream in
 * `LibraryViewModel.getMonthlyRecaps` — "Recap March" opening onto an empty list is worse than no
 * tile.
 */
@Composable
fun LibraryWrappedTab(
    navController: NavController,
    contentPadding: PaddingValues,
    recaps: LocalResource<List<MonthlyRecapItem>>,
    onScrolling: (onTop: Boolean) -> Unit = {},
    wrappedViewModel: WrappedViewModel = koinViewModel(),
    onReload: () -> Unit,
) {
    val wrappedState by wrappedViewModel.uiState.collectAsStateWithLifecycle()
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = CARD_GUTTER, vertical = 8.dp)

    GridLibraryPlaylist(
        navController = navController,
        contentPadding = contentPadding,
        data = recaps,
        emptyText = Res.string.wrapped_recap_empty,
        onScrolling = onScrolling,
        header =
            when (val state = wrappedState) {
                is WrappedUiState.Ready -> {
                    {
                        WrappedEntryCard(
                            wrapped = state.wrapped,
                            onClick = { navController.navigate(WrappedDestination) },
                            modifier = cardModifier,
                        )
                    }
                }

                is WrappedUiState.NotEnoughData -> {
                    {
                        WrappedNotEnoughCard(state = state, modifier = cardModifier)
                    }
                }

                WrappedUiState.Loading -> null
            },
        onReload = onReload,
    )
}

/**
 * [com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedNotEnoughDataScreen] at card size: the
 * same title, sentence and distance bar, without the reel's chrome. Not tappable — there is
 * nothing to open yet, and a card that looks like the entry card but does nothing on tap is the
 * one thing this must not be.
 */
@Composable
private fun WrappedNotEnoughCard(
    state: WrappedUiState.NotEnoughData,
    modifier: Modifier = Modifier,
) {
    val days = stringResource(Res.string.wrapped_not_enough_days, formatCount(state.activeDays))
    // A zero denominator is a bug upstream, not a state to draw, but a NaN width would take the
    // page down with it.
    val fraction =
        if (state.requiredDays > 0) {
            (state.activeDays.toFloat() / state.requiredDays).coerceIn(0f, 1f)
        } else {
            0f
        }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CARD_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(CARD_PADDING)) {
            Text(
                text = stringResource(Res.string.wrapped_not_enough_title),
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.wrapped_not_enough_body, days),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text =
                    stringResource(
                        Res.string.wrapped_not_enough_progress,
                        formatCount(state.activeDays),
                        formatCount(state.requiredDays),
                    ),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Same corner and inset as the entry card, so the two read as one family in either state. */
private val CARD_RADIUS = 12.dp
private val CARD_PADDING = 14.dp

/** The unlit part of the bar is the same ink as the lit part, simply held back. */
private const val TRACK_ALPHA = 0.24f
