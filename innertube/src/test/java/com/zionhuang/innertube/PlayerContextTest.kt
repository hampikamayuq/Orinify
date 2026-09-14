package com.zionhuang.innertube

import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeLocale
import com.zionhuang.innertube.models.body.PlayerBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The player varies `visitorData` per request, so what matters is that an absent one is really
 * absent from the JSON rather than sent as an explicit null. These go through the same serializer
 * the client uses, not a copy of its settings.
 */
class PlayerContextTest {
    private val locale = YouTubeLocale(gl = "BR", hl = "pt")

    private fun body(visitorData: String?): String =
        InnerTube.requestJson.encodeToString(
            PlayerBody.serializer(),
            PlayerBody(
                context = YouTubeClient.ANDROID_VR.toContext(locale, visitorData),
                videoId = "abc",
                playlistId = null,
            ),
        )

    @Test
    fun `a visitor is carried when there is one`() {
        assertTrue(body("CgtEUU1ZY2hCdEFtbw").contains("\"visitorData\":\"CgtEUU1ZY2hCdEFtbw\""))
    }

    @Test
    fun `no visitor means the field is absent, not null`() {
        val json = body(null)
        assertFalse(json.contains("visitorData"))
        assertFalse(json.contains("null"))
    }

    @Test
    fun `the client is still fully described without a visitor`() {
        val json = body(null)
        assertTrue(json.contains("\"clientName\":\"ANDROID_VR\""))
        assertTrue(json.contains("\"clientVersion\":\"1.60.19\""))
        assertTrue(json.contains("\"gl\":\"BR\""))
    }
}
