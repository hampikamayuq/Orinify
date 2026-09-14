package com.zionhuang.innertube.utils

import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.pages.PlaylistPage
import java.security.MessageDigest

suspend fun Result<PlaylistPage>.completed() = runCatching {
    val page = getOrThrow()
    val songs = page.songs.toMutableList()
    var continuation = page.songsContinuation
    while (continuation != null) {
        val continuationPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
        songs += continuationPage.songs
        continuation = continuationPage.continuation
    }
    PlaylistPage(
        playlist = page.playlist,
        songs = songs,
        songsContinuation = null,
        continuation = page.continuation
    )
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

/**
 * True when [cookie] carries a session that can actually be signed.
 *
 * Requests are authenticated with a `SAPISIDHASH` built from the SAPISID cookie, so a cookie
 * without it reaches YouTube as an anonymous request no matter how it looks. Treating "a cookie
 * exists" as "the session is authenticated" would report a Premium-capable session that the server
 * never saw, which contradicts ADR-003.
 */
fun hasAuthenticatedSession(cookie: String?): Boolean {
    if (cookie.isNullOrBlank()) return false
    return runCatching { parseCookieString(cookie) }
        .getOrNull()
        ?.containsKey("SAPISID") == true
}

/**
 * Cookie values routinely contain "=" — base64 padding in `__Secure-3PSIDTS` alone guarantees it —
 * so only the first one separates the name from the value. Splitting on every "=" truncated those
 * values, and an entry carrying no "=" at all threw, which left the caller with a cookie it could
 * not sign while still sending it. A malformed entry is skipped instead.
 */
fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split(";")
        .mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
        }
        .filter { it.first.isNotEmpty() }
        .toMap()

fun String.parseTime(): Int? {
    try {
        val parts = split(":").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}
