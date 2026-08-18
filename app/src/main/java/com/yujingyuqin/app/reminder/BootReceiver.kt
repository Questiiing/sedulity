package com.yujingyuqin.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yujingyuqin.app.KeepAliveService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    KeepAliveService.start(context)
                }
                ReminderManager.rescheduleAll(context)
                ReminderManager.checkAllTasks(context)
            }
        }
    }
}
