package dev.diego.orinify.auth

import org.junit.Assert.*
import org.junit.Test

class WebPlaybackUrlTest {
    @Test fun `opens the selected video on the fixed music origin`() {
        assertEquals("https://music.youtube.com/watch?v=jF4KKOsoyDs", WebPlaybackUrl.forVideo("jF4KKOsoyDs"))
    }

    @Test fun `non video identifiers cannot introduce a URL or parameters`() {
        listOf("", "local-song", "https://example.test", "jF4KKOsoyDs&next=evil", "../filename").forEach {
            assertNull(WebPlaybackUrl.forVideo(it))
        }
    }
}
