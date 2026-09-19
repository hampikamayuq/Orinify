package com.maxrave.simpmusic.expect

import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.koin.mp.KoinPlatform.getKoin

// Touch exploration (TalkBack and equivalents) is the signal that matters here: it is what turns a
// tap into a two-step "hear it, then double-tap" gesture, which is also what makes reading a card's
// announcement take far longer than the reel's fixed hold.
actual fun isAccessibilityServiceActive(): Boolean {
    val context: Context = getKoin().get()
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    return accessibilityManager?.isTouchExplorationEnabled == true
}
