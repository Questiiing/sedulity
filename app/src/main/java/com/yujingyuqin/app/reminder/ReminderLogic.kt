package com.yujingyuqin.app.reminder

import com.yujingyuqin.app.data.Task
import java.util.Calendar

object ReminderLogic {
    const val REPEAT_INTERVAL_MS = 30L * 60 * 1000
    const val REPEAT_INTERVAL_MINUTES = 30

    /** 今天的提醒时间；若今天已过，则返回明天的 */
    fun nextReminderTime(now: Calendar, task: Task): Calendar {
        val c = now.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, task.reminderHour)
        c.set(Calendar.MINUTE, task.reminderMinute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= now.timeInMillis) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c
    }

    /** 今天的提醒时间点是否已经到达 */
    fun reminderTimeReached(now: Calendar, task: Task): Boolean {
        val c = now.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, task.reminderHour)
        c.set(Calendar.MINUTE, task.reminderMinute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return now.timeInMillis >= c.timeInMillis
    }

    /** 下一次重复提醒时间；若超过当天结束则返回 null（当天不再重复） */
    fun nextRepeatTime(
        nowMs: Long,
        endOfDayMs: Long,
        intervalMs: Long = REPEAT_INTERVAL_MS
    ): Long? {
        val next = nowMs + intervalMs
        return if (next <= endOfDayMs) next else null
    }
}
