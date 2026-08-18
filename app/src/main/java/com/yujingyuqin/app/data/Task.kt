package com.yujingyuqin.app.data

data class Task(
    val packageName: String,
    var appLabel: String,
    var targetMinutes: Int = 30,
    var reminderHour: Int = 21,
    var reminderMinute: Int = 0,
    var repeatUntilDone: Boolean = false,
    var enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
