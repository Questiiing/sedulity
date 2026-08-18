package com.yujingyuqin.app.data

import android.content.Context
import org.json.JSONObject

data class TaskReminderState(
    val date: String,
    val notified: Boolean,
    val completed: Boolean
)

class ReminderStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)

    fun get(packageName: String): TaskReminderState? {
        val raw = prefs.getString(packageName, null) ?: return null
        return try {
            val o = JSONObject(raw)
            TaskReminderState(
                date = o.getString("date"),
                notified = o.optBoolean("notified"),
                completed = o.optBoolean("completed")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(packageName: String, state: TaskReminderState) {
        prefs.edit().putString(packageName, JSONObject().apply {
            put("date", state.date)
            put("notified", state.notified)
            put("completed", state.completed)
        }.toString()).apply()
    }

    /** 清除某任务的今日提醒状态（修改任务后调用，保证按新时间重新提醒） */
    fun reset(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }
}
