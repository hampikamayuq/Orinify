package com.maxrave.simpmusic.expect

// Desktop has no equivalent touch-exploration signal to read here, so the reel's auto-advance is
// left ungated by this check on that platform.
actual fun isAccessibilityServiceActive(): Boolean = false
