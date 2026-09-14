package dev.diego.orinify.playback

import android.net.ConnectivityManager
import com.zionhuang.music.constants.AudioQuality
import dev.diego.orinify.audio.AudioQualityPreference
import dev.diego.orinify.audio.NetworkProfile

/**
 * Adapters between the app's existing settings and the fork's audio domain.
 *
 * They live here, and not in `dev.diego.orinify.audio`, so the domain keeps no dependency on
 * Android or on upstream constants, per the dependency rules in docs/ARCHITECTURE-Q.md.
 */
fun AudioQuality.toPreference(): AudioQualityPreference =
    when (this) {
        AudioQuality.AUTO -> AudioQualityPreference.AUTO
        AudioQuality.HIGH -> AudioQualityPreference.HIGH
        AudioQuality.LOW -> AudioQualityPreference.LOW
    }

fun ConnectivityManager.networkProfile(): NetworkProfile =
    if (isActiveNetworkMetered) NetworkProfile.METERED else NetworkProfile.UNMETERED
