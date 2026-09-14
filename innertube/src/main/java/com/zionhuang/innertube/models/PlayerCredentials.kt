package com.zionhuang.innertube.models

/**
 * How one player request presents itself to the server.
 *
 * The server rejects the signed-in device's request with `INVALID_ARGUMENT` while answering the
 * same request without credentials, on every client and whether or not a `visitorData` travels
 * with it. What is malformed is therefore the credential presentation itself, and the pieces of it
 * vary independently, so they are varied independently here: the first presentation that the
 * server accepts ends the search, and the attempt trail names it.
 */
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
        private const val PUBLIC_WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

        val ANONYMOUS = PlayerCredentials(withLogin = false)

        /**
         * Ordered from the plainest presentation to the most qualified, then down to no session at
         * all. Each entry changes exactly one thing against the first, so whichever one answers
         * identifies what the server was missing.
         */
        val ORDERED_WITH_SESSION = listOf(
            PlayerCredentials(withLogin = true),
            PlayerCredentials(withLogin = true, apiKey = PUBLIC_WEB_KEY, label = " key"),
            PlayerCredentials(withLogin = true, withAuthUser = true, label = " authuser"),
            PlayerCredentials(withLogin = true, signRequest = false, label = " unsigned"),
            PlayerCredentials(withLogin = false, label = " anon"),
        )
    }
}
