package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.ChartItem
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.ui.screen.home.analytics.monthFullNameResource
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_local_playlist
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year

class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val podcastRepository: PodcastRepository,
) : BaseViewModel() {
    private val _currentScreen: MutableStateFlow<LibraryChipType> = MutableStateFlow(LibraryChipType.YOUR_LIBRARY)
    val currentScreen: StateFlow<LibraryChipType> get() = _currentScreen.asStateFlow()

    /**
     * The last [RECENTLY_PLAYED_LIMIT] distinct tracks in the listening history, newest first.
     *
     * Read off `playback_event`, not the cache-insertion union this used to be: that one listed
     * whatever the app had merely OPENED — an album page, an artist — in the order it was cached,
     * which is not what "recently played" means to a listener.
     */
    private val _recentlyPlayed: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyPlayed: StateFlow<LocalResource<List<SongEntity>>> get() = _recentlyPlayed.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _chartPlaylists: MutableStateFlow<LocalResource<List<ChartItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val chartPlaylists: StateFlow<LocalResource<List<ChartItem>>> get() = _chartPlaylists.asStateFlow()

    /**
     * The months the Wrapped tab offers a recap for, newest first.
     *
     * A [MonthlyRecapItem] rather than the destination's own
     * [LibraryDynamicPlaylistType.MonthlyRecap]: the tab draws these through the shared
     * `GridLibraryPlaylist`, which renders only [PlaylistType]s, and a tile needs a title and a
     * cover on top of the year and month the destination carries. The destination is rebuilt from
     * the year and month when a tile is tapped.
     */
    private val _monthlyRecaps: MutableStateFlow<LocalResource<List<MonthlyRecapItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val monthlyRecaps: StateFlow<LocalResource<List<MonthlyRecapItem>>> get() = _monthlyRecaps.asStateFlow()

    private val _accountThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    // One per source. Every getX() below used to launch a fresh collector on a hot Room flow and
    // never cancel it, and three of them are called on every return to the tab, so the flows piled
    // up collectors for the life of the ViewModel and each write raced the ones before it.
    private var recentlyPlayedJob: Job? = null
    private var youTubePlaylistJob: Job? = null
    private var mixForYouJob: Job? = null
    private var favoritePlaylistJob: Job? = null
    private var favoritePodcastsJob: Job? = null
    private var localPlaylistJob: Job? = null
    private var downloadedPlaylistJob: Job? = null
    private var monthlyRecapsJob: Job? = null
    private var chartPlaylistsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    /**
     * Whether the Wrapped chip has anything behind it.
     *
     * The same setting the Analytics tab follows, read the same way — Wrapped and the recaps are
     * built entirely from `playback_event`, which local tracking is what fills.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val localTrackingEnabled = dataStoreManager.localTrackingEnabled.mapLatest { it == DataStoreManager.TRUE }

    init {
        viewModelScope.launch {
            val currentScreenJob =
                launch {
                    dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                        LibraryChipType.fromStringValue(chipType)?.let {
                            _currentScreen.value = it
                        }
                    }
                }
            val cookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect {
                        _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
                    }
                }
            currentScreenJob.join()
            cookieJob.join()
        }
    }

    /** Cancels the collector a source already has before starting its replacement. */
    private fun relaunch(
        current: Job?,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        current?.cancel()
        return viewModelScope.launch(block = block)
    }

    fun setCurrentScreen(chipType: LibraryChipType) {
        _currentScreen.value = chipType
        viewModelScope.launch {
            dataStoreManager.putString("library_current_screen", chipType.toStringValue())
        }
    }

    fun getRecentlyPlayed() {
        recentlyPlayedJob =
            relaunch(recentlyPlayedJob) {
                // Held rather than cleared: this runs on every track change and every return to
                // the tab, and blanking a list already on screen to re-read the same rows is a
                // flicker, not a load. Only a first run, which has nothing to hold, spins.
                if (_recentlyPlayed.value !is LocalResource.Success) {
                    _recentlyPlayed.value = LocalResource.Loading()
                }
                _recentlyPlayed.value =
                    try {
                        LocalResource.Success(recentlyPlayedSongs())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LocalResource.Error<List<SongEntity>>(e.message ?: "Unknown error")
                    }
            }
    }

    /**
     * Newest first, one row per track, capped after resolving so a swept song leaves no gap.
     *
     * Reads the last [RECENTLY_PLAYED_EVENT_WINDOW] plays and keeps each id's first appearance:
     * a listener on repeat would otherwise fill the whole list with one song. A window over the
     * indexed timestamp rather than a `GROUP BY videoId` over the table, which scans everything —
     * a list this short never needs more history than that. The window also keeps the batch
     * lookup under SQLite's 999 bound variables on the oldest supported devices.
     */
    private suspend fun recentlyPlayedSongs(): List<SongEntity> {
        val ids =
            analyticsRepository
                .getPlaybackEventsByOffset(offset = 0, limit = RECENTLY_PLAYED_EVENT_WINDOW)
                .firstOrNull()
                .orEmpty()
                .map { it.videoId }
                .distinct()
        return songRepository
            .getSongsByListVideoId(ids)
            .firstOrNull()
            .orEmpty()
            // Removed through the row's own sheet: the play happened, but the listener asked for
            // the song to go, and a list that brings it straight back makes that action look
            // broken.
            .filterNot { it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
            .take(RECENTLY_PLAYED_LIMIT)
    }

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        youTubePlaylistJob =
            relaunch(youTubePlaylistJob) {
                // Still Success on null: the repository emits null for BOTH "no playlists" and a
                // failed fetch with no offline copy, so a failure cannot be told apart here.
                playlistRepository.getLibraryPlaylist().collect { data ->
                    _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
                }
            }
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        mixForYouJob =
            relaunch(mixForYouJob) {
                playlistRepository.getMixedForYou().collect { data ->
                    _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
                }
            }
    }

    fun getPlaylistFavorite() {
        favoritePlaylistJob =
            relaunch(favoritePlaylistJob) {
                // combine, not a collect nested inside a collect: nested, the inner one never
                // returned, so only the FIRST album emission was ever paired with the playlists —
                // liking an album after the tab had opened did nothing until it was re-entered.
                combine(albumRepository.getLikedAlbums(), playlistRepository.getLikedPlaylists()) { albums, playlists ->
                    buildList<PlaylistType> {
                        addAll(albums)
                        addAll(playlists)
                    }.sortedWith(
                        Comparator { p0, p1 ->
                            val timeP0: LocalDateTime? =
                                when (p0) {
                                    is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                    is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                    else -> null
                                }
                            val timeP1: LocalDateTime? =
                                when (p1) {
                                    is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                    is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                    else -> null
                                }
                            if (timeP0 == null || timeP1 == null) {
                                return@Comparator if (timeP0 == null && timeP1 == null) {
                                    0
                                } else if (timeP0 == null) {
                                    -1
                                } else {
                                    1
                                }
                            }
                            timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                        },
                    )
                }.collect { sortedList ->
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
    }

    fun getFavoritePodcasts() {
        favoritePodcastsJob =
            relaunch(favoritePodcastsJob) {
                podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                    val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                    _favoritePodcasts.value = LocalResource.Success(sortedList)
                }
            }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        localPlaylistJob =
            relaunch(localPlaylistJob) {
                localPlaylistRepository.getAllLocalPlaylists().collect { values ->
                    _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
                }
            }
    }

    fun getDownloadedPlaylist() {
        downloadedPlaylistJob =
            relaunch(downloadedPlaylistJob) {
                playlistRepository.getAllDownloadedPlaylist().collect { values ->
                    _downloadedPlaylist.value = LocalResource.Success(values)
                }
            }
    }

    /**
     * Which of the last twelve months the user actually listened in, and what each tile shows.
     *
     * A month with no plays is left out rather than shown empty: a "Recap March" that opens onto
     * nothing is worse than no row at all. Twelve is a cap, not a quota — a new install shows one
     * row, or none.
     *
     * The count comes first and gates everything after it: twelve `COUNT`s over an indexed
     * timestamp range are cheap, so the months with nothing in them are dropped before anything
     * asks them for a ranking. Only the survivors pay for a cover.
     *
     * Title and cover are resolved here rather than in the tile, which cannot suspend: the title
     * needs a month name out of a string resource with a format argument, and the cover needs a
     * ranking query followed by a song lookup.
     */
    fun getMonthlyRecaps() {
        _monthlyRecaps.value = LocalResource.Loading()
        monthlyRecapsJob =
            relaunch(monthlyRecapsJob) {
                val today = now().date
                val thisMonth = LocalDate(today.year, today.month, 1)
                val months =
                    (0 until MONTHS_OF_RECAP)
                        .map { thisMonth.minus(it, DateTimeUnit.MONTH) }
                        .mapNotNull { firstDay ->
                            val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                            val start = firstDay.atTime(0, 0)
                            val end = lastDay.atTime(23, 59, 59)
                            val plays =
                                analyticsRepository
                                    .getPlaybackEventCountInRange(
                                        startTimestamp = start,
                                        endTimestamp = end,
                                    ).firstOrNull() ?: 0L
                            if (plays <= 0L) return@mapNotNull null
                            MonthlyRecapItem(
                                year = firstDay.year,
                                month = firstDay.month.number,
                                title = recapTitle(firstDay.year, firstDay.month, today.year),
                            )
                        }
                _monthlyRecaps.value = LocalResource.Success(months)
            }
    }

    /**
     * "Recap January", or "Recap January 2025" once the year stops being obvious.
     *
     * The same rule and the same two format strings as the header the tile opens — see
     * [LibraryDynamicPlaylistType.title]. Fully qualified because [BaseViewModel] has a `getString`
     * of its own that takes no format argument and wraps `runBlocking`, which has no business
     * running inside a coroutine that is already suspended here.
     */
    private suspend fun recapTitle(
        year: Int,
        month: Month,
        currentYear: Int,
    ): String {
        val monthName =
            org.jetbrains.compose.resources
                .getString(monthFullNameResource(month))
        return if (year == currentYear) {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month, monthName)
        } else {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month_year, monthName, year.toString())
        }
    }

    fun getChartPlaylists() {
        _chartPlaylists.value = LocalResource.Loading()
        chartPlaylistsJob =
            relaunch(chartPlaylistsJob) {
                playlistRepository.getChartPlaylist().collectLatest {
                    when (it) {
                        is Resource.Success -> _chartPlaylists.value = LocalResource.Success(it.data ?: emptyList())
                        is Resource.Error -> _chartPlaylists.value = LocalResource.Error(it.message ?: "Unknown error")
                    }
                }
            }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            localPlaylistRepository
                .insertLocalPlaylist(
                    localPlaylistEntity,
                    getString(Res.string.added_local_playlist),
                ).lastOrNull()
                ?.let {
                    log("Created playlist with id: $it")
                }
            getLocalPlaylist()
        }
    }

    fun deleteSong(videoId: String) {
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            getRecentlyPlayed()
        }
    }

    companion object {
        /** How far back the Wrapped tab offers recaps, counting the current month as the first. */
        private const val MONTHS_OF_RECAP = 12

        /** A shelf on the tab; the See all page holds the long version. */
        private const val RECENTLY_PLAYED_LIMIT = 10

        /** Enough plays behind ten distinct tracks for a listener who repeats. */
        private const val RECENTLY_PLAYED_EVENT_WINDOW = 60
    }
}
