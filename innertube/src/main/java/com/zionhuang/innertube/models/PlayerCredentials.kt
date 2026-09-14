package com.zionhuang.innertube.models

/** Explicit credential policy for player requests. */
data class PlayerCredentials(
    val withLogin: Boolean,
    val withVisitorData: Boolean = true,
    /** Send the `SAPISIDHASH` Authorization header alongside the cookie. */
    val signRequest: Boolean = true,
    /** Disambiguate which signed-in account of the cookie the request means. */
    val withAuthUser: Boolean = false,
    /** Present this API key instead of whatever the client declares. */
    val apiKey: String? = null,
    /** What the attempt trail calls this presentation. Empty for the plain one. */
    val label: String = "",
) {
    companion object {
        val ANONYMOUS = PlayerCredentials(withLogin = false, withVisitorData = false)

        fun forClient(client: YouTubeClient, hasSession: Boolean): List<PlayerCredentials> =
            if (hasSession && client.supportsCookieAuth) {
                listOf(PlayerCredentials(withLogin = true))
            } else {
                listOf(ANONYMOUS)
            }
    }
}
