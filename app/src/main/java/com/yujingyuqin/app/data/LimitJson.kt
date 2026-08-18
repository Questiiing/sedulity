package com.yujingyuqin.app.data

import org.json.JSONArray
import org.json.JSONObject

object LimitJson {
    fun toJson(limit: AppLimit): JSONObject = JSONObject().apply {
        put("packageName", limit.packageName)
        put("appLabel", limit.appLabel)
        put("maxMinutes", limit.maxMinutes)
        put("enabled", limit.enabled)
        put("createdAt", limit.createdAt)
    }

    fun fromJson(json: JSONObject): AppLimit = AppLimit(
        packageName = json.getString("packageName"),
        appLabel = json.optString("appLabel", json.getString("packageName")),
        maxMinutes = json.optInt("maxMinutes", 60),
        enabled = json.optBoolean("enabled", true),
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
    )

    fun toJsonArray(limits: List<AppLimit>): JSONArray = JSONArray().apply {
        limits.forEach { put(toJson(it)) }
    }

    fun fromJsonArray(arr: JSONArray): List<AppLimit> =
        (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
}
