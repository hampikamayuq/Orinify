package dev.diego.orinify.auth

object WebPlaybackUrl {
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")

    fun forVideo(videoId: String): String? =
        if (videoIdPattern.matches(videoId)) "https://music.youtube.com/watch?v=$videoId" else null
}
