package com.zionhuang.innertube

import com.zionhuang.innertube.YouTube.PlayerAttemptOutcome
import com.zionhuang.innertube.models.response.PlayerResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayabilityOutcomeTest {
    private fun response(formats: String? = null): PlayerResponse =
        InnerTube.requestJson.decodeFromString(
            PlayerResponse.serializer(),
            """{
                "responseContext": {},
                "playabilityStatus": {"status": "OK"},
                "streamingData": ${formats?.let {
                    """{"expiresInSeconds": "3600", "adaptiveFormats": [$it]}"""
                } ?: "null"}
            }""",
        )

    private fun format(mimeType: String, url: String? = null): String =
        """{"itag": 140, "mimeType": "$mimeType", "bitrate": 128000, "quality": "tiny",
            "url": ${url?.let { "\"$it\"" } ?: "null"}}"""

    @Test
    fun `OK without streaming data must not stop fallback`() {
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(response()))
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(response("")))
    }

    @Test
    fun `cipher only or blank audio URLs must not stop fallback`() {
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(response(format("audio/mp4"))))
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(response(format("audio/mp4", ""))))
    }

    @Test
    fun `video only response must not stop audio fallback`() {
        assertEquals(PlayerAttemptOutcome.NOT_PLAYABLE, YouTube.playabilityOutcome(response(format("video/mp4", "https://example.test/video"))))
    }

    @Test
    fun `direct audio stream makes OK playable`() {
        assertEquals(PlayerAttemptOutcome.OK, YouTube.playabilityOutcome(response(format("audio/mp4", "https://example.test/audio"))))
    }

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
