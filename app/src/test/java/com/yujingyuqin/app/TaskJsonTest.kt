package com.yujingyuqin.app

import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.data.TaskJson
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskJsonTest {

    @Test
    fun `task roundtrip keeps all fields`() {
        val task = Task(
            packageName = "com.read",
            appLabel = "微信读书",
            targetMinutes = 45,
            reminderHour = 22,
            reminderMinute = 30,
            repeatUntilDone = true,
            enabled = false,
            createdAt = 123456789L
        )
        val restored = TaskJson.fromJson(TaskJson.toJson(task))
        assertEquals(task, restored)
    }

    @Test
    fun `fromJson fills defaults for missing fields`() {
        val json = JSONObject().put("packageName", "com.kindle")
        val task = TaskJson.fromJson(json)
        assertEquals("com.kindle", task.packageName)
        assertEquals(30, task.targetMinutes)
        assertEquals(21, task.reminderHour)
        assertEquals(0, task.reminderMinute)
        assertEquals(false, task.repeatUntilDone)
        assertEquals(true, task.enabled)
    }

    @Test
    fun `array roundtrip preserves order`() {
        val tasks = listOf(
            Task(packageName = "a", appLabel = "A"),
            Task(packageName = "b", appLabel = "B"),
            Task(packageName = "c", appLabel = "C")
        )
        val restored = TaskJson.fromJsonArray(JSONArray(TaskJson.toJsonArray(tasks).toString()))
        assertEquals(tasks, restored)
    }
}
