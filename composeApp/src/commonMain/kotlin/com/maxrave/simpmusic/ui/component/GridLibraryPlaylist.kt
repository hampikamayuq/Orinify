package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.ChartItem
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.load_failed
import simpmusic.composeapp.generated.resources.retry

/** Narrowest artwork a tile may carry; cells stretch from here so the gutters stay put. */
private val MIN_THUMB = 132.dp

/**
 * The padding `HomeItemContentPlaylist` wraps around its own artwork. The tile is exactly
 * `thumbSize + 2 × TILE_INSET` wide, so a cell of that width is filled edge to edge — and the
 * inset is what separates neighbouring artworks. Must track the tile's `padding(10.dp)`.
 */
private val TILE_INSET = 10.dp

/** Where the artwork's edge lands on the page — Home's shelves sit at the same 15dp. */
private val PAGE_EDGE = 15.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> GridLibraryPlaylist(
    navController: NavController,
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    // Hoisted so a caller can read the scroll position itself — Mix for you derives its
    // top-bar frost from `index == 0 && offset == 0`, which the coarse onScrolling below
    // cannot say.
    state: LazyGridState = rememberLazyGridState(),
    onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    // A full-width block above the tiles, as a real grid item spanning every column — the same
    // mechanism the create tile and the footer below use. A caller drawing it in a Box over the
    // grid instead has to reserve its height in contentPadding and translate it by the scroll
    // offset by hand, which is what left a screen-tall hole above the Wrapped tab.
    header: (@Composable () -> Unit)? = null,
    // A full-width block after the tiles, before the end-of-page credit. Only drawn when there
    // are tiles: it belongs to the list, not to the loading, empty or error state.
    footer: (@Composable () -> Unit)? = null,
    createNewPlaylist: (() -> Unit)? = null,
    onReload: () -> Unit,
) {
    val isScrollingUp by state.isScrollingUp()

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        onRefresh = onReload,
        isRefreshing = data is LocalResource.Loading,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = data is LocalResource.Loading,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                        ),
                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                color = PullToRefreshDefaults.indicatorColor,
                maxDistance = PullToRefreshDefaults.PositionalThreshold,
            )
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layoutDirection = LocalLayoutDirection.current
            // The tile pins its own width to the artwork, so the artwork is sized from the cell
            // rather than the cell from the artwork: as many columns as fit at MIN_THUMB, then
            // every tile widened to fill its column exactly. Gutters are therefore always
            // 2 × TILE_INSET and the edge always PAGE_EDGE, whatever the window width. Adaptive
            // cells alone would leave a fixed tile floating in a wider cell, and FixedSize with
            // SpaceEvenly gave 32dp gutters at 360dp and 4dp at 411dp.
            val startPadding = contentPadding.calculateStartPadding(layoutDirection)
            val endPadding = contentPadding.calculateEndPadding(layoutDirection)
            val available = maxWidth - startPadding - endPadding - (PAGE_EDGE - TILE_INSET) * 2
            val columns = maxOf(1, (available / (MIN_THUMB + TILE_INSET * 2)).toInt())
            val thumbSize = available / columns - TILE_INSET * 2
            val gridPadding =
                PaddingValues(
                    start = startPadding + PAGE_EDGE - TILE_INSET,
                    top = contentPadding.calculateTopPadding(),
                    end = endPadding + PAGE_EDGE - TILE_INSET,
                    bottom = contentPadding.calculateBottomPadding(),
                )

            // Keyed on the KIND, not the instance: LocalResource has no equals, so every Room
            // emission is a new Success and keying on it faded the whole grid on each one.
            Crossfade(targetState = data.kind(), label = "GridLibraryPlaylist") { kind ->
                when (kind) {
                    ResourceKind.LOADING ->
                        HeadedState(gridPadding, thumbSize, header, createNewPlaylist) {
                            CenterLoadingBox(Modifier.fillMaxSize())
                        }

                    ResourceKind.ERROR ->
                        HeadedState(gridPadding, thumbSize, header, createNewPlaylist) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.load_failed),
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                TextButton(onClick = onReload) {
                                    Text(stringResource(Res.string.retry))
                                }
                            }
                        }

                    ResourceKind.SUCCESS -> {
                        // The outgoing branch of a fade reads the resource that replaced it;
                        // drawing nothing there beats drawing the empty text over a list that
                        // was full a frame ago.
                        val list = (data as? LocalResource.Success)?.data ?: return@Crossfade
                        if (list.isEmpty()) {
                            HeadedState(gridPadding, thumbSize, header, createNewPlaylist) {
                                Text(
                                    text = stringResource(emptyText),
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                contentPadding = gridPadding,
                                state = state,
                            ) {
                                if (header != null) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        header()
                                    }
                                }
                                if (createNewPlaylist != null) {
                                    item(key = "create") {
                                        CreatePlaylistTile(thumbSize, createNewPlaylist)
                                    }
                                }
                                itemsIndexed(
                                    list,
                                    // Strings only: a grid key has to survive saved state, and
                                    // the type prefix keeps a chart's playlist id apart from a
                                    // stored playlist's. Room re-emits the whole list, so
                                    // without keys every tile was rebuilt on each write.
                                    key = { index, item ->
                                        when (item) {
                                            is LocalPlaylistEntity -> "local:${item.id}"
                                            is PlaylistsResult -> "yt:${item.browseId}"
                                            is AlbumEntity -> "album:${item.browseId}"
                                            is PlaylistEntity -> "playlist:${item.id}"
                                            is PodcastsEntity -> "podcast:${item.podcastId}"
                                            is ChartItem -> "chart:${item.ytPlaylistId}"
                                            is MonthlyRecapItem -> "recap:${item.year}-${item.month}"
                                            else -> "item:$index"
                                        }
                                    },
                                ) { _, item ->
                                    if (item !is PlaylistType) {
                                        return@itemsIndexed
                                    }
                                    HomeItemContentPlaylist(
                                        onClick = {
                                            when (item) {
                                                is ChartItem -> {
                                                    navController.navigate(
                                                        PlaylistDestination(
                                                            playlistId = item.ytPlaylistId,
                                                            isYourYouTubePlaylist = false,
                                                        ),
                                                    )
                                                }

                                                is LocalPlaylistEntity -> {
                                                    navController.navigate(
                                                        LocalPlaylistDestination(
                                                            item.id,
                                                        ),
                                                    )
                                                }

                                                is PlaylistsResult -> {
                                                    navController.navigate(
                                                        PlaylistDestination(
                                                            item.browseId,
                                                            isYourYouTubePlaylist = true,
                                                        ),
                                                    )
                                                }

                                                is AlbumEntity -> {
                                                    navController.navigate(
                                                        AlbumDestination(
                                                            item.browseId,
                                                        ),
                                                    )
                                                }

                                                is PlaylistEntity -> {
                                                    navController.navigate(
                                                        PlaylistDestination(
                                                            item.id,
                                                        ),
                                                    )
                                                }

                                                is PodcastsEntity -> {
                                                    navController.navigate(
                                                        PodcastDestination(
                                                            podcastId = item.podcastId,
                                                        ),
                                                    )
                                                }

                                                // The recap is not a stored playlist — it is a
                                                // query over `playback_event` — so it opens the
                                                // dynamic playlist screen, which rebuilds it from
                                                // the year and month carried here.
                                                is MonthlyRecapItem -> {
                                                    navController.navigate(
                                                        LibraryDynamicPlaylistDestination(
                                                            type =
                                                                LibraryDynamicPlaylistType
                                                                    .MonthlyRecap(
                                                                        year = item.year,
                                                                        month = item.month,
                                                                    ).toStringParams(),
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                        data = item,
                                        thumbSize = thumbSize,
                                    )
                                }

                                if (footer != null) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        footer()
                                    }
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EndOfPage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ResourceKind { LOADING, ERROR, SUCCESS }

private fun LocalResource<*>.kind(): ResourceKind =
    when (this) {
        is LocalResource.Loading -> ResourceKind.LOADING
        is LocalResource.Error -> ResourceKind.ERROR
        is LocalResource.Success -> ResourceKind.SUCCESS
    }

/**
 * Loading, error and empty, with whatever the tab shows above its tiles kept in place.
 *
 * The create tile and the header exist in every state the tab does — a user on an empty local
 * playlists tab still needs the way to make one — so they sit above the state message rather
 * than being swapped out by it. `contentPadding` is applied here as a whole, so the message
 * centres in the area between the bars instead of under them.
 */
@Composable
private fun HeadedState(
    contentPadding: PaddingValues,
    thumbSize: Dp,
    header: (@Composable () -> Unit)?,
    createNewPlaylist: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        header?.invoke()
        if (createNewPlaylist != null) {
            CreatePlaylistTile(thumbSize, createNewPlaylist)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun CreatePlaylistTile(
    thumbSize: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(TILE_INSET),
        ) {
            Box(
                Modifier
                    .size(thumbSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .angledGradientBackground(
                        colors =
                            listOf(
                                seed,
                                Color.White.copy(alpha = 0.8f),
                            ),
                        degrees = 45f,
                    ),
                Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(84.dp),
                    imageVector = SimpIcons.Add,
                    tint = Color.White,
                    contentDescription = null,
                )
            }
            Text(
                text = stringResource(Res.string.create),
                style = typo().titleSmall,
                // Sits on the page background, not on the tile, so it has to follow the theme —
                // hard-coded white vanished in light theme while every other tile label stayed
                // readable.
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(thumbSize)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 8.dp)
                        .basicMarquee(
                            iterations = marqueeIterations(Int.MAX_VALUE),
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        }
    }
}
