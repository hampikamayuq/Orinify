package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.Config.REMOVED_SONG_DATE_TIME
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.screen.home.analytics.monthFullNameResource
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year

class LibraryDynamicPlaylistViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val analyticsRepository: AnalyticsRepository,
) : BaseViewModel() {
    private val _listFavoriteSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listFavoriteSong: StateFlow<List<SongEntity>> get() = _listFavoriteSong

    private val _listFollowedArtist: MutableStateFlow<List<ArtistEntity>> = MutableStateFlow(emptyList())
    val listFollowedArtist: StateFlow<List<ArtistEntity>> get() = _listFollowedArtist

    private val _listMostPlayedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMostPlayedSong: StateFlow<List<SongEntity>> get() = _listMostPlayedSong

    private val _listDownloadedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listDownloadedSong: StateFlow<List<SongEntity>> get() = _listDownloadedSong

    private val _listRediscoverSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listRediscoverSong: StateFlow<List<SongEntity>> get() = _listRediscoverSong

    private val _listMightLikeSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMightLikeSong: StateFlow<List<SongEntity>> get() = _listMightLikeSong

    private val _listRecentlyPlayedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listRecentlyPlayedSong: StateFlow<List<SongEntity>> get() = _listRecentlyPlayedSong

    /**
     * True while the suggestions are being fetched.
     *
     * The other lists read the local database and are on screen in the same frame; this one waits
     * on the network, and an empty list with no explanation reads as "nothing to suggest" rather
     * than "still asking".
     */
    private val _mightLikeLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val mightLikeLoading: StateFlow<Boolean> get() = _mightLikeLoading

    /**
     * One month's top songs, filled in only once a route names the month.
     *
     * Unlike the four lists above it is not started in [init] and not observed: there is no
     * "current" month here — the screen may be showing any of the last twelve — so loading is
     * driven by [getMonthlyRecapSong].
     */
    private val _listMonthlyRecapSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMonthlyRecapSong: StateFlow<List<SongEntity>> get() = _listMonthlyRecapSong

    /**
     * The name of the month [_listMonthlyRecapSong] currently holds, resolved when it was loaded.
     *
     * Resolved there rather than where the queue is built because "Recap January" needs a format
     * argument, and the only `getString` that takes one suspends — [playAll] and [shuffle] do not.
     * Loading already runs in a coroutine, so the name comes free at the one moment it is knowable.
     */
    private var loadedRecapName: String? = null

    init {
        getFavoriteSong()
        getFollowedArtist()
        getMostPlayedSong()
        getDownloadedSong()
        getRediscoverSong()
        getMightLikeSong()
        getRecentlyPlayedSong()
    }

    /**
     * Tracks the listener has never played, related to the ones they play most.
     *
     * The only list here that asks the network, and the only one that can be empty for a reason
     * other than an empty library: the seeds come from the listening history, so a library with
     * fewer than [MIGHT_LIKE_MIN_SEEDS] played tracks has nothing to reason from.
     *
     * Seeds are drawn at random from the top [MIGHT_LIKE_SEED_POOL] rather than taken in order.
     * Taken in order the list is the same every time it is opened, which is the opposite of what a
     * discovery list is for; drawn from the pool it varies while still being anchored to what the
     * listener actually plays.
     */
    private fun getMightLikeSong() {
        viewModelScope.launch {
            _mightLikeLoading.value = true
            try {
                val seeds =
                    analyticsRepository
                        .queryTopPlayedSongsInRange(
                            startTimestamp = now().date.minus(MIGHT_LIKE_HISTORY_DAYS, DateTimeUnit.DAY).atTime(0, 0),
                            endTimestamp = now(),
                        ).firstOrNull()
                        .orEmpty()
                        .take(MIGHT_LIKE_SEED_POOL)
                if (seeds.size < MIGHT_LIKE_MIN_SEEDS) {
                    _listMightLikeSong.value = emptyList()
                    return@launch
                }
                val chosen = seeds.shuffled().take(MIGHT_LIKE_SEEDS)
                val seedIds = chosen.map { it.videoId }.toSet()
                // Asked in parallel: each seed is its own round trip, and done in sequence the
                // screen would wait for the sum of them.
                val candidates =
                    coroutineScope {
                        chosen
                            .map { seed ->
                                async {
                                    (
                                        songRepository
                                            .getRelatedData(seed.videoId)
                                            .firstOrNull { it is Resource.Success || it is Resource.Error }
                                            as? Resource.Success
                                    )?.data?.first.orEmpty()
                                }
                            }.awaitAll()
                    }.flatten()
                        .distinctBy { it.videoId }
                        // A seed's own radio leads with neighbouring tracks, but the seed can
                        // still come back through another seed's radio.
                        .filter { it.videoId !in seedIds }
                // The whole point: what is left is only what the listener has NOT heard. Asked in
                // one query rather than per candidate — a hundred round trips to the database to
                // filter a hundred rows is the shape that makes a list feel slow.
                val alreadyPlayed = analyticsRepository.queryAlreadyPlayed(candidates.map { it.videoId }).toSet()
                _listMightLikeSong.value =
                    candidates
                        .filterNot { it.videoId in alreadyPlayed }
                        .take(MIGHT_LIKE_LIMIT)
                        .map { it.toSongEntity() }
            } finally {
                _mightLikeLoading.value = false
            }
        }
    }

    /**
     * Tracks the listener used to play often and has since let go.
     *
     * Started in [init] like the other four rather than on demand: it has no parameter to wait for
     * — the window is always "the last [REDISCOVER_QUIET_DAYS] days" — so there is nothing a route
     * could tell it that it does not already know.
     */
    private fun getRediscoverSong() {
        viewModelScope.launch {
            analyticsRepository
                .queryRediscoverTracks(
                    goneQuietSince =
                        now()
                            .date
                            .minus(REDISCOVER_QUIET_DAYS, DateTimeUnit.DAY)
                            .atTime(0, 0),
                    minPlays = REDISCOVER_MIN_PLAYS,
                    limit = REDISCOVER_QUERY_LIMIT,
                ).collectLatest { rows ->
                    _listRediscoverSong.value =
                        songRepository
                            // One query for the whole ranking, not one per row. The batch keeps
                            // the input order and drops ids it cannot resolve, which is exactly
                            // the contract the per-row `mapNotNull` had — so this is still
                            // resolved THEN capped, and a row whose song has since been swept
                            // from the library still leaves no gap in the list.
                            .getSongsByListVideoId(rows.map { it.videoId })
                            .firstOrNull()
                            .orEmpty()
                            .take(REDISCOVER_LIMIT)
                }
        }
    }

    /**
     * The long form of the Library tab's Recently played shelf: newest first, one row per track.
     *
     * Same two steps as the shelf — a window of the latest plays, each id's first appearance kept,
     * then one batch lookup — so the page opened from "See all" starts with exactly the rows the
     * shelf showed. The window bounds the batch under SQLite's 999 bound variables on the oldest
     * supported devices; a listener who repeats a lot gets fewer than [RECENTLY_PLAYED_LIMIT]
     * rows rather than a query that throws.
     */
    private fun getRecentlyPlayedSong() {
        viewModelScope.launch {
            val ids =
                analyticsRepository
                    .getPlaybackEventsByOffset(offset = 0, limit = RECENTLY_PLAYED_EVENT_WINDOW)
                    .firstOrNull()
                    .orEmpty()
                    .map { it.videoId }
                    .distinct()
            _listRecentlyPlayedSong.value =
                songRepository
                    .getSongsByListVideoId(ids)
                    .firstOrNull()
                    .orEmpty()
                    .filterNot { it.inLibrary == REMOVED_SONG_DATE_TIME }
                    .take(RECENTLY_PLAYED_LIMIT)
        }
    }

    private fun getFavoriteSong() {
        viewModelScope.launch {
            songRepository.getLikedSongs().collectLatest { likedSong ->
                _listFavoriteSong.value =
                    likedSong.sortedByDescending {
                        it.favoriteAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    private fun getFollowedArtist() {
        viewModelScope.launch {
            artistRepository.getFollowedArtists().collectLatest { followedArtist ->
                _listFollowedArtist.value =
                    followedArtist.sortedByDescending {
                        it.followedAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    private fun getMostPlayedSong() {
        viewModelScope.launch {
            songRepository.getMostPlayedSongs().collectLatest { mostPlayedSong ->
                _listMostPlayedSong.value = mostPlayedSong.sortedByDescending { it.totalPlayTime }
            }
        }
    }

    private fun getDownloadedSong() {
        viewModelScope.launch {
            songRepository.getDownloadedSongs().collectLatest { downloadedSong ->
                _listDownloadedSong.value =
                    (downloadedSong ?: emptyList()).sortedByDescending {
                        it.downloadedAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    /**
     * One calendar month's top songs, newest ranking first.
     *
     * Same two steps the Analytics screen's top-tracks list takes — the ranking from
     * [com.maxrave.domain.repository.AnalyticsRepository], then each row's `videoId` paired with
     * its stored [SongEntity] — so a recap and the Analytics list can never disagree about what
     * was played.
     *
     * The month's bounds are local wall-clock and inclusive at both ends, matching
     * `WrappedViewModel.yearRange`: the query underneath matches with `BETWEEN`, so ending at
     * midnight on the 1st of the next month would drop the last day of this one.
     */
    fun getMonthlyRecapSong(recap: LibraryDynamicPlaylistType.MonthlyRecap) {
        viewModelScope.launch {
            loadedRecapName = recapName(recap)
            val firstDay = LocalDate(recap.year, recap.month, 1)
            val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            analyticsRepository
                .queryTopPlayedSongsInRange(
                    startTimestamp = firstDay.atTime(0, 0),
                    endTimestamp = lastDay.atTime(23, 59, 59),
                ).collectLatest { rows ->
                    _listMonthlyRecapSong.value =
                        songRepository
                            // One query, as in the rediscover loader above. Capped after
                            // resolving, not before: a row whose song row is gone would otherwise
                            // leave a "top 50" 49 long. The query already stops at 100, so this
                            // reaches at most that far.
                            .getSongsByListVideoId(rows.map { it.videoId })
                            .firstOrNull()
                            .orEmpty()
                            .take(MONTHLY_RECAP_LIMIT)
                }
        }
    }

    /** "Recap January", or "Recap January 2025" once the year stops being obvious. */
    private suspend fun recapName(recap: LibraryDynamicPlaylistType.MonthlyRecap): String {
        val month =
            org.jetbrains.compose.resources
                .getString(monthFullNameResource(Month(recap.month)))
        return if (recap.year == now().date.year) {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month, month)
        } else {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month_year, month, recap.year.toString())
        }
    }

    /**
     * What the Now Playing screen calls the queue this screen started.
     *
     * A recap cannot answer [LibraryDynamicPlaylistType.name] with its own name, so it is the one
     * case read from the name resolved at load time instead.
     */
    private fun playlistName(type: LibraryDynamicPlaylistType): String {
        val name =
            when (type) {
                is LibraryDynamicPlaylistType.MonthlyRecap -> loadedRecapName ?: getString(type.name())
                else -> getString(type.name())
            }
        return "${getString(Res.string.playlist)} $name"
    }

    fun playSong(
        videoId: String,
        type: LibraryDynamicPlaylistType,
    ) {
        val (targetList, playTrack) =
            when (type) {
                LibraryDynamicPlaylistType.Favorite -> listFavoriteSong.value to listFavoriteSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Downloaded -> listDownloadedSong.value to listDownloadedSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Followed -> return
                LibraryDynamicPlaylistType.MostPlayed -> listMostPlayedSong.value to listMostPlayedSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Rediscover -> listRediscoverSong.value to listRediscoverSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.MightLike -> listMightLikeSong.value to listMightLikeSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.RecentlyPlayed ->
                    listRecentlyPlayedSong.value to listRecentlyPlayedSong.value.find { it.videoId == videoId }
                is LibraryDynamicPlaylistType.MonthlyRecap ->
                    listMonthlyRecapSong.value to listMonthlyRecapSong.value.find { it.videoId == videoId }
                else -> return
            }
        if (playTrack == null) return
        setQueueData(
            QueueData.Data(
                listTracks = targetList.toArrayListTrack(),
                firstPlayedTrack = playTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            playTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            targetList.indexOf(playTrack).coerceAtLeast(0),
        )
    }

    private fun getSongList(type: LibraryDynamicPlaylistType): List<SongEntity> =
        when (type) {
            LibraryDynamicPlaylistType.Favorite -> listFavoriteSong.value
            LibraryDynamicPlaylistType.Downloaded -> listDownloadedSong.value
            LibraryDynamicPlaylistType.MostPlayed -> listMostPlayedSong.value
            LibraryDynamicPlaylistType.Rediscover -> listRediscoverSong.value
            LibraryDynamicPlaylistType.MightLike -> listMightLikeSong.value
            LibraryDynamicPlaylistType.RecentlyPlayed -> listRecentlyPlayedSong.value
            is LibraryDynamicPlaylistType.MonthlyRecap -> listMonthlyRecapSong.value
            else -> emptyList()
        }

    fun playAll(type: LibraryDynamicPlaylistType) {
        val targetList = getSongList(type)
        val firstTrack = targetList.firstOrNull() ?: return
        setQueueData(
            QueueData.Data(
                listTracks = targetList.toArrayListTrack(),
                firstPlayedTrack = firstTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            firstTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            0,
        )
    }

    fun shuffle(type: LibraryDynamicPlaylistType) {
        val targetList = getSongList(type)
        if (targetList.isEmpty()) return
        val shuffledList = targetList.shuffled()
        val firstTrack = shuffledList.first()
        setQueueData(
            QueueData.Data(
                listTracks = shuffledList.toArrayListTrack(),
                firstPlayedTrack = firstTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            firstTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            0,
        )
    }

    companion object {
        /** Songs a monthly recap holds. The ranking underneath already stops at 100. */
        private const val MONTHLY_RECAP_LIMIT = 50

        /**
         * How long a track must have been untouched to count as let go. Two months is long enough
         * that a track merely between rotations is not offered back, and short enough that the
         * list still has something in it after a few months of use.
         */
        private const val REDISCOVER_QUIET_DAYS = 60

        /**
         * A track played once and dropped was never in rotation to begin with — that is a track
         * the listener did not like, and offering it back is worse than offering nothing.
         */
        private const val REDISCOVER_MIN_PLAYS = 3

        private const val REDISCOVER_QUERY_LIMIT = 100
        private const val REDISCOVER_LIMIT = 50

        /** How far back the suggestions look for something to reason from. */
        private const val MIGHT_LIKE_HISTORY_DAYS = 90

        /** The pool the seeds are drawn from, and the floor below which there is nothing to draw. */
        private const val MIGHT_LIKE_SEED_POOL = 20
        private const val MIGHT_LIKE_MIN_SEEDS = 3

        /** One network round trip each, so this is a cost as much as a setting. */
        private const val MIGHT_LIKE_SEEDS = 4
        private const val MIGHT_LIKE_LIMIT = 50

        private const val RECENTLY_PLAYED_LIMIT = 200

        /** Plays read behind the 200 rows; distinct ids out of it stay under the 999 bound. */
        private const val RECENTLY_PLAYED_EVENT_WINDOW = 600
    }
}