package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val api_key: String,
    val userAgent: String,
    val osVersion: String? = null,
    val referer: String? = null,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )

    companion object {
        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"

        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
        private const val USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36"
        private const val USER_AGENT_ANDROID_MUSIC = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14) gzip"
        private const val USER_AGENT_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12; GB) gzip"
        private const val USER_AGENT_IOS = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"

        /**
         * The music app. Version 5.01 was retired server-side: the player endpoint answers it with
         * HTTP 400 FAILED_PRECONDITION, which is why every track failed with "unknown error".
         * Verified against the live endpoint: this version answers 200.
         */
        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "7.27.52",
            // No key. This version is accepted without one, and Google APIs reject a request that
            // presents an API key together with real credentials.
            api_key = "",
            userAgent = USER_AGENT_ANDROID_MUSIC
        )

        /**
         * Plays without a signed-in session and returns direct stream URLs, with no signature to
         * decipher. Verified against the live endpoint: status OK anonymously, audio formats with
         * plain `url` fields and no `signatureCipher`. This is what the retired IOS client used to
         * provide.
         */
        /**
         * Measured as the only client that answers `OK` without a session, returning direct URLs
         * and no `signatureCipher`, which is why it anchors the player chain.
         *
         * It is asked with the session first all the same. Anonymously the server answers real
         * accounts with "sign in to confirm you're not a bot" and refuses every track, and a bot
         * check is not something a client version can satisfy. The anonymous attempt still follows
         * when the session is refused.
         */
        val ANDROID_VR = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.60.19",
            api_key = "",
            userAgent = USER_AGENT_ANDROID_VR,
            osVersion = "12L",
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "17.13.3",
            api_key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            userAgent = USER_AGENT_ANDROID,
        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.2021111",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3",
            userAgent = USER_AGENT_WEB
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20220606.03.00",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            api_key = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8",
            userAgent = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"
        )

        /**
         * Retired server-side: the player endpoint answers HTTP 400 FAILED_PRECONDITION. Kept only
         * so upstream merges stay small; [ANDROID_VR] replaced it in the player chain.
         */
        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.29.1",
            api_key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            userAgent = USER_AGENT_IOS,
            osVersion = "17.5.1.21F90",
        )
    }
}
