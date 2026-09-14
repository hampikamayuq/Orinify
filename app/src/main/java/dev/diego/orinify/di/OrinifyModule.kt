package dev.diego.orinify.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diego.orinify.network.StreamUrlCache
import javax.inject.Qualifier
import javax.inject.Singleton

/** Stream URLs resolved for playback. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackStreamUrls

/** Stream URLs resolved for downloads, which carry an explicit byte range. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadStreamUrls

/**
 * Bindings owned by the Orinify fork. Kept out of `com.zionhuang.music.di` so upstream merges do
 * not touch it.
 */
@Module
@InstallIn(SingletonComponent::class)
object OrinifyModule {
    /**
     * Playback and downloads keep separate caches on purpose: the download URL carries a
     * `range` parameter that must never reach the player, and the player URL is chunked by the
     * data source instead.
     */
    @Provides
    @Singleton
    @PlaybackStreamUrls
    fun providePlaybackStreamUrlCache(): StreamUrlCache = StreamUrlCache()

    @Provides
    @Singleton
    @DownloadStreamUrls
    fun provideDownloadStreamUrlCache(): StreamUrlCache = StreamUrlCache()
}
