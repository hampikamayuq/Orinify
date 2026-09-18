package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.DropdownButton
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.ArtistMixShelf
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.OfflineErrorState
import com.maxrave.simpmusic.ui.component.QuickPicksItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Notifications
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PersonalHomeViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.chart
import simpmusic.composeapp.generated.resources.commute
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.energize
import simpmusic.composeapp.generated.resources.feel_good
import simpmusic.composeapp.generated.resources.focus
import simpmusic.composeapp.generated.resources.go_to_log_in_page
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.home_offline_title
import simpmusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import simpmusic.composeapp.generated.resources.let_s_start_with_a_radio
import simpmusic.composeapp.generated.resources.log_in_warning
import simpmusic.composeapp.generated.resources.might_like_empty
import simpmusic.composeapp.generated.resources.notification
import simpmusic.composeapp.generated.resources.party
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.recently
import simpmusic.composeapp.generated.resources.relax
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.romance
import simpmusic.composeapp.generated.resources.sad
import simpmusic.composeapp.generated.resources.settings
import simpmusic.composeapp.generated.resources.sleep
import simpmusic.composeapp.generated.resources.top_artists
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.welcome_back
import simpmusic.composeapp.generated.resources.what_is_best_choice_today
import simpmusic.composeapp.generated.resources.workout

// DataStore key for blog-promo one-shot dialog. Bump the suffix (v2, v3, …) to re-promote.

