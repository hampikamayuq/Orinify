package dev.diego.orinify.audio

import com.zionhuang.innertube.models.response.PlayerResponse

private val codecsParameter = Regex(
    pattern = "(?:^|;)\\s*codecs\\s*=\\s*\\\"?([^;\\\"]+)",
    option = RegexOption.IGNORE_CASE,
)

internal fun codecFromMimeType(mimeType: String): String? =
    codecsParameter.find(mimeType)
        ?.groupValues
        ?.getOrNull(1)
        ?.substringBefore(',')
        ?.trim()
        ?.takeIf(String::isNotEmpty)

fun PlayerResponse.StreamingData.Format.toAudioFormatInfo(
    sourceClient: String?,
    isAuthenticated: Boolean,
) = AudioFormatInfo(
    itag = itag,
    mimeType = mimeType.substringBefore(';').trim(),
    codec = codecFromMimeType(mimeType),
    bitrate = bitrate.toLong(),
    sampleRate = audioSampleRate,
    channels = audioChannels,
    contentLength = contentLength,
    audioQuality = audioQuality,
    sourceClient = sourceClient,
    isAuthenticated = isAuthenticated,
)
