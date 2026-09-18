package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Whether the user has asked the system to stop animating.
 *
 * Android has no "reduce motion" switch. The accessibility setting **Remove animations** works by
 * setting `Settings.Global.ANIMATOR_DURATION_SCALE` to `0`, and that scale is the only signal the
 * platform gives — it is what the framework's own animators read, so it is what a Compose
 * animation should read too. Desktop exposes nothing comparable, so it answers `false`.
 *
 * Read once per composition of the caller; the setting changes rarely enough that no listener is
 * worth its cost on a list item.
 */
@Composable
expect fun rememberReduceMotion(): Boolean
