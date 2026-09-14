package com.zionhuang.innertube

import com.zionhuang.innertube.models.PlayerCredentials
import com.zionhuang.innertube.models.YouTubeClient
import org.junit.Assert.*
import org.junit.Test

class PlayerCredentialPolicyTest {
    @Test fun `native clients never receive browser credentials or visitor identity`() {
        listOf(YouTubeClient.ANDROID_MUSIC, YouTubeClient.ANDROID_VR, YouTubeClient.IOS).forEach { client ->
            assertFalse(client.supportsCookieAuth)
            val modes = PlayerCredentials.forClient(client, true)
            assertEquals(1, modes.size)
            assertFalse(modes.single().withLogin)
            assertFalse(modes.single().withVisitorData)
        }
    }

    @Test fun `web playback uses one signed session request without credential probes`() {
        val modes = PlayerCredentials.forClient(YouTubeClient.WEB_MUSIC_PLAYER, true)
        assertEquals(1, modes.size)
        assertTrue(modes.single().withLogin)
        assertTrue(modes.single().signRequest)
        assertNull(modes.single().apiKey)
    }

    @Test fun `absent session never claims authentication`() {
        assertFalse(PlayerCredentials.forClient(YouTubeClient.WEB_MUSIC_PLAYER, false).single().withLogin)
    }
}
