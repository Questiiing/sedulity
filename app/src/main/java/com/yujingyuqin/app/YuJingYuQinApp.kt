package com.yujingyuqin.app

import android.app.Application
import com.yujingyuqin.app.reminder.CheckWorker

class YuJingYuQinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CheckWorker.schedule(this)
        KeepAliveService.start(this)
    }
}
