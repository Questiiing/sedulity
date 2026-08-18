package com.yujingyuqin.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yujingyuqin.app.limit.LimitManager
import java.util.concurrent.TimeUnit

class CheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        ReminderManager.checkAllTasks(appContext)
        ReminderManager.rescheduleAll(appContext)
        LimitManager.checkAllLimits(appContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
