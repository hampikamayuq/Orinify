package com.zionhuang.innertube

import com.zionhuang.innertube.YouTube.PlayerAttemptOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayabilityOutcomeTest {
    @Test
    fun `a playable response is the only OK outcome`() {
        assertEquals(PlayerAttemptOutcome.OK, YouTube.playabilityOutcome("OK"))
    }

    @Test
    fun `a request for a signed-in session is told apart from a refusal`() {
        assertEquals(PlayerAttemptOutcome.LOGIN_REQUIRED, YouTube.playabilityOutcome("LOGIN_REQUIRED"))
    }

    @Test
    fun `every other status is a refusal by this client, not a transport problem`() {
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome("UNPLAYABLE"))
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome("AGE_VERIFICATION_REQUIRED"))
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome("ERROR"))
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(""))
    }

    @Test
    fun `status casing does not change the outcome`() {
        assertEquals(PlayerAttemptOutcome.OK, YouTube.playabilityOutcome("ok"))
        assertEquals(PlayerAttemptOutcome.LOGIN_REQUIRED, YouTube.playabilityOutcome("Login_Required"))
    }

    @Test
    fun `the third party fallback is off until it is asked for`() {
        assertEquals(false, YouTube.allowPipedFallback)
    }
}
