package com.zionhuang.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.zionhuang.innertube.YouTube
import com.zionhuang.music.R
import com.zionhuang.music.constants.AudioQuality
import com.zionhuang.music.constants.AudioQualityKey
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.FormatEntity
import com.zionhuang.music.di.DownloadCache
import com.zionhuang.music.di.PlayerCache
import com.zionhuang.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diego.orinify.audio.FormatResolver
import dev.diego.orinify.audio.LegacyFormatPolicy
import dev.diego.orinify.audio.codecFromMimeType
import dev.diego.orinify.audio.toAudioFormatInfo
import dev.diego.orinify.di.DownloadStreamUrls
import dev.diego.orinify.diagnostics.ResolverShadow
import dev.diego.orinify.network.StreamUrlCache
import dev.diego.orinify.playback.networkProfile
import dev.diego.orinify.playback.toPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
    @DownloadStreamUrls private val songUrlCache: StreamUrlCache,
    private val resolverShadow: ResolverShadow,
) {
    private val appContext = context
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val dataSourceFactory = ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient.Builder()
                        .proxy(YouTube.proxy)
                        .build()
                )
            )
    ) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        val length = if (dataSpec.length >= 0) dataSpec.length else 1

        if (playerCache.isCached(mediaId, dataSpec.position, length)) {
            return@Factory dataSpec
        }

        songUrlCache.get(mediaId)?.let { cachedUrl ->
            return@Factory dataSpec.withUri(cachedUrl.toUri())
        }

        val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
        val playerResponse = runBlocking(Dispatchers.IO) {
            YouTube.player(mediaId)
        }.getOrThrow()
        if (playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(playerResponse.playabilityStatus.reason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
        }

        val streamingData = playerResponse.streamingData
        // Only audio-only formats carrying a usable URL can be downloaded.
        val audioFormats = streamingData?.adaptiveFormats
            ?.filter { it.isAudio && !it.url.isNullOrEmpty() }
            .orEmpty()

        val audioFormatInfos = audioFormats.map {
            it.toAudioFormatInfo(sourceClient = null, isAuthenticated = YouTube.isLoggedIn)
        }
        val qualityPreference = audioQuality.toPreference()
        val networkProfile = connectivityManager.networkProfile()

        // Downloads run the same comparison as playback, into the same counters.
        val legacyIndex = LegacyFormatPolicy.select(audioFormatInfos, qualityPreference, networkProfile)
        resolverShadow.record(
            videoId = mediaId,
            candidates = audioFormatInfos,
            legacyIndex = legacyIndex,
            selection = FormatResolver.resolve(
                candidates = audioFormatInfos,
                target = FormatResolver.qualityTargetFor(qualityPreference, networkProfile),
            ),
        )

        // Prefer the itag already played, but fall back to the regular selection when the
        // responding client does not offer it, instead of failing the download.
        val reusedFormat = playedFormat?.let { played -> audioFormats.find { it.itag == played.itag } }
        val format = reusedFormat
            ?: legacyIndex?.let(audioFormats::getOrNull)
            ?: throw PlaybackException(
                appContext.getString(R.string.error_no_stream),
                null,
                MusicService.ERROR_CODE_NO_STREAM
            )

        if (playedFormat != null && reusedFormat == null) {
            // Same hazard as playback: this pipeline reads through the player cache, which keys its
            // spans by media id alone, so bytes of the previous encoding would be written into the
            // download. Drop them before fetching the replacement.
            playerCache.removeResource(mediaId)
        }

        val streamUrl = format.url
            ?: throw PlaybackException(
                appContext.getString(R.string.error_no_stream),
                null,
                MusicService.ERROR_CODE_NO_STREAM
            )
        val contentLength = format.contentLength
        // Specify range to avoid YouTube's throttling. When the server did not report a length,
        // request the whole resource: a made-up ceiling silently truncates long tracks.
        val downloadUrl = contentLength?.let { "$streamUrl&range=0-$it" } ?: streamUrl

        // The entity requires a content length, so persist the details only when it is known.
        if (contentLength != null) {
            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.substringBefore(';').trim(),
                        codecs = codecFromMimeType(format.mimeType).orEmpty(),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = contentLength,
                        loudnessDb = playerResponse.playerConfig?.audioConfig?.loudnessDb
                    )
                )
            }
        }

        songUrlCache.put(mediaId, downloadUrl, streamingData?.expiresInSeconds ?: 0)
        dataSpec.withUri(downloadUrl.toUri())
    }
    val downloadNotificationHelper = DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)
    val downloadManager: DownloadManager = DownloadManager(context, databaseProvider, downloadCache, dataSourceFactory, Executor(Runnable::run)).apply {
        maxParallelDownloads = 3
        addListener(
            ExoDownloadService.TerminalStateNotificationHelper(
                context = context,
                notificationHelper = downloadNotificationHelper,
                nextNotificationId = ExoDownloadService.NOTIFICATION_ID + 1
            )
        )
    }
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    fun getDownload(songId: String?): Flow<Download?> = downloads.map { it[songId] }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            set(download.request.id, download)
                        }
                    }
                }
            }
        )
    }
}