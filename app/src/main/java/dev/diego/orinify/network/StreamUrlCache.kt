package dev.diego.orinify.network

/**
 * Remembers signed stream URLs until shortly before the expiry declared by the player response.
 *
 * The player endpoint reports a lifetime (`expiresInSeconds`), not an instant. Storing that
 * duration and comparing it against the wall clock never expires anything, so a URL kept this way
 * is replayed long after the server stopped honouring it. This cache converts the lifetime into an
 * absolute instant once, subtracts a safety margin, and serves an entry only while that instant is
 * still in the future.
 *
 * The clock is injectable so expiry is testable without waiting. No Android, Compose or database
 * dependency, per the dependency rules in `docs/ARCHITECTURE-Q.md`.
 *
 * URLs are transport data: they are held in memory only, never persisted and never logged.
 */
class StreamUrlCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val expiryMarginSeconds: Long = DEFAULT_EXPIRY_MARGIN_SECONDS,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(
        val url: String,
        val expiresAtEpochMs: Long,
    )

    private val cache = LinkedHashMap<String, Entry>()

    /**
     * The URL stored for [key], or null when nothing was stored or the entry is no longer usable.
     */
    @Synchronized
    fun get(key: String): String? {
        val entry = cache[key] ?: return null
        if (nowEpochMs() >= entry.expiresAtEpochMs) {
            cache.remove(key)
            return null
        }
        return entry.url
    }

    /**
     * Stores [url] for [key] until [expiresInSeconds] minus the safety margin has elapsed.
     *
     * A blank URL, or a lifetime already shorter than the margin, stores nothing and drops any
     * previous entry: it is better to resolve again than to hand out a URL about to be refused.
     */
    @Synchronized
    fun put(key: String, url: String, expiresInSeconds: Int) {
        val usableSeconds = expiresInSeconds - expiryMarginSeconds
        cache.remove(key)
        if (url.isBlank() || usableSeconds <= 0) return
        cache[key] = Entry(
            url = url,
            expiresAtEpochMs = nowEpochMs() + usableSeconds * 1000L,
        )
        evict()
    }

    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
    }

    /**
     * Drops every entry. Called when the session changes, so URLs resolved for one account are
     * never reused for another.
     */
    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size

    private fun evict() {
        val now = nowEpochMs()
        cache.entries.removeAll { it.value.expiresAtEpochMs <= now }
        while (cache.size > maxEntries) {
            val eldest = cache.keys.firstOrNull() ?: break
            cache.remove(eldest)
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 64

        /**
         * Stop serving a URL a minute before the declared expiry, so a request that starts just in
         * time is not refused halfway through.
         */
        const val DEFAULT_EXPIRY_MARGIN_SECONDS = 60L
    }
}
