package dev.diego.orinify.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamUrlCacheTest {
    private var now = 1_700_000_000_000L

    private val cache = StreamUrlCache(
        maxEntries = 3,
        expiryMarginSeconds = 60,
        nowEpochMs = { now },
    )

    @Test
    fun `missing key returns null`() {
        assertNull(cache.get("absent"))
    }

    @Test
    fun `url is served while the lifetime has not elapsed`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 21_600)

        now += 10_000_000L

        assertEquals("https://example.invalid/a", cache.get("a"))
    }

    /**
     * Regression test for the duration-versus-instant comparison: a stored lifetime used to be
     * compared against the wall clock, which is always larger, so an expired URL was served
     * forever.
     */
    @Test
    fun `url is dropped once the lifetime has elapsed`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 21_600)

        now += 21_600 * 1000L

        assertNull(cache.get("a"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `url stops being served one margin before the declared expiry`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)

        now += (600 - 61) * 1000L
        assertEquals("https://example.invalid/a", cache.get("a"))

        now += 1_000L
        assertNull(cache.get("a"))
    }

    @Test
    fun `lifetime shorter than the margin is not stored`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 30)

        assertNull(cache.get("a"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `blank url is not stored and replaces nothing`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)
        cache.put("a", "", expiresInSeconds = 600)

        assertNull(cache.get("a"))
    }

    @Test
    fun `newer url replaces the previous one`() {
        cache.put("a", "https://example.invalid/old", expiresInSeconds = 600)
        cache.put("a", "https://example.invalid/new", expiresInSeconds = 600)

        assertEquals("https://example.invalid/new", cache.get("a"))
        assertEquals(1, cache.size())
    }

    @Test
    fun `clear drops every entry`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)
        cache.put("b", "https://example.invalid/b", expiresInSeconds = 600)

        cache.clear()

        assertNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `remove drops a single entry`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)
        cache.put("b", "https://example.invalid/b", expiresInSeconds = 600)

        cache.remove("a")

        assertNull(cache.get("a"))
        assertEquals("https://example.invalid/b", cache.get("b"))
    }

    @Test
    fun `cache is bounded and drops the oldest entry first`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)
        cache.put("b", "https://example.invalid/b", expiresInSeconds = 600)
        cache.put("c", "https://example.invalid/c", expiresInSeconds = 600)
        cache.put("d", "https://example.invalid/d", expiresInSeconds = 600)

        assertEquals(3, cache.size())
        assertNull(cache.get("a"))
        assertEquals("https://example.invalid/d", cache.get("d"))
    }

    @Test
    fun `expired entries are reclaimed when new ones arrive`() {
        cache.put("a", "https://example.invalid/a", expiresInSeconds = 600)

        now += 600 * 1000L
        cache.put("b", "https://example.invalid/b", expiresInSeconds = 600)

        assertEquals(1, cache.size())
        assertEquals("https://example.invalid/b", cache.get("b"))
    }
}
