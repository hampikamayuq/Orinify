package com.maxrave.simpmusic.expect.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// "Remove animations" (Settings → Accessibility) sets ANIMATOR_DURATION_SCALE to 0; there is no
// dedicated reduce-motion flag on Android. Default 1f when the key is absent, i.e. animate.
@Composable
actual fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
