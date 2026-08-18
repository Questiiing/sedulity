package com.yujingyuqin.app.limit

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.yujingyuqin.app.KeepAliveService
import com.yujingyuqin.app.ui.BlockActivity

/**
 * 实时前台检测：纯事件驱动，空闲时零开销。
 *
 * 关键防御（针对 HyperOS 上"回到业精于勤时才弹窗"的误判）：
 * 1. 事件延迟过滤：事件自带生成时间戳，若到达时已超过 MAX_EVENT_AGE_MS，
 *    说明是延迟投递的旧事件（进程被冻结期间排队，直到我们 App 被打开才送达），
 *    只记录、不驱动拦截，防止把旧前台当成当前前台。
 * 2. 包名缺失时只做一次 300ms 延迟重查（不循环）。
 */
class LimitWatchService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRecheck: Runnable? = null
    private var recheckAttempts = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        KeepAliveService.start(this)
        LimitDiagnostics.log(this, "service", "无障碍服务已连接")
        checkActiveWindow()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        lastEventTimeMs = System.currentTimeMillis()
        eventCount++
        val lag = SystemClock.uptimeMillis() - event.eventTime
        lastEventLagMs = lag
        val typeName =
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) "STATE" else "WINDOWS"
        val pkg = event.packageName?.toString()
        LimitDiagnostics.log(
            this, "event",
            "$typeName pkg=$pkg lag=${lag}ms root=${activeWindowPackage()}"
        )

        // 事件时间戳为 0 时（部分系统会这样）不做过期判断，避免把所有事件都当成旧事件丢弃
        if (event.eventTime <= 0L) {
            LimitDiagnostics.log(this, "event", "事件时间戳为 0，按正常事件处理")
            if (pkg != null) onPackage(pkg) else scheduleRecheck()
        } else if (lag > MAX_EVENT_AGE_MS) {
            // 延迟投递的旧事件：只有当前活动窗口仍确认是该应用（用户确实还在里面）才处理，
            // 否则忽略，防止把旧前台当成当前前台误弹
            val root = activeWindowPackage()
            if (pkg != null && root == pkg) {
                LimitDiagnostics.log(this, "event", "延迟事件(${lag}ms)但活动窗口确认是 $pkg，按正常处理")
                onPackage(pkg)
            } else {
                LimitDiagnostics.log(this, "event", "延迟事件(${lag}ms)且活动窗口为 $root，忽略")
            }
        } else {
            if (pkg != null) onPackage(pkg) else scheduleRecheck()
        }
    }

    override fun onInterrupt() {
        LimitDiagnostics.log(this, "service", "无障碍服务被系统中断")
    }

    override fun onDestroy() {
        LimitDiagnostics.log(this, "service", "无障碍服务销毁")
        cancelRecheck()
        BlockActivity.dismissCurrent()
        instance = null
        super.onDestroy()
    }

    private fun onPackage(packageName: String) {
        cancelRecheck()
        recheckAttempts = 0
        lastForegroundPackage = packageName
        LimitManager.onForegroundChanged(this, packageName)
    }

    private fun checkActiveWindow() {
        val pkg = activeWindowPackage()
        if (pkg != null) {
            LimitDiagnostics.log(this, "recheck", "当前活动窗口=$pkg")
            onPackage(pkg)
        } else {
            LimitDiagnostics.log(this, "recheck", "当前活动窗口为空（第 $recheckAttempts 次补查）")
            // 慢设备上窗口可能还没就绪：最多再补查两次（仍是有界重试，不是轮询）
            scheduleRecheck()
        }
    }

    private fun scheduleRecheck() {
        if (recheckAttempts >= MAX_RECHECK_ATTEMPTS) return
        if (pendingRecheck != null) return
        recheckAttempts++
        pendingRecheck = Runnable {
            pendingRecheck = null
            checkActiveWindow()
        }
        handler.postDelayed(pendingRecheck!!, RECHECK_DELAY_MS)
    }

    private fun cancelRecheck() {
        pendingRecheck?.let { handler.removeCallbacks(it) }
        pendingRecheck = null
    }

    companion object {
        @Volatile
        private var instance: LimitWatchService? = null

        /** 最近一次收到窗口事件的时间（0 表示从未收到） */
        @Volatile
        var lastEventTimeMs: Long = 0L
            private set

        /** 最近一次事件的投递延迟（毫秒），诊断用 */
        @Volatile
        var lastEventLagMs: Long = 0L
            private set

        /** 本次服务生命周期内收到的事件总数 */
        @Volatile
        var eventCount: Long = 0L
            private set

        /** 最近一次识别到的前台应用 */
        @Volatile
        var lastForegroundPackage: String? = null
            private set

        /** 读取系统当前活动窗口的包名（弹窗前的二次核对用） */
        fun activeWindowPackage(): String? =
            instance?.rootInActiveWindow?.packageName?.toString()

        private const val RECHECK_DELAY_MS = 300L
        private const val MAX_EVENT_AGE_MS = 2_000L
        private const val MAX_RECHECK_ATTEMPTS = 2
    }
}
