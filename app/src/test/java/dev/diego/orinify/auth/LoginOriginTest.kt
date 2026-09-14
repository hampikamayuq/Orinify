package dev.diego.orinify.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginOriginTest {
    @Test fun `session extraction only accepts exact secure music origin`() {
        assertTrue(LoginOrigin.isMusic("https://music.youtube.com/"))
        assertTrue(LoginOrigin.isMusic("https://music.youtube.com:443/browse"))
        listOf(null, "", "http://music.youtube.com", "https://music.youtube.com.evil.test",
            "https://music.youtube.com@evil.test", "https://accounts.google.com/",
            "https://music.youtube.com:8443/", "javascript:alert(1)").forEach {
            assertFalse(LoginOrigin.isMusic(it))
        }
    }
}
