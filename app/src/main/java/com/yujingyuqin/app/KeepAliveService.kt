package com.yujingyuqin.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.yujingyuqin.app.limit.LimitDiagnostics

/**
 * 轻量保活服务：只让进程保持存活（无轮询、无任何周期任务，几乎不耗电）。
 * 解决 HyperOS 冻结后台进程、导致打开别的应用时无障碍事件要等回到
 * 业精于勤才补送的问题——进程活着，事件就能实时到达。
 */
class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            LimitDiagnostics.log(this, "keepalive", "保活服务已启动")
        } catch (e: Exception) {
            LimitDiagnostics.log(this, "keepalive", "保活服务启动失败: ${e.javaClass.simpleName}: ${e.message}")
        }
        startHeartbeat()
        return START_STICKY
    }

    override fun onDestroy() {
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
        heartbeatRunnable = null
        LimitDiagnostics.log(this, "keepalive", "保活服务被系统停止")
        super.onDestroy()
    }

    /** 心跳仅用于日志证明服务存活（无轮询检测、几乎零耗电）；被冻结时不运行，正好暴露问题 */
    private fun startHeartbeat() {
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
        heartbeatRunnable = object : Runnable {
            override fun run() {
                LimitDiagnostics.log(this@KeepAliveService, "keepalive", "心跳：保活服务仍在运行")
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }
        handler.postDelayed(heartbeatRunnable!!, HEARTBEAT_INTERVAL_MS)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "保持实时拦截稳定运行（无轮询，几乎不耗电）" }
            )
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("业精于勤·实时拦截运行中")
            .setContentText("保持后台运行以实时检测超限应用（无轮询，几乎不耗电）")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(intent)
                }
            } catch (e: Exception) {
                LimitDiagnostics.log(context, "keepalive", "保活服务启动失败: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        private const val CHANNEL_ID = "keep_alive"
        private const val CHANNEL_NAME = "实时拦截服务"
        private const val NOTIFICATION_ID = 101
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60_000L
    }
}
