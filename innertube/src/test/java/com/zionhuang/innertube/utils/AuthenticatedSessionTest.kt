package com.zionhuang.innertube.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedSessionTest {
    @Test
    fun `no cookie is not an authenticated session`() {
        assertFalse(hasAuthenticatedSession(null))
    }

    @Test
    fun `blank cookie is not an authenticated session`() {
        assertFalse(hasAuthenticatedSession(""))
        assertFalse(hasAuthenticatedSession("   "))
    }

    @Test
    fun `cookie without SAPISID cannot sign requests`() {
        assertFalse(hasAuthenticatedSession("VISITOR_INFO1_LIVE=abc; YSC=def"))
    }

    @Test
    fun `cookie with SAPISID is an authenticated session`() {
        assertTrue(hasAuthenticatedSession("VISITOR_INFO1_LIVE=abc; SAPISID=secret; YSC=def"))
    }

    @Test
    fun `malformed cookie is reported as anonymous instead of throwing`() {
        assertFalse(hasAuthenticatedSession("not-a-cookie"))
    }
}
