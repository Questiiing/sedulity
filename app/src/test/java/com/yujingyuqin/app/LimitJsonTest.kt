package com.yujingyuqin.app

import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.data.LimitJson
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitJsonTest {

    @Test
    fun `limit roundtrip keeps all fields`() {
        val limit = AppLimit(
            packageName = "com.tiktok",
            appLabel = "抖音",
            maxMinutes = 45,
            enabled = false,
            createdAt = 987654321L
        )
        val restored = LimitJson.fromJson(LimitJson.toJson(limit))
        assertEquals(limit, restored)
    }

    @Test
    fun `fromJson fills defaults for missing fields`() {
        val json = JSONObject().put("packageName", "com.kuaishou")
        val limit = LimitJson.fromJson(json)
        assertEquals("com.kuaishou", limit.packageName)
        assertEquals(60, limit.maxMinutes)
        assertEquals(true, limit.enabled)
    }

    @Test
    fun `array roundtrip preserves order`() {
        val limits = listOf(
            AppLimit(packageName = "a", appLabel = "A"),
            AppLimit(packageName = "b", appLabel = "B")
        )
        val restored =
            LimitJson.fromJsonArray(JSONArray(LimitJson.toJsonArray(limits).toString()))
        assertEquals(limits, restored)
    }
}
