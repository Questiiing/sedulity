package com.yujingyuqin.app.data

import org.json.JSONArray
import org.json.JSONObject

object TaskJson {
    fun toJson(task: Task): JSONObject = JSONObject().apply {
        put("packageName", task.packageName)
        put("appLabel", task.appLabel)
        put("targetMinutes", task.targetMinutes)
        put("reminderHour", task.reminderHour)
        put("reminderMinute", task.reminderMinute)
        put("repeatUntilDone", task.repeatUntilDone)
        put("enabled", task.enabled)
        put("createdAt", task.createdAt)
    }

    fun fromJson(json: JSONObject): Task = Task(
        packageName = json.getString("packageName"),
        appLabel = json.optString("appLabel", json.getString("packageName")),
        targetMinutes = json.optInt("targetMinutes", 30),
        reminderHour = json.optInt("reminderHour", 21),
        reminderMinute = json.optInt("reminderMinute", 0),
        repeatUntilDone = json.optBoolean("repeatUntilDone", false),
        enabled = json.optBoolean("enabled", true),
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
    )

    fun toJsonArray(tasks: List<Task>): JSONArray = JSONArray().apply {
        tasks.forEach { put(toJson(it)) }
    }

    fun fromJsonArray(arr: JSONArray): List<Task> =
        (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
}
