package com.zionhuang.innertube.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieParsingTest {
    @Test
    fun `a value keeps everything after the first separator`() {
        val parsed = parseCookieString("__Secure-3PSIDTS=sidts-CjIB3e/g==; SAPISID=abc123")
        assertEquals("sidts-CjIB3e/g==", parsed["__Secure-3PSIDTS"])
        assertEquals("abc123", parsed["SAPISID"])
    }

    @Test
    fun `a malformed entry is skipped instead of throwing`() {
        val parsed = parseCookieString("broken; SAPISID=abc123; =orphan")
        assertEquals(mapOf("SAPISID" to "abc123"), parsed)
    }

    @Test
    fun `entries are read whether or not the separator carries a space`() {
        assertEquals(
            mapOf("a" to "1", "b" to "2"),
            parseCookieString("a=1;b=2"),
        )
    }

    @Test
    fun `a session is authenticated only when it can be signed`() {
        assertTrue(hasAuthenticatedSession("__Secure-3PSID=x==; SAPISID=abc123"))
        assertFalse(hasAuthenticatedSession("__Secure-3PSID=x=="))
        assertFalse(hasAuthenticatedSession("broken"))
        assertFalse(hasAuthenticatedSession(null))
    }
}
