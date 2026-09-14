package dev.diego.orinify.audio

/** Builders for format fixtures, so each test states only the fields it cares about. */
object TestAudioFormats {
    fun audioFormat(
        itag: Int? = null,
        mimeType: String = "audio/webm",
        codec: String? = "opus",
        bitrate: Long? = 128_000,
        sampleRate: Int? = 48_000,
        channels: Int? = 2,
        contentLength: Long? = 1_000_000,
        audioQuality: String? = "AUDIO_QUALITY_MEDIUM",
        sourceClient: String? = "ANDROID_MUSIC",
        isAuthenticated: Boolean = false,
    ) = AudioFormatInfo(
        itag = itag,
        mimeType = mimeType,
        codec = codec,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        contentLength = contentLength,
        audioQuality = audioQuality,
        sourceClient = sourceClient,
        isAuthenticated = isAuthenticated,
    )

    fun opus(itag: Int, bitrate: Long?) = audioFormat(
        itag = itag,
        mimeType = "audio/webm",
        codec = "opus",
        bitrate = bitrate,
    )

    fun aac(itag: Int, bitrate: Long?) = audioFormat(
        itag = itag,
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = bitrate,
    )
}
