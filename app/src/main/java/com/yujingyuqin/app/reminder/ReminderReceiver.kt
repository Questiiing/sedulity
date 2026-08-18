package com.yujingyuqin.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yujingyuqin.app.data.TaskStore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(ReminderManager.EXTRA_PACKAGE) ?: return
        val task = TaskStore(context).load().firstOrNull { it.packageName == pkg } ?: return
        ReminderManager.checkTask(context, task)
    }
}
