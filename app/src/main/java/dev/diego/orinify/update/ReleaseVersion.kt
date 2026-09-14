package dev.diego.orinify.update

/**
 * Normalizes a GitHub release identifier so it can be compared with `BuildConfig.VERSION_NAME`.
 *
 * Orinify tags releases as `v0.5.10-q1` while the build reports `0.5.10-q1`. Comparing the two
 * verbatim makes every install look outdated forever, so the tag prefix is stripped before the
 * comparison. Only a `v` directly followed by a digit is treated as a prefix, so a version that
 * legitimately starts with a letter is left alone.
 *
 * Returns null when the value is missing or unusable, so the caller can fall back or fail instead
 * of comparing against an empty string.
 */
fun normalizeReleaseVersionName(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return null
    val withoutTagPrefix = when {
        trimmed.length > 1 && (trimmed[0] == 'v' || trimmed[0] == 'V') && trimmed[1].isDigit() ->
            trimmed.substring(1)

        else -> trimmed
    }
    return withoutTagPrefix.ifEmpty { null }
}
