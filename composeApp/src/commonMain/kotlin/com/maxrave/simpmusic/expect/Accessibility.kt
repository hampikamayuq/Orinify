package com.maxrave.simpmusic.expect

/**
 * Whether an accessibility service that needs extra time to read the screen — TalkBack and other
 * touch-exploration screen readers — is currently active.
 *
 * Used to hold auto-advancing UI (the Wrapped reel's per-card timer) for as long as such a service
 * is on, alongside the existing press/busy/scroll guards: a fixed hold long enough for a sighted
 * user is routinely too short for a screen-reader announcement to finish.
 */
expect fun isAccessibilityServiceActive(): Boolean
