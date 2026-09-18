package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artist_mix_unavailable
import simpmusic.composeapp.generated.resources.might_like
import simpmusic.composeapp.generated.resources.might_like_subtitle
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.rediscover
import simpmusic.composeapp.generated.resources.rediscover_subtitle

/**
 * The half of Home that comes from this device's own listening, above the half that comes from
 * YouTube.
 *
 * It is a view model of its own rather than more state on [HomeViewModel], because the two halves
 * fail independently: the YouTube half is one paginated network response that can time out or come
 * back empty, and this half is three unrelated sources — two SQL queries and, for You might like, a
 * fan-out of radio requests. Folding them together would mean one slow or failed source deciding
 * whether the other renders, and the personal lists are the ones that must survive a bad network.
 *
 * The shelves are plain [HomeItem]s, so they draw through the same shelf component every YouTube
 * row already uses and there is no second card design to keep in step. That works because a
 * [Content] carrying a `videoId`, no `playlistId`/`browseId` and a SQUARE thumbnail is exactly what
 * `HomeItem` routes to its song card — and that card's tap already starts a radio. Artist mixes
 * cannot use that route, and get their own shelf; see [artistMixes].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonalHomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {
    /**
     * Whether any of this has anything behind it. With local tracking off `playback_event` is
     * empty, so all three shelves would be headings over nothing.
     */
    val localTrackingEnabled = dataStoreManager.localTrackingEnabled.mapLatest { it == DataStoreManager.TRUE }

    private val _songShelves: MutableStateFlow<List<HomeItem>> = MutableStateFlow(emptyList())

    /** Rediscover and You might like, ready to render, in that order. Empty ones are not included. */
    val songShelves: StateFlow<List<HomeItem>> get() = _songShelves.asStateFlow()

    /**
     * The artists this listener plays most, each standing for a radio.
     *
     * Kept apart from [songShelves] because it cannot be a [Content]: the generic shelf routes an
     * artist to the artist PAGE, and the mix needs the artist's radio endpoint, which is only
     * known after fetching that page. A card that opened the page instead would be lying about
     * what it does, so this draws through `ArtistMixShelf` and plays through [playArtistMix].
     */
    private val _artistMixes: MutableStateFlow<List<ArtistEntity>> = MutableStateFlow(emptyList())
    val artistMixes: StateFlow<List<ArtistEntity>> get() = _artistMixes.asStateFlow()

    /** The channel id whose mix is being resolved, or null. Two round trips, so the card says so. */
    private val _loadingArtistMix: MutableStateFlow<String?> = MutableStateFlow(null)
    val loadingArtistMix: StateFlow<String?> get() = _loadingArtistMix.asStateFlow()

    /**
     * True once every source of a [load] has answered, empty or not. Home needs it to tell "not
     * enough listening yet" from "still querying": Principle 3 says an empty personal list must
     * say so, and saying it during the first 200ms of every open would be a lie that flickers.
     */
    private val _loaded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> get() = _loaded.asStateFlow()

    init {
        load()
    }

    /**
     * Loads all three, each in its own coroutine.
     *
     * Separate jobs on purpose: Rediscover and Artist mixes are local and land in milliseconds,
     * while You might like fans out to one radio request per seed. Awaiting them together would
     * hold the two instant shelves behind the slow one, on the screen that opens the app.
     */
    fun load() {
        viewModelScope.launch {
            _loaded.value = false
            if (dataStoreManager.localTrackingEnabled.firstOrNull() != DataStoreManager.TRUE) {
                _songShelves.value = emptyList()
                _artistMixes.value = emptyList()
                _loaded.value = true
                return@launch
            }
            val rediscover = async { rediscoverSongs() }
            val mightLike = async { mightLikeSongs() }
            val artists = launch { _artistMixes.value = topArtists() }
            val shelves = mutableListOf<HomeItem>()
            rediscover.await().takeIf { it.isNotEmpty() }?.let {
                shelves += songShelf(it, getString(Res.string.rediscover), getString(Res.string.rediscover_subtitle))
                // Published as soon as it resolves rather than with its slower sibling: this one
                // is a single indexed query and there is no reason for it to wait.
                _songShelves.value = shelves.toList()
            }
            mightLike.await().takeIf { it.isNotEmpty() }?.let {
                shelves += songShelf(it, getString(Res.string.might_like), getString(Res.string.might_like_subtitle))
                _songShelves.value = shelves.toList()
            }
            artists.join()
            _loaded.value = true
        }
    }

    /** Tracks played often before the cutoff and not once since. One query, then one batch lookup. */
    private suspend fun rediscoverSongs(): List<SongEntity> =
        analyticsRepository
            .queryRediscoverTracks(
                goneQuietSince = now().date.minus(REDISCOVER_QUIET_DAYS, DateTimeUnit.DAY).atTime(0, 0),
                minPlays = REDISCOVER_MIN_PLAYS,
                limit = SHELF_QUERY_LIMIT,
            ).firstOrNull()
            .orEmpty()
            .let { rows ->
                songRepository
                    .getSongsByListVideoId(rows.map { it.videoId })
                    .firstOrNull()
                    .orEmpty()
                    .take(SHELF_LIMIT)
            }

    /**
     * Tracks the listener has never played, drawn from the radios around the ones they play most.
     *
     * The only shelf on Home that asks the network. Seeds are taken at random from the top pool so
     * the shelf is not the same every open, which is the opposite of what a discovery list is for.
     */
    private suspend fun mightLikeSongs(): List<SongEntity> {
        val seeds =
            analyticsRepository
                .queryTopPlayedSongsInRange(
                    startTimestamp = now().date.minus(MIGHT_LIKE_HISTORY_DAYS, DateTimeUnit.DAY).atTime(0, 0),
                    endTimestamp = now(),
                ).firstOrNull()
                .orEmpty()
                .take(MIGHT_LIKE_SEED_POOL)
        if (seeds.size < MIGHT_LIKE_MIN_SEEDS) return emptyList()
        val chosen = seeds.shuffled().take(MIGHT_LIKE_SEEDS)
        val seedIds = chosen.map { it.videoId }.toSet()
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
                .filter { it.videoId !in seedIds }
        val alreadyPlayed = analyticsRepository.queryAlreadyPlayed(candidates.map { it.videoId }).toSet()
        return candidates
            .filterNot { it.videoId in alreadyPlayed }
            .take(SHELF_LIMIT)
            .map { it.toSongEntity() }
    }

    private suspend fun topArtists(): List<ArtistEntity> =
        analyticsRepository
            .queryTopArtistsLastXDays(ARTIST_MIX_DAYS)
            .firstOrNull()
            .orEmpty()
            .take(ARTIST_MIX_COUNT)
            .mapNotNull { artistRepository.getArtistOrFetch(it.channelId) }

    /**
     * Wraps songs as a shelf the generic `HomeItem` component can draw.
     *
     * The thumbnail is declared SQUARE, and that is load-bearing rather than cosmetic: `HomeItem`
     * chooses between its song card and its video card by comparing the first thumbnail's width
     * and height. A `SongEntity` only stores a url, so the numbers here are a declaration of
     * intent — these are songs — not a measurement of the image.
     */
    private fun songShelf(
        songs: List<SongEntity>,
        title: String,
        subtitle: String,
    ): HomeItem =
        HomeItem(
            title = title,
            subtitle = subtitle,
            contents =
                songs.map { song ->
                    Content(
                        album = null,
                        artists = song.artistName?.mapIndexed { i, name -> Artist(song.artistId?.getOrNull(i), name) },
                        description = null,
                        isExplicit = song.isExplicit,
                        // Both null so the shelf routes this to its song card: a non-null
                        // playlistId or browseId sends it to a playlist, album or artist page.
                        playlistId = null,
                        browseId = null,
                        thumbnails = listOf(Thumbnail(544, song.thumbnails.orEmpty(), 544)),
                        title = song.title,
                        videoId = song.videoId,
                        views = null,
                        durationSeconds = song.durationSeconds,
                    )
                },
        )

    /**
     * Starts [artist]'s radio.
     *
     * The endpoint comes off the artist's own page rather than a guessed `RDAMVM<videoId>`: that
     * would be a radio around one SONG, which drifts wherever that song's neighbours lead, while
     * `radioId` is the mix YouTube built for the artist. `shuffleId` is the fallback — narrower,
     * but still theirs, and present on pages that carry no radio.
     */
    fun playArtistMix(artist: ArtistEntity) {
        if (_loadingArtistMix.value != null) return
        viewModelScope.launch {
            _loadingArtistMix.value = artist.channelId
            try {
                val browse = artistRepository.getArtistData(artist.channelId).lastOrNull()?.data
                val endpoint = browse?.radioId ?: browse?.shuffleId
                if (endpoint == null) {
                    makeToast(getString(Res.string.artist_mix_unavailable))
                    return@launch
                }
                val radio = songRepository.getRadioFromEndpoint(endpoint).lastOrNull()
                val tracks = radio?.data?.first
                if (radio !is Resource.Success || tracks.isNullOrEmpty()) {
                    makeToast(radio?.message ?: getString(Res.string.artist_mix_unavailable))
                    return@launch
                }
                setQueueData(
                    QueueData.Data(
                        listTracks = tracks,
                        firstPlayedTrack = tracks.first(),
                        playlistId = endpoint.playlistId,
                        playlistName = "\"${artist.name}\" ${getString(Res.string.radio)}",
                        playlistType = PlaylistType.RADIO,
                        continuation = radio.data?.second,
                    ),
                )
                loadMediaItem(tracks.first(), Config.PLAYLIST_CLICK, 0)
            } finally {
                _loadingArtistMix.value = null
            }
        }
    }

    companion object {
        /** A shelf scrolls sideways; past twenty nobody reaches the end. The lists themselves are capped further up. */
        private const val SHELF_LIMIT = 20
        private const val SHELF_QUERY_LIMIT = 100

        // The same policy the Library's own copies of these lists use. Changing one without the
        // other would make Home and Library disagree about what "rediscover" means.
        private const val REDISCOVER_QUIET_DAYS = 60
        private const val REDISCOVER_MIN_PLAYS = 3

        private const val MIGHT_LIKE_HISTORY_DAYS = 90
        private const val MIGHT_LIKE_SEED_POOL = 20
        private const val MIGHT_LIKE_MIN_SEEDS = 3

        /** One network round trip each, so this is a cost as much as a setting. */
        private const val MIGHT_LIKE_SEEDS = 4

        private const val ARTIST_MIX_DAYS = 90
        private const val ARTIST_MIX_COUNT = 12
    }
}
