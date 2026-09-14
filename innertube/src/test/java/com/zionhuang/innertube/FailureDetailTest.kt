package com.zionhuang.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FailureDetailTest {
    private fun detailFor(status: HttpStatusCode, body: String): String = runBlocking {
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            engine {
                addHandler {
                    respond(
                        content = body,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        }
        val thrown = runCatching { client.get("https://example.invalid/player") }.exceptionOrNull()!!
        client.close()
        YouTube.failureDetail(thrown)
    }

    @Test
    fun `reports the server status token`() {
        assertEquals(
            "HTTP 400 FAILED_PRECONDITION",
            detailFor(HttpStatusCode.BadRequest, """{"error":{"code":400,"status":"FAILED_PRECONDITION"}}"""),
        )
    }

    @Test
    fun `falls back to the reason when there is no status token`() {
        assertEquals(
            "HTTP 403 Forbidden by policy",
            detailFor(HttpStatusCode.Forbidden, """{"error":{"reason":"Forbidden by policy"}}"""),
        )
    }

    @Test
    fun `says so when the server sent no detail`() {
        assertEquals("HTTP 400 no detail", detailFor(HttpStatusCode.BadRequest, ""))
    }

    @Test
    fun `carries nothing but the token out of the body`() {
        val detail = detailFor(
            HttpStatusCode.BadRequest,
            """{"error":{"status":"FAILED_PRECONDITION","message":"key=SECRET123 is invalid"}}""",
        )
        assertEquals("HTTP 400 FAILED_PRECONDITION", detail)
    }

    @Test
    fun `names the failure type when there was no response`() = runBlocking {
        assertEquals("IllegalStateException", YouTube.failureDetail(IllegalStateException("boom")))
    }
}
