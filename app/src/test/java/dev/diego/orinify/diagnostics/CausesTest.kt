package dev.diego.orinify.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class CausesTest {
    @Test
    fun `a bare throwable is its own chain`() {
        val error = IllegalStateException("boom")

        assertEquals(listOf(error), error.causeChain())
    }

    @Test
    fun `a wrapped cause is still found`() {
        val root = ConnectException("no route")
        val wrapped = IllegalStateException("outer", root)

        assertTrue(wrapped.causeChain().any { it is ConnectException })
    }

    @Test
    fun `the chain is ordered nearest first`() {
        val root = ConnectException("no route")
        val middle = IllegalStateException("middle", root)
        val outer = RuntimeException("outer", middle)

        assertEquals(listOf(outer, middle, root), outer.causeChain())
    }

    @Test
    fun `a self referencing chain terminates`() {
        val looping = object : RuntimeException("loop") {
            override val cause: Throwable get() = this
        }

        assertEquals(1, looping.causeChain().size)
    }

    @Test
    fun `the walk is bounded`() {
        var deepest: Throwable = RuntimeException("root")
        repeat(20) { deepest = RuntimeException("wrap", deepest) }

        assertEquals(DEFAULT_CAUSE_DEPTH, deepest.causeChain().size)
    }
}
