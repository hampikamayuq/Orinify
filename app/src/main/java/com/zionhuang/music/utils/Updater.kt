package com.zionhuang.music.utils

import dev.diego.orinify.update.normalizeReleaseVersionName
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> = runCatching {
        val response = client.get("https://api.github.com/repos/hampikamayuq/Orinify/releases/latest").bodyAsText()
        val json = JSONObject(response)
        // The tag is the authoritative version; the release title is only a fallback because it is
        // free text and may be absent.
        val versionName = normalizeReleaseVersionName(json.optString("tag_name"))
            ?: normalizeReleaseVersionName(json.optString("name"))
            ?: error("Release without a usable version name")
        lastCheckTime = System.currentTimeMillis()
        versionName
    }
}
