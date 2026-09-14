import com.zionhuang.kugou.KuGou
import com.zionhuang.kugou.KuGou.generateKeyword
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test

class Test {
    /**
     * This test calls KuGou for real. Live services are unstable, rate limited and unavailable to
     * pull requests, so the gate skips them unless they are asked for explicitly.
     */
    @Before
    fun requireNetworkTests() {
        Assume.assumeTrue(
            "Set ORINIFY_NETWORK_TESTS=1 to run tests that reach live services.",
            System.getenv("ORINIFY_NETWORK_TESTS") == "1"
        )
    }

    @Test
    fun test() = runBlocking {
        val candidates = KuGou.getLyricsCandidate(generateKeyword("千年以後 (After A Thousand Years)", "陳零九"), 285)
        assertTrue(candidates != null)
        assertTrue(KuGou.getLyrics("楊丞琳", "點水", 259).isSuccess)
    }
}
