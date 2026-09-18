package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// No desktop OS exposes a reduce-motion preference through anything the JVM can reach without a
// native binding, so Desktop always animates.
@Composable
actual fun rememberReduceMotion(): Boolean = false
