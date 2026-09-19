package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.PlaybackEventEntity
import com.maxrave.domain.data.entities.analytics.query.TopPlayedAlbum
import com.maxrave.domain.data.entities.analytics.query.TopPlayedArtist
import com.maxrave.domain.data.entities.analytics.query.TopPlayedTracks
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _analyticsUIState: MutableStateFlow<AnalyticsUiState> =
        MutableStateFlow(AnalyticsUiState())
    val analyticsUIState: StateFlow<AnalyticsUiState> get() = _analyticsUIState.asStateFlow()

    // One job per top list, cancelled before its replacement starts. Each loader collects a flow
    // inside its own launch, and nothing used to stop the previous one: step back two periods
    // quickly and the first load could land after the second and overwrite it, putting the wrong
    // period's list on screen. Replacing the job makes the latest request the only one that writes.
    private var topTracksJob: Job? = null
    private var topArtistsJob: Job? = null
    private var topAlbumsJob: Job? = null

    // Set by [showRange]. A view model opened for one fixed range must not have its lists replaced
    // by the navigator's own "latest period" load, which [init] starts asynchronously and which can
    // therefore arrive after the range was set.
    private var rangePinned = false

    init {
        getScrobblesCount()
        getArtistCount()
        getTotalListenTime()
        getRecentlyRecord()
        viewModelScope.launch {
            val saved = dataStoreManager.getString(ANALYTICS_DAY_RANGE_KEY).firstOrNull()
            val dayRange = saved?.let {
                runCatching { AnalyticsUiState.DayRange.valueOf(it) }.getOrNull()
            } ?: AnalyticsUiState.DayRange.LAST_7_DAYS
            _analyticsUIState.update { it.copy(dayRange = dayRange) }
            loadPeriod()
        }
    }

    companion object {
        private const val ANALYTICS_DAY_RANGE_KEY = "analytics_day_range"

        /** Entries per top list. The screen prints five; the "see all" beside each list is its own query. */
        private const val TOP_COUNT = 5
    }

    /**
     * The span the screen is showing, as [start, end].
     *
     * [offset] counts periods BACKWARDS from now: 0 is the current one, 1 the one before it. The
     * whole navigator is this one function; every delta is it against [previousRangeFor] — the
     * range queries underneath already existed and were only ever used for "this year".
     *
     * Ends are inclusive-by-day: `end` is the last moment of its day, so a play at 23:59 belongs to
     * the period it happened in rather than to the next one.
     */
    private fun rangeFor(
        dayRange: AnalyticsUiState.DayRange,
        offset: Int,
    ): Pair<LocalDateTime, LocalDateTime> {
        val today = now().date
        return if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
            val year = today.year - offset
            val start = LocalDate(year, 1, 1)
            // The current year stops at today; an earlier one runs to its own 31 December.
            val end = if (offset == 0) today else LocalDate(year, 12, 31)
            start.atTime(0, 0) to end.atTime(23, 59, 59)
        } else {
            val length = dayRange.lengthInDays
            val end = today.minus(DatePeriod(days = offset * length))
            val start = end.minus(DatePeriod(days = length - 1))
            start.atTime(0, 0) to end.atTime(23, 59, 59)
        }
    }

    /**
     * The span every delta on the screen is measured against.
     *
     * For the day ranges it is simply the period before. For THIS_YEAR it is the same stretch of
     * the year before — 1 Jan to the same month and day — and NOT the whole previous year: a
     * year-to-date against twelve full months reads as a fall on every figure for eleven months of
     * the twelve. A past year is already whole, so its comparison is the whole year before it.
     */
    private fun previousRangeFor(
        dayRange: AnalyticsUiState.DayRange,
        offset: Int,
    ): Pair<LocalDateTime, LocalDateTime> {
        if (dayRange != AnalyticsUiState.DayRange.THIS_YEAR) return rangeFor(dayRange, offset + 1)
        val (start, end) = rangeFor(dayRange, offset)
        // DatePeriod arithmetic clamps 29 Feb to 28 Feb in a common year.
        val previousStart = start.date.minus(DatePeriod(years = 1))
        val previousEnd = end.date.minus(DatePeriod(years = 1))
        return previousStart.atTime(0, 0) to previousEnd.atTime(23, 59, 59)
    }

    private fun loadPeriod() {
        val state = _analyticsUIState.value
        val (start, end) = rangeFor(state.dayRange, state.periodOffset)
        val (previousStart, previousEnd) = previousRangeFor(state.dayRange, state.periodOffset)
        // Pinned means the span was chosen by whoever opened this screen; the navigator's own
        // latest period must not replace it, neither the lists nor the dates that describe them.
        if (!rangePinned) {
            _analyticsUIState.update {
                it.copy(periodStart = start.date, periodEnd = end.date)
            }
            getTopTracks(start, end)
            getTopArtists(start, end)
            getTopAlbums(start, end)
        }
        getScrobblesLineChart(state.dayRange, end.date)
        getPeriodStats(start, end, previousStart, previousEnd)
    }

    /**
     * This period and the one it is compared against, fetched as a matched pair.
     *
     * The previous one is what turns every number on the screen from a quantity into a change. It
     * is deliberately not shown when it is empty: a first-week user comparing against zero would
     * see the same "+∞%" against every single figure. Its dates are published regardless, so the
     * screen can name the span it compared against instead of saying "previous period".
     */
    private fun getPeriodStats(
        start: LocalDateTime,
        end: LocalDateTime,
        previousStart: LocalDateTime,
        previousEnd: LocalDateTime,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    stats = LocalResource.Loading(),
                    previousPeriodStart = previousStart.date,
                    previousPeriodEnd = previousEnd.date,
                )
            }
            val loaded =
                resourceOf {
                    analyticsRepository.getPeriodStats(start, end) to
                        analyticsRepository.getPeriodStats(previousStart, previousEnd)
                }
            val pair = loaded.data
            _analyticsUIState.update {
                if (pair != null) {
                    it.copy(
                        stats = LocalResource.Success(pair.first),
                        previousStats = pair.second.takeIf { p -> !p.isEmpty },
                    )
                } else {
                    it.copy(stats = LocalResource.Error(loaded.message.orEmpty()), previousStats = null)
                }
            }
        }
    }

    /**
     * A load that throws becomes [LocalResource.Error] rather than a flow parked on Loading for the
     * life of the screen. Cancellation is rethrown: a cancelled load is being replaced, not failing.
     */
    private suspend fun <T> resourceOf(block: suspend () -> T): LocalResource<T> =
        try {
            LocalResource.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LocalResource.Error<T>(e.message ?: e::class.simpleName.orEmpty())
        }

    /**
     * The first [TOP_COUNT] rows that resolve to an entity, each paired with it.
     *
     * The query returns up to 100 rows and each resolve is a DB read — for an artist never stored
     * locally, a network round trip — while the screen prints five. Rank is over what survived, so
     * a row whose entity is missing leaves no gap and the list reaches further down to stay full.
     */
    private suspend fun <T, R> List<T>.resolveTop(resolve: suspend (T) -> R?): List<Pair<T, R>> {
        val resolved = mutableListOf<Pair<T, R>>()
        for (row in this) {
            if (resolved.size == TOP_COUNT) break
            val entity = resolve(row) ?: continue
            resolved += row to entity
        }
        return resolved
    }

    /**
     * Shows the top lists for one fixed span, for a screen opened on a period the user picked
     * elsewhere — the Analytics screen hands its visible period to the playlist it opens.
     *
     * Days are inclusive at both ends, the same rule [rangeFor] uses, so a play at 23:59 on the last
     * day is counted in this span and not dropped.
     */
    fun showRange(
        start: LocalDate,
        end: LocalDate,
    ) {
        rangePinned = true
        _analyticsUIState.update { it.copy(periodStart = start, periodEnd = end) }
        val startTime = start.atTime(0, 0)
        val endTime = end.atTime(23, 59, 59)
        getTopTracks(startTime, endTime)
        getTopArtists(startTime, endTime)
        getTopAlbums(startTime, endTime)
    }

    /** Step the window back ([delta] = -1) or forward ([delta] = +1). Never past the present. */
    fun stepPeriod(delta: Int) {
        val next = (_analyticsUIState.value.periodOffset - delta).coerceAtLeast(0)
        if (next == _analyticsUIState.value.periodOffset) return
        _analyticsUIState.update { it.copy(periodOffset = next) }
        loadPeriod()
    }

    private fun getScrobblesCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    scrobblesCount = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalPlaybackEventCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        scrobblesCount = LocalResource.Success(count),
                    )
                }
            }
        }
    }

    private fun getArtistCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    artistCount = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalEventArtistCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        artistCount = LocalResource.Success(count),
                    )
                }
            }
        }
    }

    private fun getTotalListenTime() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    totalListenTimeInSeconds = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalListeningTimeInSeconds().collect { total ->
                _analyticsUIState.update {
                    it.copy(
                        totalListenTimeInSeconds = LocalResource.Success(total),
                    )
                }
            }
        }
    }

    private fun getTopTracks(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        topTracksJob?.cancel()
        topTracksJob = viewModelScope.launch {
            _analyticsUIState.update { it.copy(topTracks = LocalResource.Loading()) }
            analyticsRepository
                .queryTopPlayedSongsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedTracks ->
                    // One query for the whole ranking rather than one per row: the query returns
                    // up to 100, and each `getSongById` is its own flow, dispatcher hop and
                    // statement. Paired back up through a map because the ranking row carries the
                    // play count and the song row does not.
                    val pairs =
                        resourceOf {
                            val songs =
                                songRepository
                                    .getSongsByListVideoId(topPlayedTracks.map { it.videoId })
                                    .firstOrNull()
                                    .orEmpty()
                                    .associateBy { it.videoId }
                            topPlayedTracks.mapNotNull { row -> songs[row.videoId]?.let { row to it } }
                        }
                    _analyticsUIState.update { it.copy(topTracks = pairs) }
                }
        }
    }

    private fun getTopArtists(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        topArtistsJob?.cancel()
        topArtistsJob = viewModelScope.launch {
            _analyticsUIState.update { it.copy(topArtists = LocalResource.Loading()) }
            analyticsRepository
                .queryTopArtistsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedArtists ->
                    val pairs =
                        resourceOf {
                            topPlayedArtists.resolveTop { artistRepository.getArtistOrFetch(it.channelId) }
                        }
                    _analyticsUIState.update { it.copy(topArtists = pairs) }
                }
        }
    }

    private fun getTopAlbums(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        topAlbumsJob?.cancel()
        topAlbumsJob = viewModelScope.launch {
            _analyticsUIState.update { it.copy(topAlbums = LocalResource.Loading()) }
            analyticsRepository
                .queryTopAlbumsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedAlbums ->
                    val pairs =
                        resourceOf {
                            topPlayedAlbums.resolveTop { albumRepository.getAlbum(it.albumBrowseId).lastOrNull() }
                        }
                    _analyticsUIState.update { it.copy(topAlbums = pairs) }
                }
        }
    }

    private fun getRecentlyRecord() {
        viewModelScope.launch {
            analyticsRepository
                .getPlaybackEventsByOffset(
                    offset = 0,
                    limit = 5,
                ).collect { events ->
                    // Same batch as the top tracks above. Only five rows here, so this is about
                    // not leaving the per-row shape lying around to be copied, not about speed.
                    val songs =
                        songRepository
                            .getSongsByListVideoId(events.map { it.videoId })
                            .firstOrNull()
                            .orEmpty()
                            .associateBy { it.videoId }
                    // An empty list is published too: skipping it left the row on Loading for
                    // ever, and "nothing played yet" is a state the screen has to be able to say.
                    events
                        .mapNotNull { event -> songs[event.videoId]?.let { event to it } }
                        .let {
                            _analyticsUIState.update { state ->
                                state.copy(
                                    recentlyRecord = LocalResource.Success(it),
                                )
                            }
                        }
                }
        }
    }

    private fun getScrobblesLineChart(
        dayRange: AnalyticsUiState.DayRange,
        endDate: LocalDate,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    scrobblesLineChart = LocalResource.Loading(),
                )
            }
            val chartTypes =
                when (dayRange) {
                    AnalyticsUiState.DayRange.LAST_7_DAYS -> {
                        (0 until 7).map {
                            AnalyticsUiState.ChartType.Day(
                                day = endDate.minus(DatePeriod(days = it)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_30_DAYS -> {
                        // Newest week first, matching how the day buckets above are ordered.
                        (0 until 4).map { week ->
                            AnalyticsUiState.ChartType.Week(
                                start = endDate.minus(DatePeriod(days = week * 7 + 6)),
                                end = endDate.minus(DatePeriod(days = week * 7)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_90_DAYS -> {
                        (0 until 3).map {
                            AnalyticsUiState.ChartType.Month(
                                month = endDate.minus(DatePeriod(months = it)).month,
                                year = endDate.minus(DatePeriod(months = it)).year,
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.THIS_YEAR -> {
                        val currentMonth = endDate.month
                        (1..currentMonth.number).map {
                            AnalyticsUiState.ChartType.Month(
                                month = kotlinx.datetime.Month(it),
                                year = endDate.year,
                            )
                        }
                    }
                }
            val currentTimeZone = TimeZone.currentSystemDefault()
            val data =
                resourceOf {
                    chartTypes.map {
                        when (it) {
                            is AnalyticsUiState.ChartType.Day -> {
                                val startTimestamp = it.day.atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                val endTimestamp =
                                    it.day
                                        .plus(DatePeriod(days = 1))
                                        .atStartOfDayIn(currentTimeZone)
                                        .toLocalDateTime(currentTimeZone)
                                val count =
                                    analyticsRepository
                                        .getPlaybackEventCountInRange(
                                            startTimestamp = startTimestamp,
                                            endTimestamp = endTimestamp,
                                        ).lastOrNull() ?: 0L
                                Pair(it, count)
                            }

                            is AnalyticsUiState.ChartType.Week -> {
                                val startTimestamp =
                                    it.start.atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                // `end` is inclusive, so the range runs to the start of the day after it.
                                val endTimestamp =
                                    it.end
                                        .plus(DatePeriod(days = 1))
                                        .atStartOfDayIn(currentTimeZone)
                                        .toLocalDateTime(currentTimeZone)
                                val count =
                                    analyticsRepository
                                        .getPlaybackEventCountInRange(
                                            startTimestamp = startTimestamp,
                                            endTimestamp = endTimestamp,
                                        ).lastOrNull() ?: 0L
                                Pair(it, count)
                            }

                            is AnalyticsUiState.ChartType.Month -> {
                                val startTimestamp =
                                    LocalDate(
                                        year = it.year,
                                        month = it.month.number,
                                        day = 1,
                                    ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                val endTimestamp =
                                    if (it.month == kotlinx.datetime.Month.DECEMBER) {
                                        LocalDate(
                                            year = it.year + 1,
                                            month = 1,
                                            day = 1,
                                        ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                    } else {
                                        LocalDate(
                                            year = it.year,
                                            month = it.month.number + 1,
                                            day = 1,
                                        ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                    }
                                val count =
                                    analyticsRepository
                                        .getPlaybackEventCountInRange(
                                            startTimestamp = startTimestamp,
                                            endTimestamp = endTimestamp,
                                        ).lastOrNull() ?: 0L
                                Pair(it, count)
                            }
                        }
                    }
                }
            log("Scrobbles line chart data: ${data.data}")
            _analyticsUIState.update {
                it.copy(
                    scrobblesLineChart = data,
                )
            }
        }
    }

    fun setDayRange(dayRange: AnalyticsUiState.DayRange) {
        // A different range length makes the old offset meaningless — three periods back at
        // 7 days is not three periods back at 90 — so switching always returns to the present.
        _analyticsUIState.update {
            it.copy(
                dayRange = dayRange,
                periodOffset = 0,
            )
        }
        loadPeriod()
        viewModelScope.launch {
            dataStoreManager.putString(ANALYTICS_DAY_RANGE_KEY, dayRange.name)
        }
    }
}

data class AnalyticsUiState(
    val scrobblesCount: LocalResource<Long> = LocalResource.Loading(),
    val artistCount: LocalResource<Long> = LocalResource.Loading(),
    val totalListenTimeInSeconds: LocalResource<Long> = LocalResource.Loading(),
    val dayRange: DayRange = DayRange.LAST_7_DAYS,
    /** Periods back from now: 0 is the present one, 1 the one before it. */
    val periodOffset: Int = 0,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    /**
     * The span [previousStats] was measured over, inclusive at both ends. Set even when
     * [previousStats] is null, so the screen can say WHICH span held nothing.
     */
    val previousPeriodStart: LocalDate? = null,
    val previousPeriodEnd: LocalDate? = null,
    val stats: LocalResource<AnalyticsPeriodStats> = LocalResource.Loading(),
    /** Null when the previous period held nothing — the screen then shows no deltas at all. */
    val previousStats: AnalyticsPeriodStats? = null,
    val recentlyRecord: LocalResource<List<Pair<PlaybackEventEntity, SongEntity>>> = LocalResource.Loading(),
    val topTracks: LocalResource<List<Pair<TopPlayedTracks, SongEntity>>> = LocalResource.Loading(),
    val topArtists: LocalResource<List<Pair<TopPlayedArtist, ArtistEntity>>> = LocalResource.Loading(),
    val topAlbums: LocalResource<List<Pair<TopPlayedAlbum, AlbumEntity>>> = LocalResource.Loading(),
    val scrobblesLineChart: LocalResource<List<Pair<ChartType, Long>>> = LocalResource.Loading(),
) {
    /** True while the window is in the past, so the forward arrow has somewhere to go. */
    val canStepForward: Boolean get() = periodOffset > 0

    enum class DayRange(
        val lengthInDays: Int,
    ) {
        LAST_7_DAYS(7),
        LAST_30_DAYS(30),
        LAST_90_DAYS(90),

        /** Length is unused — a year steps by calendar years, see rangeFor. */
        THIS_YEAR(365),
    }

    sealed class ChartType {
        data class Day(
            val day: LocalDate,
        ) : ChartType()

        /**
         * Seven days, inclusive at both ends.
         *
         * Thirty rows is not a chart, it is a list nobody reads to the end — so the 30-day range
         * buckets by week. Four buckets of exactly seven days, rather than four-and-a-bit covering
         * all thirty: an uneven last bucket would carry more days than the others and draw a
         * longer bar for it, which is the one thing a bar chart must not do.
         */
        data class Week(
            val start: LocalDate,
            val end: LocalDate,
        ) : ChartType()

        data class Month(
            val month: kotlinx.datetime.Month,
            val year: Int,
        ) : ChartType()
    }
}