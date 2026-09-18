package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SimpMusicChartButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.downloaded_playlists
import simpmusic.composeapp.generated.resources.favorite_playlists
import simpmusic.composeapp.generated.resources.favorite_podcasts
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.load_failed
import simpmusic.composeapp.generated.resources.logged_in
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_YouTube_playlists
import simpmusic.composeapp.generated.resources.no_charts_found
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.recently_played
import simpmusic.composeapp.generated.resources.recently_played_empty
import simpmusic.composeapp.generated.resources.recently_played_subtitle
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.see_all
import simpmusic.composeapp.generated.resources.simpmusic_charts
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.your_library
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_youtube_playlists
import kotlin.math.roundToInt
import com.maxrave.domain.mediaservice.handler.PlaylistType as DomainPlaylistType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val sharedViewModel: SharedViewModel = koinInject()

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    // Wrapped and its recaps are built entirely from playback_event, so the chip follows the same
    // setting the Analytics tab does.
    val localTrackingEnabled by viewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val monthlyRecaps by viewModel.monthlyRecaps.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val chartPlaylists by viewModel.chartPlaylists.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    var topAppBarHeight by remember {
        mutableStateOf(0.dp)
    }
    var showAddSheet by remember { mutableStateOf(false) }
    // The song whose "more" sheet is open, if any. One sheet for the whole shelf, hoisted out of
    // the rows, since the rows are now items of the tab's own LazyColumn.
    var moreSong by remember { mutableStateOf<SongEntity?>(null) }

    LaunchedEffect(nowPlaying) {
        viewModel.getRecentlyPlayed()
    }

    val chipRowState = rememberScrollState()
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()

    // Ordered by hand, not `entries`: the enum's declaration order is persistence order, and
    // Mix for you sits in it with no chip of its own.
    val chips =
        remember(loggedIn, localTrackingEnabled) {
            listOfNotNull(
                LibraryChipType.YOUR_LIBRARY,
                LibraryChipType.LOCAL_PLAYLIST,
                LibraryChipType.YOUTUBE_MUSIC_PLAYLIST.takeIf { loggedIn },
                LibraryChipType.FAVORITE_PLAYLIST,
                LibraryChipType.DOWNLOADED_PLAYLIST,
                LibraryChipType.FAVORITE_PODCAST,
                // Nothing to recap without the plays — gated exactly as the YouTube chip is
                // gated on being logged in.
                LibraryChipType.WRAPPED.takeIf { localTrackingEnabled },
                LibraryChipType.CHART,
            )
        }

    // Each chip's bounds in the Row's content space, written from onGloballyPositioned. The Row
    // is a plain horizontalScroll, so nothing brings a chip into view on its own: the one restored
    // from DataStore on return, or tapped at the right edge, sat half off-screen. Same mechanism
    // as Home's chip row.
    val chipBounds = remember { mutableStateMapOf<LibraryChipType, Rect>() }
    val chipRowPaddingPx = with(density) { CHIP_ROW_HORIZONTAL_PADDING.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(currentFilter) {
        // The map is empty until the row has been placed, on first composition and on return.
        val bounds = snapshotFlow { chipBounds[currentFilter] }.filterNotNull().first()
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

    LaunchedEffect(currentFilter) {
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                if (youTubePlaylist.data.isNullOrEmpty()) {
                    viewModel.getYouTubePlaylist()
                }
            }

            // Mix for you has its own nav tab now. The filter is persisted, so a build upgraded
            // while it was selected would land here with no chip to match — send it back to the
            // default. The enum value itself stays so older persisted values still parse.
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY)
            }

            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getRecentlyPlayed()
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }

            LibraryChipType.CHART -> {
                if (chartPlaylists.data.isNullOrEmpty()) {
                    viewModel.getChartPlaylists()
                }
            }

            LibraryChipType.WRAPPED -> {
                viewModel.getMonthlyRecaps()
            }
        }
    }

    // Above the Crossfade, not inside its content lambda: there it was created afresh on every
    // return to the tab, so the scroll position was lost each time another chip was visited.
    val libraryListState = rememberLazyListState()

    Crossfade(
        modifier = Modifier.hazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        when (filter) {
            LibraryChipType.YOUR_LIBRARY -> {
                val isScrollingUp by libraryListState.isScrollingUp()
                LaunchedEffect(libraryListState) {
                    snapshotFlow { libraryListState.firstVisibleItemIndex }
                        .collect {
                            if (it <= 1) {
                                onScrolling.invoke(true)
                            } else {
                                onScrolling.invoke(isScrollingUp)
                            }
                        }
                }
                LazyColumn(
                    contentPadding =
                        innerPadding.copy(
                            top = topAppBarHeight,
                        ),
                    state = libraryListState,
                ) {
                    item {
                        LibraryTilingBox(navController)
                    }

                    item(key = "recently_played:header") {
                        RecentlyPlayedHeader {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.RecentlyPlayed.toStringParams(),
                                ),
                            )
                        }
                    }

                    // Rows are items of THIS list, not a Column inside one item: a Column is one
                    // item to the LazyColumn, so it was measured and composed whole.
                    when (val recent = recentlyPlayed) {
                        is LocalResource.Loading -> {
                            item(key = "recently_played:loading") {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(130.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(Modifier.wrapContentSize())
                                }
                            }
                        }

                        is LocalResource.Error -> {
                            item(key = "recently_played:error") {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.load_failed),
                                        style = typo().bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { viewModel.getRecentlyPlayed() }) {
                                        Text(text = stringResource(Res.string.retry))
                                    }
                                }
                            }
                        }

                        is LocalResource.Success -> {
                            val songs = recent.data.orEmpty()
                            if (songs.isEmpty()) {
                                // The heading stays: this line is what explains it.
                                item(key = "recently_played:empty") {
                                    Text(
                                        text = stringResource(Res.string.recently_played_empty),
                                        style = typo().bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                    )
                                }
                            } else {
                                items(songs, key = { it.videoId }) { song ->
                                    SongFullWidthItems(
                                        songEntity = song,
                                        isPlaying = song.videoId == nowPlaying,
                                        modifier = Modifier,
                                        onMoreClickListener = { moreSong = song },
                                        onClickListener = {
                                            viewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(song.toTrack()),
                                                    firstPlayedTrack = song.toTrack(),
                                                    playlistId = "RDAMVM${song.videoId}",
                                                    playlistName = song.title,
                                                    playlistType = DomainPlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            viewModel.loadMediaItem(
                                                song,
                                                type = Config.SONG_CLICK,
                                                index = 0,
                                            )
                                        },
                                        selectionMode = selectionState.isActive,
                                        isSelected = selectionState.isSelected(song.videoId),
                                        onLongClick = { selectionState.start(it) },
                                        onSelectToggle = { selectionState.toggle(it) },
                                        onAddToQueue = {
                                            sharedViewModel.addListToQueue(
                                                arrayListOf(song.toTrack()),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
            }

            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    youTubePlaylist,
                    emptyText = Res.string.no_YouTube_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getYouTubePlaylist()
                }
            }

            // Nothing to draw: MixForYouScreen owns this content now, and the effect above bounces
            // the filter back to YOUR_LIBRARY the moment it lands here.
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Unit

            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    yourLocalPlaylist,
                    onScrolling = onScrolling,
                    emptyText = Res.string.no_playlists_added,
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    viewModel.getLocalPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePlaylist,
                    emptyText = Res.string.no_favorite_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                    emptyText = Res.string.no_playlists_downloaded,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getDownloadedPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePodcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }

            LibraryChipType.CHART -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    chartPlaylists,
                    emptyText = Res.string.no_charts_found,
                    onScrolling = onScrolling,
                    footer = {
                        SimpMusicChartButton(
                            modifier =
                                Modifier
                                    .wrapContentWidth()
                                    .padding(vertical = 16.dp),
                            onClick = { uriHandler.openUri("https://chart.simpmusic.org") },
                        )
                    },
                ) {
                    viewModel.getChartPlaylists()
                }
            }

            LibraryChipType.WRAPPED -> {
                LibraryWrappedTab(
                    navController = navController,
                    contentPadding = innerPadding.copy(top = topAppBarHeight),
                    recaps = monthlyRecaps,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getMonthlyRecaps()
                }
            }
        }
    }
    moreSong?.let { song ->
        NowPlayingBottomSheet(
            onDismiss = { moreSong = null },
            navController = navController,
            song = song,
            onLibraryDelete = { viewModel.deleteSong(song.videoId) },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        // Read in composition and captured: the click handler used to resolve it with
        // runBlocking on the main thread.
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showAddSheetState.hide()
                    showAddSheet = false
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.outline,
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.playlist_name))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // The one action in the sheet is a filled button, and it is disabled while
                    // the name is blank: a toast after the tap told the user what the button
                    // could have told them before it.
                    Button(
                        onClick = {
                            viewModel.createPlaylist(newTitle.trim())
                            hideEditTitleBottomSheet()
                        },
                        enabled = newTitle.isNotBlank(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.create))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            }.onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.library),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            navigationIcon = {
                AnimatedVisibility(
                    !accountThumbnail.isNullOrEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(accountThumbnail)
                                .crossfade(550)
                                .build(),
                        placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                        error = rememberVectorPainter(SimpIcons.PeopleAlt),
                        // Not decorative: this face is the only thing on the screen that says
                        // an account is signed in.
                        contentDescription = stringResource(Res.string.logged_in),
                        modifier =
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape),
                    )
                }
            },
            // The Library bar had no actions slot at all — added for the Listen Together entry,
            // which the design canvas puts on Home AND Library.
            actions = {
                ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
            },
        )
        AnimatedVisibility(visible = selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                // Stacked BELOW the Library TopAppBar in the same Column, which already consumed
                // the status-bar inset — leaving the default here reserved it twice and opened a
                // status-bar-sized band of dead blur between the two bars. Same fix as Search;
                // the overlay-style call sites (Album, Artist, Recently…) keep the default because
                // they COVER their normal bar instead of standing under it.
                windowInsets = WindowInsets(0),
                onSelectAll = {
                    selectionState.toggleSelectAll(
                        recentlyPlayed.data.orEmpty().map { it.videoId },
                    )
                },
                onOpenActions = { showSelectionSheet = true },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(
            modifier =
                Modifier
                    .horizontalScroll(chipRowState)
                    .padding(horizontal = CHIP_ROW_HORIZONTAL_PADDING)
                    .padding(bottom = 8.dp)
                    .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chips.forEach { type ->
                // Chip takes no Modifier, hence the Box. boundsInParent is relative to the Row's
                // content, so it does not move with the scroll; the equality guard keeps every
                // scroll frame from writing the same value as state.
                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInParent()
                        if (chipBounds[type] != bounds) chipBounds[type] = bounds
                    },
                ) {
                    Chip(
                        isAnimated = false,
                        isSelected = type == currentFilter,
                        text = stringResource(type.label()),
                    ) {
                        viewModel.setCurrentScreen(type)
                    }
                }
            }
        }
        if (showSelectionSheet) {
            val selectedIds = selectionState.selected.toList()
            SelectedSongsBottomSheet(
                count = selectedIds.size,
                onDismiss = { showSelectionSheet = false },
                onPlayNext = {
                    selectionViewModel.playNext(selectedIds)
                    selectionState.exit()
                },
                onAddToQueue = {
                    selectionViewModel.addToQueue(selectedIds)
                    selectionState.exit()
                },
                onAddToPlaylist = { showSelectionAddToPlaylist = true },
                onDownload = {
                    selectionViewModel.download(selectedIds)
                    selectionState.exit()
                },
                onAddToFavorite = {
                    selectionViewModel.addToFavorite(selectedIds)
                    selectionState.exit()
                },
            )
        }
        if (showSelectionAddToPlaylist) {
            val selectedIds = selectionState.selected.toList()
            val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
            AddToPlaylistModalBottomSheet(
                isBottomSheetVisible = true,
                listLocalPlaylist = localPlaylists,
                listYouTubePlaylist = emptyList(),
                onDismiss = { showSelectionAddToPlaylist = false },
                onClick = { playlist ->
                    selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                    selectionState.exit()
                },
                onYTPlaylistClick = {},
            )
        }
    }
}

