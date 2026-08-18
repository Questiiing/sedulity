package com.yujingyuqin.app.data

import android.content.Context
import org.json.JSONArray

class TaskStore(context: Context) {
    private val prefs = context.getSharedPreferences("yujingyuqin", Context.MODE_PRIVATE)

    fun load(): List<Task> {
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            TaskJson.fromJsonArray(JSONArray(raw))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(tasks: List<Task>) {
        prefs.edit()
            .putString(KEY_TASKS, TaskJson.toJsonArray(tasks).toString())
            .apply()
    }

    fun upsert(task: Task) {
        val tasks = load().toMutableList()
        val idx = tasks.indexOfFirst { it.packageName == task.packageName }
        if (idx >= 0) tasks[idx] = task else tasks.add(task)
        save(tasks.sortedBy { it.createdAt })
    }

    fun delete(packageName: String) {
        save(load().filterNot { it.packageName == packageName })
    }

    private companion object {
        const val KEY_TASKS = "tasks"
    }
}
