package dev.diego.orinify.audio

/**
 * Sanitized, immutable description of a format returned by the player endpoint.
 *
 * Deliberately excludes the stream URL, request headers, cookies and account identifiers so it is
 * safe to pass to local diagnostics and UI code.
 */
data class AudioFormatInfo(
    val itag: Int?,
    val mimeType: String,
    val codec: String?,
    val bitrate: Long?,
    val sampleRate: Int?,
    val channels: Int?,
    val contentLength: Long?,
    val audioQuality: String?,
    val sourceClient: String?,
    val isAuthenticated: Boolean,
) {
    val container: String
        get() = when (mimeType.substringBefore(';').trim().lowercase()) {
            "audio/webm" -> "WebM"
            "audio/mp4", "audio/m4a" -> "MP4"
            "audio/ogg" -> "Ogg"
            else -> mimeType.substringAfter('/', mimeType).substringBefore(';')
                .ifBlank { "Unknown" }
                .replaceFirstChar(Char::uppercase)
        }
}