/**
 * Eyebrow over title, the way a Home shelf names itself, with "See all" on the trailing edge.
 *
 * The eyebrow says where the rows come from; on this tab that is the point — every other block
 * on it is a bucket the listener filled by hand, and this one they filled by listening.
 */
@Composable
private fun RecentlyPlayedHeader(onSeeAll: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.recently_played_subtitle),
                style = typo().bodySmall,
            )
            Text(
                text = stringResource(Res.string.recently_played),
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        TextButton(onClick = onSeeAll) {
            Text(text = stringResource(Res.string.see_all))
        }
    }
}

private fun LibraryChipType.label(): StringResource =
    when (this) {
        LibraryChipType.YOUR_LIBRARY -> Res.string.your_library
        LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> Res.string.your_youtube_playlists
        LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Res.string.mix_for_you
        LibraryChipType.LOCAL_PLAYLIST -> Res.string.your_playlists
        LibraryChipType.FAVORITE_PLAYLIST -> Res.string.favorite_playlists
        LibraryChipType.DOWNLOADED_PLAYLIST -> Res.string.downloaded_playlists
        LibraryChipType.FAVORITE_PODCAST -> Res.string.favorite_podcasts
        LibraryChipType.CHART -> Res.string.simpmusic_charts
        LibraryChipType.WRAPPED -> Res.string.wrapped
    }

// The Row's own horizontal padding, added back onto boundsInParent so both sides of the
// scroll-into-view maths count the same thing: boundsInParent stops at the Row's content,
// ScrollState counts the padding.
private val CHIP_ROW_HORIZONTAL_PADDING = 15.dp
