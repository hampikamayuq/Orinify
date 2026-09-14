package dev.diego.orinify.settings

import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Preferences owned by the fork. Kept out of the upstream constants file so a merge from InnerTune
 * never has to resolve them.
 */
val AllowPipedFallbackKey = booleanPreferencesKey("orinifyAllowPipedFallback")
