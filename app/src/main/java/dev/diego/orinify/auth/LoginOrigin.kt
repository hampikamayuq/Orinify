package dev.diego.orinify.auth

import java.net.URI

object LoginOrigin {
    fun isMusic(url: String?): Boolean = runCatching {
        val uri = URI(url ?: return false)
        uri.scheme == "https" && uri.host == "music.youtube.com" &&
            uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    }.getOrDefault(false)
}
