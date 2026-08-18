package com.yujingyuqin.app.data

import android.content.Context
import org.json.JSONArray

class LimitStore(context: Context) {
    private val prefs = context.getSharedPreferences("yujingyuqin", Context.MODE_PRIVATE)

    fun load(): List<AppLimit> {
        val raw = prefs.getString(KEY_LIMITS, null) ?: return emptyList()
        return try {
            LimitJson.fromJsonArray(JSONArray(raw))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(limits: List<AppLimit>) {
        prefs.edit()
            .putString(KEY_LIMITS, LimitJson.toJsonArray(limits).toString())
            .apply()
    }

    fun upsert(limit: AppLimit) {
        val limits = load().toMutableList()
        val idx = limits.indexOfFirst { it.packageName == limit.packageName }
        if (idx >= 0) limits[idx] = limit else limits.add(limit)
        save(limits.sortedBy { it.createdAt })
    }

    fun delete(packageName: String) {
        save(load().filterNot { it.packageName == packageName })
    }

    private companion object {
        const val KEY_LIMITS = "limits"
    }
}
