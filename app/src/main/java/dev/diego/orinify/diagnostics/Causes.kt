package dev.diego.orinify.diagnostics

/**
 * The throwable and its causes, nearest first.
 *
 * Failures now arrive wrapped, so recognising a connection or timeout problem means looking past
 * the outermost exception. The walk is bounded and skips a throwable it has already seen, so a
 * self-referencing chain cannot loop.
 */
fun Throwable.causeChain(maxDepth: Int = DEFAULT_CAUSE_DEPTH): List<Throwable> {
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && chain.size < maxDepth) {
        if (chain.any { it === current }) break
        chain += current
        current = current.cause
    }
    return chain
}

const val DEFAULT_CAUSE_DEPTH = 8