private val listOfHomeChip =
    listOf(
        Res.string.all,
        Res.string.relax,
        Res.string.sleep,
        Res.string.energize,
        Res.string.sad,
        Res.string.romance,
        Res.string.feel_good,
        Res.string.workout,
        Res.string.party,
        Res.string.commute,
        Res.string.focus,
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    personalViewModel: PersonalHomeViewModel =
        koinViewModel(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    // Home's own half. Collected separately from `homeData` because the two fail independently —
    // see PersonalHomeViewModel.
    val personalTracking by personalViewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val personalShelves by personalViewModel.songShelves.collectAsStateWithLifecycle()
    val artistMixes by personalViewModel.artistMixes.collectAsStateWithLifecycle()
    val loadingArtistMix by personalViewModel.loadingArtistMix.collectAsStateWithLifecycle()
    val personalLoaded by personalViewModel.loaded.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val homeLoadFailed by viewModel.homeLoadFailed.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var accountShow by rememberSaveable {
        mutableStateOf(false)
    }
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val selectedChip = homeChipFor(params)
    // Each chip's bounds in the Row's content space, written from onGloballyPositioned. The Row
    // is a plain horizontalScroll, so nothing brings a chip into view on its own: one restored
    // from `params` on return, or tapped at the right edge, sat half off-screen.
    val chipBounds = remember { mutableStateMapOf<StringResource, Rect>() }
    val chipRowPaddingPx = with(LocalDensity.current) { CHIP_ROW_HORIZONTAL_PADDING.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(params) {
        // The map is empty until the row has been placed, on first composition and on return.
        val bounds = snapshotFlow { chipBounds[selectedChip] }.filterNotNull().first()
        val viewport = chipRowState.viewportSize
        if (viewport <= 0) return@LaunchedEffect
        // boundsInParent excludes the Row's own padding, which scrolls with the content.
        val chipStart = bounds.left + chipRowPaddingPx
        val chipEnd = bounds.right + chipRowPaddingPx
        // Left-based window. In RTL scroll value 0 shows the content's RIGHT edge.
        val windowStart =
            if (isRtl) (chipRowState.maxValue - chipRowState.value).toFloat() else chipRowState.value.toFloat()
        val target =
            when {
                chipStart < windowStart -> chipStart
                chipEnd > windowStart + viewport -> chipEnd - viewport
                else -> return@LaunchedEffect // already fully visible
            }
        val value = if (isRtl) chipRowState.maxValue - target else target
        chipRowState.animateScrollTo(value.roundToInt().coerceIn(0, chipRowState.maxValue))
    }
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()


    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    // What is ACTUALLY painted behind this screen. The desktop shell wraps content in a rounded
    // panel (App.kt: surfaceContainer on light, desktopPanelDark on dark) — deliberately not
    // colorScheme.background — so a gradient tail aimed at colorScheme.background ends on the
    // wrong colour and draws a seam where the first item stops. Same light check as App.kt's
    // isLightScheme.
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember {
        mutableStateOf(backgroundColor)
    }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val mainHomeThumbnail by viewModel.mainHomeThumbnail.collectAsStateWithLifecycle()
    // `remember`, and the DisposableEffect below, because only the LOADER was remembered here:
    // the argument was evaluated on every recomposition, and this composable reads the scroll
    // offset, so it recomposed on every frame of a scroll. Each frame allocated an HTTP client
    // with its own connection pool — jank on the one gesture this screen is made of.
    val paletteHttpClient = remember { HttpClient(CIO) }
    DisposableEffect(paletteHttpClient) {
        onDispose { paletteHttpClient.close() }
    }
    val networkLoader = rememberNetworkLoader(paletteHttpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )

    LaunchedEffect(mainHomeThumbnail) {
        mainHomeThumbnail?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            // Light theme: pull the artwork color toward white for a soft pastel header;
            // dark theme keeps the original darkened tone.
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }


    var topAppBarHeightPx by rememberSaveable {
        mutableIntStateOf(0)
    }

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.getHomeItemList(params)
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (scrollState.layoutInfo.totalItemsCount - 1)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

//    if (shouldShowGetDataSyncIdBottomSheet) {
//        GetDataSyncIdBottomSheet(
//            cookie = youTubeCookie,
//            onDismissRequest = {
//                shouldShowGetDataSyncIdBottomSheet = false
//            },
//        )
//    }





    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            title = {
                Text(stringResource(Res.string.warning))
            },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    doNotShowAgain = !doNotShowAgain
                                }.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = doNotShowAgain,
                            onCheckedChange = {
                                doNotShowAgain = it
                            },
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(stringResource(Res.string.do_not_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                    navController.navigate(LoginDestination)
                }) {
                    Text(stringResource(Res.string.go_to_log_in_page))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            onDismissRequest = {
                viewModel.doneShowLogInAlert()
            },
        )
    }

    Box {
        PullToRefreshBox(
            modifier =
                Modifier
                    .hazeSource(hazeState),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top =
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    // The full-page offline state only when there is nothing at all to show AND
                    // the home request actually failed. An empty list on its own is not an error:
                    // a mood chip can legitimately return no shelves, and the personal half needs
                    // no network, so it keeps rendering with an inline retry row instead.
                    val hasPersonalContent =
                        personalTracking && (personalShelves.isNotEmpty() || artistMixes.isNotEmpty())
                    if (homeData.isEmpty() && homeLoadFailed && !hasPersonalContent) {
                        OfflineErrorState(
                            onRetry = onRefresh,
                            onOpenDownloaded = {
                                navController.navigate(
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ),
                                )
                            },
                        )
                        return@Crossfade
                    }
                    LazyColumn(
                        state = scrollState,
                        // `top = 0` on purpose, and it is the only part of this that differs from
                        // the sibling tabs: the first shelf draws its artwork gradient edge to edge
                        // UNDER the transparent top bar, so a top inset here would cut it off at a
                        // hard seam. The bottom is what was missing — without it Home was the one
                        // destination drawing beneath the mini player and the system navigation
                        // bar, which is what hid the second shelf's card titles while leaving their
                        // subtitles visible.
                        contentPadding = innerPadding.copy(top = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Principle 2: when both halves have something to show, the listener's own
                        // history goes first. These are ordinary HomeItems, so they draw through
                        // the same shelf component as everything below them.
                        if (personalTracking) {
                            items(personalShelves, key = { "personal:" + it.title }) { shelf ->
                                HomeItem(
                                    navController = navController,
                                    data = shelf,
                                )
                            }
                            item(key = "personal:artistMixes") {
                                // Not a HomeItem: the generic shelf routes an artist to the artist
                                // page, and a mix has to start the artist's radio instead.
                                ArtistMixShelf(
                                    artists = artistMixes,
                                    isLoading = false,
                                    loadingChannelId = loadingArtistMix,
                                    onClick = { artist -> personalViewModel.playArtistMix(artist) },
                                )
                            }
                            // Principle 3: an empty personal list is a state, not a bug, and it
                            // has to say so. Tracking is on and every source has answered with
                            // nothing — a fresh install, or a cleared history — so one quiet line
                            // where the shelves will be, never a heading over nothing and never
                            // while the queries are still running.
                            if (personalLoaded && personalShelves.isEmpty() && artistMixes.isEmpty()) {
                                item(key = "personal:empty") {
                                    Text(
                                        text = stringResource(Res.string.might_like_empty),
                                        style = typo().bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 15.dp, vertical = 12.dp)
                                                .alpha(0.7f),
                                    )
                                }
                            }
                        }
                        // The key used to carry `mainHomeThumbnail`, which is DERIVED from this
                        // same list — so the moment the artwork resolved, every key changed and
                        // LazyList destroyed and rebuilt every row, losing each shelf's horizontal
                        // scroll position. Nothing needed it: the gradient it was guarding is
                        // driven by `animatedColor`, which is state and recomposes on its own.
                        // Index first because two shelves can carry the same title and a duplicate
                        // key is a runtime crash.
                        itemsIndexed(homeData, key = { index, item ->
                            "$index:${item.title}"
                        }) { index, item ->
                            Box {
                                if (index == 0) {
                                    Box(
                                        modifier =
                                            Modifier
                                                // matchParentSize, not height(300.dp): 300 is the
                                                // height of this shelf ON A PHONE. On a desktop
                                                // window the first item is taller, the gradient
                                                // stopped mid-item and everything below it fell
                                                // back to the flat background — a hard colour seam
                                                // straight across Home. Sized by the item, the
                                                // bottom scrim always lands on the item's edge.
                                                .matchParentSize()
                                                .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(pageBackground)),
                                        )
                                    }
                                }
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 15.dp),
                                ) {
                                    if (index == 0) {
                                        Spacer(
                                            Modifier.height(
                                                with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (index == 0 && accountInfo != null && accountShow) {
                                        AccountLayout(
                                            accountName = accountInfo?.first ?: "",
                                            url = accountInfo?.second ?: "",
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    if (item.title == stringResource(Res.string.quick_picks)) {
                                        AnimatedVisibility(
                                            visible =
                                                homeData.find {
                                                    it.title ==
                                                        stringResource(
                                                            Res.string.quick_picks,
                                                        )
                                                } != null,
                                        ) {
                                            QuickPicks(
                                                homeItem =
                                                    (
                                                        homeData.find {
                                                            it.title ==
                                                                stringResource(
                                                                    Res.string.quick_picks,
                                                                )
                                                        } ?: return@AnimatedVisibility
                                                    ).let { content ->
                                                        content.copy(
                                                            contents =
                                                                content.contents.mapNotNull { ct ->
                                                                    ct?.copy(
                                                                        artists =
                                                                            ct.artists?.let { art ->
                                                                                if (art.size > 1) {
                                                                                    art.dropLast(1)
                                                                                } else {
                                                                                    art
                                                                                }
                                                                            },
                                                                    )
                                                                },
                                                        )
                                                    },
                                                navController = navController,
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else {
                                        HomeItem(
                                            navController = navController,
                                            data = item,
                                        )
                                    }
                                }
                            }
                        }
                        // The YouTube half failed while the personal half is on screen: one line
                        // and a retry where the shelves would have been, not the full-page state.
                        if (homeData.isEmpty() && homeLoadFailed) {
                            item(key = "home:loadFailed") {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.home_offline_title),
                                        style = typo().bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = onRefresh) {
                                        Text(stringResource(Res.string.retry))
                                    }
                                }
                            }
                        }
                        item {
                            AnimatedVisibility(
                                homeListState == ListState.PAGINATING,
                                enter = expandVertically() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CenterLoadingBox(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                )
                            }
                        }
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            items(newRelease, key = { it.hashCode() }) {
                                AnimatedVisibility(
                                    visible = newRelease.isNotEmpty(),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(horizontal = 15.dp),
                                    ) {
                                        HomeItem(
                                            navController = navController,
                                            data = it,
                                        )
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = moodMomentAndGenre != null,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(horizontal = 15.dp),
                                    ) {
                                        moodMomentAndGenre?.let {
                                            MoodMomentAndGenre(
                                                mood = it,
                                                navController = navController,
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Column(
                                    Modifier
                                        .padding(vertical = 10.dp)
                                        .padding(horizontal = 15.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ChartTitle()
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(targetState = regionChart) {
                                        Logger.w("HomeScreen", "regionChart: $it")
                                        if (it != null) {
                                            DropdownButton(
                                                items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                                defaultSelected =
                                                    CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                        CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                    )
                                                        ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                            ) {
                                                viewModel.exploreChart(
                                                    CHART_SUPPORTED_COUNTRY.items[
                                                        CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                            it,
                                                        ),
                                                    ],
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(
                                        targetState = chartLoading,
                                        label = "Chart",
                                    ) { loading ->
                                        if (!loading) {
                                            chart?.let {
                                                ChartData(
                                                    chart = it,
                                                    navController = navController,
                                                )
                                            }
                                        } else {
                                            CenterLoadingBox(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(400.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) {
                                    topAppBarHeightPx.toDp()
                                },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }
        AnimatedContent(
            targetState = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { target ->
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (target) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier
                                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                                        blurEnabled = true
                                    }
                            },
                        ).onGloballyPositioned { coordinates ->
                            topAppBarHeightPx = coordinates.size.height
                        },
            ) {
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    HomeTopAppBar(navController)
                }
                AnimatedVisibility(
                    visible = !isScrollingUp,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Spacer(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(
                                    WindowInsets.statusBars,
                                ),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            // Inset before the scroll: a cutout eats into the row's own bounds, so
                            // without this the first and last chip sit under it in landscape.
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
                            ).horizontalScroll(chipRowState)
                            .padding(vertical = 8.dp, horizontal = CHIP_ROW_HORIZONTAL_PADDING)
                            .background(Color.Transparent),
                    // 8dp, not 4: android.md asks for at least 8dp between touch targets, and
                    // these chips are the primary filter of the screen.
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOfHomeChip.forEach { id ->
                        // Chip takes no Modifier, hence the Box. boundsInParent is relative to
                        // the Row's content, so it does not move with the scroll; the equality
                        // guard keeps every scroll frame from writing the same value as state.
                        Box(
                            Modifier.onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                if (chipBounds[id] != bounds) chipBounds[id] = bounds
                            },
                        ) {
                            Chip(
                                isAnimated = loading,
                                isSelected = id == selectedChip,
                                text = stringResource(id),
                            ) {
                                when (id) {
                                    Res.string.all -> viewModel.setParams(null)
                                    Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                    Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                    Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                    Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                    Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                    Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                    Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                    Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                    Res.string.commute -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                    Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour =
        remember {
            val date = now().time
            date.hour
        }
    TopAppBar(
        // The Start inset used to be excluded outright. `TopAppBarDefaults.windowInsets` is
        // `safeDrawing.only(Horizontal + Top)`, so that also threw away `displayCutout` — putting
        // the title under a left-edge cutout in landscape, and under the cutout in RTL. On a phone
        // in portrait the Start inset is 0, which is why the exclusion looked free.
        windowInsets = TopAppBarDefaults.windowInsets,
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text =
                        // Noon is afternoon and 5am is morning: 12 used to greet "good morning".
                        when (hour) {
                            in 5..11 -> {
                                stringResource(Res.string.good_morning)
                            }

                            in 12..17 -> {
                                stringResource(Res.string.good_afternoon)
                            }

                            in 18..22 -> {
                                stringResource(Res.string.good_evening)
                            }

                            else -> {
                                stringResource(Res.string.good_night)
                            }
                        },
                    style = typo().bodySmall,
                )
            }
        },
        actions = {
            // All four carry a contentDescription. Without one they announced as four identical
            // unnamed "Button"s in a row, on the app's opening screen — RippleIconButton used to
            // hardcode null and offer no way to pass a label.
            RippleIconButton(
                imageVector = SimpIcons.Notifications,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(Res.string.notification),
            ) {
                navController.navigate(NotificationDestination)
            }
            RippleIconButton(
                imageVector = SimpIcons.History,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(Res.string.recently),
            ) {
                navController.navigate(RecentlySongsDestination)
            }
            // Fourth button, immediately before Settings — the position the design canvas fixes.
            ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
            RippleIconButton(
                imageVector = SimpIcons.Settings,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(Res.string.settings),
            ) {
                navController.navigate(SettingsDestination)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Text(
                text = accountName,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .padding(start = 8.dp),
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    var bottomSheetShow by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf<Track?>(null) }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Text(
            text = stringResource(Res.string.let_s_start_with_a_radio),
            style = typo().bodySmall,
        )
        Text(
            text = stringResource(Res.string.quick_picks),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(QUICK_PICKS_ROWS),
            modifier = Modifier.height(rememberQuickPicksGridHeight()),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            // `contents` is List<Content?> — the body has always skipped nulls — and
            // `Any?.hashCode()` returns 0 for every one of them. Two nulls in one shelf therefore
            // produced the same key twice, which LazyGrid rejects outright. Keyed by the id the
            // card actually navigates with, and indexed so a null still gets a key of its own.
            itemsIndexed(
                homeItem.contents,
                key = { index, item -> item?.videoId ?: item?.playlistId ?: item?.browseId ?: index },
            ) { _, item ->
                if (item != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = item.toTrack()
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${item.videoId}",
                                    playlistName = "\"${item.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onLongClick = {
                            track = item.toTrack()
                            bottomSheetShow = true
                        },
                        data = item,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        // One block per section YouTube returned, headed by ITS OWN title. Hard-coding
        // "Moods & moment" / "Genre" here (and reading mood.moodsMoments / mood.genres by
        // index) mislabelled every row as soon as a signed-in account got an extra
        // "For you" section, and hid the real Genres section altogether.
        val gridHeight = rememberMoodGridHeight()
        mood.sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(MOOD_GRID_ROWS),
                modifier = Modifier.height(gridHeight),
                state = gridState,
                flingBehavior = flingBehavior,
            ) {
                items(section.items, key = { it.params }) { item ->
                    MoodMomentAndGenreHomeItem(
                        title = item.title,
                        stripeColor = item.stripeColor,
                    ) {
                        navController.navigate(
                            MoodDestination(
                                item.params,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(Res.string.what_is_best_choice_today),
            style = typo().bodyMedium,
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(CHART_ARTIST_ROWS),
            modifier = Modifier.height(rememberChartArtistGridHeight()),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = data.browseId,
                            ),
                        )
                    },
                    data = data,
                    widthDp = gridWidthDp,
                )
            }
        }
    }
}

// Horizontal padding of the chip row. Named because the scroll-into-view effect has to add the
// same number back: boundsInParent stops at the Row's content, ScrollState counts the padding.
private val CHIP_ROW_HORIZONTAL_PADDING = 15.dp

// Which chip a params value selects; null and anything unknown fall to "All".
private fun homeChipFor(params: String?): StringResource =
    when (params) {
        HOME_PARAMS_RELAX -> Res.string.relax
        HOME_PARAMS_SLEEP -> Res.string.sleep
        HOME_PARAMS_ENERGIZE -> Res.string.energize
        HOME_PARAMS_SAD -> Res.string.sad
        HOME_PARAMS_ROMANCE -> Res.string.romance
        HOME_PARAMS_FEEL_GOOD -> Res.string.feel_good
        HOME_PARAMS_WORKOUT -> Res.string.workout
        HOME_PARAMS_PARTY -> Res.string.party
        HOME_PARAMS_COMMUTE -> Res.string.commute
        HOME_PARAMS_FOCUS -> Res.string.focus
        else -> Res.string.all
    }

// The three LazyHorizontalGrids on this screen carried a height literal tuned by eye at font
// scale 1.0 (256, 210, 240). None of typo()'s styles declares a lineHeight, so the text grows
// with the system font-size setting while the literal did not, and at 1.3 the last row clipped.
// Each is now rows × what one cell actually holds, measured the way rememberShelfCardMinHeight
// in AdapterItems.kt measures a shelf card. A row's contents are restated here from the item
// composable it renders — change one, change the other.
private const val QUICK_PICKS_ROWS = 4
private const val MOOD_GRID_ROWS = 3
private const val CHART_ARTIST_ROWS = 3

@Composable
private fun rememberTextLinesHeight(
    style: TextStyle,
    lines: Int,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style, lines) {
        val probe = List(lines) { "A" }.joinToString("\n")
        with(density) { measurer.measure(probe, style).size.height.toDp() }
    }
}

// QuickPicksItem: 10dp row padding around max(44dp thumbnail, one-line titleSmall + 3dp + the
// artist row, which is one line of bodySmall or the 20dp explicit badge). 64dp/row at scale 1.0.
@Composable
private fun rememberQuickPicksGridHeight(): Dp {
    val title = rememberTextLinesHeight(typo().titleSmall, 1)
    val artists = rememberTextLinesHeight(typo().bodySmall, 1)
    val text = title + 3.dp + maxOf(artists, 20.dp)
    return (maxOf(44.dp, text) + 20.dp) * QUICK_PICKS_ROWS
}

// MoodMomentAndGenreHomeItem: 8dp padding around a card floored at 48dp holding up to two lines
// of titleSmall. 64dp/row at scale 1.0: the old 70dp/row stretched every card to 54dp (a
// horizontal grid fixes the cell height); the card now sits at its own 48dp floor.
@Composable
private fun rememberMoodGridHeight(): Dp {
    val title = rememberTextLinesHeight(typo().titleSmall, 2)
    return (maxOf(48.dp, title) + 16.dp) * MOOD_GRID_ROWS
}

// ItemArtistChart: 10dp row padding around max(one-line titleLarge rank, 60dp avatar, two-line
// titleSmall name + one-line bodySmall subscribers). 80dp/row at scale 1.0.
@Composable
private fun rememberChartArtistGridHeight(): Dp {
    val rank = rememberTextLinesHeight(typo().titleLarge, 1)
    val name = rememberTextLinesHeight(typo().titleSmall, 2) + rememberTextLinesHeight(typo().bodySmall, 1)
    return (maxOf(rank, 60.dp, name) + 20.dp) * CHART_ARTIST_ROWS
}
