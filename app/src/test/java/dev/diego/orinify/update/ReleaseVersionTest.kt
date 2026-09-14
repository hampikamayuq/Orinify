package dev.diego.orinify.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseVersionTest {
    /**
     * Regression test for the perpetual update banner: the release is tagged `v0.5.10-q1` while
     * the build reports `0.5.10-q1`, so the raw values never matched.
     */
    @Test
    fun `tag prefix is stripped so it matches the build version name`() {
        assertEquals("0.5.10-q1", normalizeReleaseVersionName("v0.5.10-q1"))
    }

    @Test
    fun `version without a prefix is unchanged`() {
        assertEquals("0.5.10-q1", normalizeReleaseVersionName("0.5.10-q1"))
    }

    @Test
    fun `uppercase prefix is stripped`() {
        assertEquals("1.0.0", normalizeReleaseVersionName("V1.0.0"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("0.5.10-q1", normalizeReleaseVersionName("  v0.5.10-q1\n"))
    }

    @Test
    fun `leading letter that is not a tag prefix is preserved`() {
        assertEquals("version-2", normalizeReleaseVersionName("version-2"))
    }

    @Test
    fun `missing or empty values return null`() {
        assertNull(normalizeReleaseVersionName(null))
        assertNull(normalizeReleaseVersionName(""))
        assertNull(normalizeReleaseVersionName("   "))
    }

    @Test
    fun `literal null string returns null`() {
        assertNull(normalizeReleaseVersionName("null"))
    }
}
