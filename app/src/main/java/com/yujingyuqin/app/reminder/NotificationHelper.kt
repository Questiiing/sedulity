package com.yujingyuqin.app.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yujingyuqin.app.R
import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.ui.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "task_reminder"
    private const val CHANNEL_NAME = "任务提醒"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每日保底任务到点提醒"
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, task: Task, usedMinutes: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val remaining = (task.targetMinutes - usedMinutes).coerceAtLeast(0)
        val id = task.packageName.hashCode() and 0x7FFFFFFF
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("《${task.appLabel}》今日任务")
            .setContentText(
                "已用 $usedMinutes / ${task.targetMinutes} 分钟，还差 $remaining 分钟，现在去打开吧！"
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "已用 $usedMinutes / ${task.targetMinutes} 分钟，还差 $remaining 分钟，现在去打开吧！"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(context, id))

        val openIntent = context.packageManager.getLaunchIntentForPackage(task.packageName)
        if (openIntent != null) {
            val openPi = PendingIntent.getActivity(
                context,
                id + 1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_stat_reminder, "打开应用", openPi)
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showCompleted(context: Context, task: Task, usedMinutes: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val id = task.packageName.hashCode() and 0x7FFFFFFF
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("《${task.appLabel}》已达标 ✓")
            .setContentText("今天已使用 $usedMinutes 分钟，完成目标 ${task.targetMinutes} 分钟，太棒了！")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(context, id))
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showLimitExceeded(context: Context, limit: AppLimit, usedMinutes: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val id = "limit_${limit.packageName}".hashCode() and 0x7FFFFFFF
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("《${limit.appLabel}》已超限")
            .setContentText(
                "今日已用 $usedMinutes 分钟，超过上限 ${limit.maxMinutes} 分钟，该休息了"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent(context, id))
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun contentPendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
