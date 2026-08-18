package com.yujingyuqin.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yujingyuqin.app.data.ReminderStateStore
import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.data.TaskReminderState
import com.yujingyuqin.app.data.TaskStore
import com.yujingyuqin.app.usage.UsageChecker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderManager {
    const val ACTION_CHECK = "com.yujingyuqin.app.action.CHECK"
    const val ACTION_REPEAT = "com.yujingyuqin.app.action.REPEAT"
    const val EXTRA_PACKAGE = "extra_package"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    fun checkAllTasks(context: Context) {
        val store = TaskStore(context)
        for (task in store.load()) {
            checkTask(context, task)
        }
    }

    fun checkTask(context: Context, task: Task) {
        if (!task.enabled) {
            cancelAlarms(context, task)
            return
        }
        if (!UsageChecker.needsUsageAccess(context)) return

        val now = Calendar.getInstance()
        val used = UsageChecker.minutesUsedToday(context, task.packageName)
        val stateStore = ReminderStateStore(context)
        val today = dateFormat.format(now.time)
        val state = stateStore.get(task.packageName)
            ?.takeIf { it.date == today }
            ?: TaskReminderState(today, notified = false, completed = false)

        if (used >= task.targetMinutes) {
            if (!state.completed) {
                stateStore.save(task.packageName, state.copy(completed = true, notified = true))
                NotificationHelper.showCompleted(context, task, used)
            }
            cancelRepeatAlarm(context, task)
            return
        }

        if (!ReminderLogic.reminderTimeReached(now, task)) return

        if (!state.notified) {
            NotificationHelper.showReminder(context, task, used)
            stateStore.save(task.packageName, state.copy(notified = true))
        }

        if (task.repeatUntilDone) {
            val endOfDay = UsageChecker.endOfToday().timeInMillis
            val next = ReminderLogic.nextRepeatTime(System.currentTimeMillis(), endOfDay)
            if (next != null) {
                scheduleAlarm(context, task, next, repeat = true)
            } else {
                cancelRepeatAlarm(context, task)
            }
        }
    }

    /** 重新为所有启用的任务安排今日/明日提醒闹钟 */
    fun rescheduleAll(context: Context) {
        val store = TaskStore(context)
        for (task in store.load()) {
            if (!task.enabled) {
                cancelAlarms(context, task)
                continue
            }
            // 先清掉旧闹钟（含旧重复闹钟），再按最新设置重排
            cancelAlarms(context, task)
            val next = ReminderLogic.nextReminderTime(Calendar.getInstance(), task)
            scheduleAlarm(context, task, next.timeInMillis, repeat = false)
        }
    }

    fun cancelAlarms(context: Context, task: Task) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context, task, repeat = false))
        am.cancel(alarmPendingIntent(context, task, repeat = true))
    }

    fun cancelRepeatAlarm(context: Context, task: Task) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context, task, repeat = true))
    }

    private fun scheduleAlarm(context: Context, task: Task, triggerAtMs: Long, repeat: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context, task, repeat)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // 未授予精确闹钟权限：降级为不精确窗口（延迟最多约 15 分钟，由 WorkManager 兜底）
            try {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMs, 15 * 60_000L, pi)
            } catch (_: Exception) {
            }
            return
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } catch (_: SecurityException) {
            try {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMs, 15 * 60_000L, pi)
            } catch (_: Exception) {
            }
        }
    }

    private fun alarmPendingIntent(context: Context, task: Task, repeat: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = if (repeat) ACTION_REPEAT else ACTION_CHECK
            putExtra(EXTRA_PACKAGE, task.packageName)
        }
        val requestCode = task.packageName.hashCode() * 10 + if (repeat) 1 else 0
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
