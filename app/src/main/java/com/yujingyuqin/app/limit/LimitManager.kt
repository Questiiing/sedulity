package com.yujingyuqin.app.limit

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.data.LimitStore
import com.yujingyuqin.app.reminder.NotificationHelper
import com.yujingyuqin.app.ui.BlockActivity
import com.yujingyuqin.app.usage.UsageChecker
import com.yujingyuqin.app.util.AppForeground

object LimitManager {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingConfirm: PendingConfirm? = null
    private var pendingRunnable: Runnable? = null
    private var gracePackage: String? = null
    private var graceUntilMs: Long = 0
    private val lastNotifiedAt = mutableMapOf<String, Long>()

    private class PendingConfirm(
        val context: Context,
        val limit: AppLimit,
        val usedMinutes: Int,
        val startedAt: Long
    )

    /** 检测到前台应用变化时调用（无障碍事件驱动，无轮询） */
    fun onForegroundChanged(context: Context, packageName: String) {
        LimitDiagnostics.log(context, "manager", "前台变化: $packageName")

        // 忽略本应用自己：拦截页本身会触发窗口事件，不能当成"离开了超限应用"
        if (packageName == context.packageName) {
            LimitDiagnostics.log(context, "manager", "是本应用自己，忽略")
            return
        }

        // 本应用界面正显示（用户就在业精于勤里）时，任何外部应用事件都是过期/误投的，直接忽略
        if (AppForeground.visible) {
            LimitDiagnostics.log(context, "manager", "本应用界面正显示，忽略外部事件 $packageName")
            return
        }

        // 前台已切到别的应用：取消尚未确认的拦截任务（防止切换到别处后误弹）
        val pendingBefore = pendingConfirm
        if (pendingBefore != null && pendingBefore.limit.packageName != packageName) {
            LimitDiagnostics.log(
                context, "manager",
                "前台已切到 $packageName，取消待确认的 ${pendingBefore.limit.packageName} 拦截"
            )
            cancelPendingBlock()
        }

        // 用户已离开被拦截的应用 → 立即收起拦截页
        if (BlockActivity.isShowing()) {
            if (BlockActivity.isForPackage(packageName)) {
                LimitDiagnostics.log(context, "manager", "拦截页正展示同一应用，不重复处理")
                return
            }
            LimitDiagnostics.log(context, "manager", "已离开拦截目标，收起拦截页")
            BlockActivity.dismissCurrent()
        }

        val limit = LimitStore(context).load()
            .firstOrNull { it.packageName == packageName && it.enabled }
        if (limit == null) {
            LimitDiagnostics.log(context, "manager", "$packageName 未设置启用限制，跳过")
            return
        }

        val now = System.currentTimeMillis()
        if (gracePackage == packageName && now < graceUntilMs) {
            LimitDiagnostics.log(context, "manager", "$packageName 处于宽限期，跳过")
            return
        }

        val used = UsageChecker.minutesUsedToday(context, packageName)
        if (!LimitChecker.isOverLimit(used, limit.maxMinutes)) {
            LimitDiagnostics.log(context, "manager", "$packageName 已用 $used 分钟，未超 ${limit.maxMinutes}，跳过")
            return
        }

        val pending = pendingConfirm
        if (pending != null && pending.limit.packageName == packageName &&
            now < pending.startedAt + CONFIRM_TIMEOUT_MS
        ) {
            LimitDiagnostics.log(context, "manager", "$packageName 已有待确认任务，跳过重复")
            return
        }

        LimitDiagnostics.log(context, "manager", "$packageName 超限($used 分钟)，安排 ${CONFIRM_DELAY_MS}ms 后二次确认")
        scheduleConfirm(context, limit, used, now, requireRootMatch = false)
    }

    private fun scheduleConfirm(
        context: Context,
        limit: AppLimit,
        usedMinutes: Int,
        now: Long,
        requireRootMatch: Boolean
    ) {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        val pending = PendingConfirm(context.applicationContext, limit, usedMinutes, now)
        pendingConfirm = pending
        pendingRunnable = Runnable {
            pendingConfirm = null
            pendingRunnable = null
            confirmAndBlock(pending, 0, requireRootMatch)
        }
        handler.postDelayed(pendingRunnable!!, CONFIRM_DELAY_MS)
    }

    /** 取消尚未确认的拦截任务（本应用页面创建、或前台切到其他应用时调用） */
    fun cancelPendingBlock() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
        pendingConfirm = null
    }

    /**
     * 二次确认：
     * - 事件路径（requireRootMatch=false）：本机 rootInActiveWindow 常返回桌面/不可信，
     *   因此只防"活动窗口是本应用自己"一种误判，其余直接信任新鲜事件，保证该弹就弹；
     * - 兜底路径（requireRootMatch=true）：必须核对到目标应用才弹，避免把旧前台误当当前。
     */
    private fun confirmAndBlock(pending: PendingConfirm, attempt: Int, requireRootMatch: Boolean) {
        val pkg = pending.limit.packageName
        val context = pending.context
        val active = LimitWatchService.activeWindowPackage()
        if (requireRootMatch) {
            if (active != null && active != pkg) {
                LimitDiagnostics.log(context, "manager", "兜底确认失败：活动窗口=$active，目标=$pkg，放弃弹窗")
                return
            }
            if (active == null && attempt < MAX_CONFIRM_RETRIES) {
                // 窗口可能还没就绪：再确认一次
                LimitDiagnostics.log(context, "manager", "兜底确认：活动窗口暂不可读，${ROOT_RETRY_MS}ms 后重试")
                handler.postDelayed({ confirmAndBlock(pending, attempt + 1, true) }, ROOT_RETRY_MS)
                return
            }
        } else {
            if (active == context.packageName) {
                LimitDiagnostics.log(context, "manager", "确认时活动窗口是本应用自己，放弃弹窗")
                return
            }
            LimitDiagnostics.log(context, "manager", "事件确认 root=$active（本机 root 不可信，仅记录）")
        }
        if (AppForeground.visible) {
            LimitDiagnostics.log(context, "manager", "确认时本应用界面正显示，放弃弹窗")
            return
        }
        val now = System.currentTimeMillis()
        if (gracePackage == pkg && now < graceUntilMs) {
            LimitDiagnostics.log(context, "manager", "确认时处于宽限期，放弃")
            return
        }
        if (BlockActivity.isShowing() && BlockActivity.isForPackage(pkg)) {
            LimitDiagnostics.log(context, "manager", "确认时拦截页已展示，跳过")
            return
        }
        if (now - (lastNotifiedAt[pkg] ?: 0L) >= NOTIFY_COOLDOWN_MS) {
            NotificationHelper.showLimitExceeded(context, pending.limit, pending.usedMinutes)
            lastNotifiedAt[pkg] = now
        }
        LimitDiagnostics.log(context, "manager", "确认通过，弹出拦截页 $pkg")
        showBlock(context, pending.limit, pending.usedMinutes)
    }

    /** WorkManager 兜底：仅当超限应用确实是当前前台时才补发通知，不弹拦截页 */
    fun checkAllLimits(context: Context) {
        // 本应用界面正显示时不兜底判定，避免使用统计延迟导致误通知
        if (AppForeground.visible) {
            LimitDiagnostics.log(context, "manager", "兜底检查：本应用正显示，跳过")
            return
        }
        val pkg = LimitWatchService.lastForegroundPackage
            ?: UsageChecker.currentForegroundPackage(context)
            ?: run {
                LimitDiagnostics.log(context, "manager", "兜底检查：无法确定当前前台，跳过")
                return
            }
        if (pkg == context.packageName) return
        val limit = LimitStore(context).load()
            .firstOrNull { it.packageName == pkg && it.enabled }
            ?: run {
                LimitDiagnostics.log(context, "manager", "兜底检查：$pkg 无启用限制，跳过")
                return
            }
        val now = System.currentTimeMillis()
        if (gracePackage == pkg && now < graceUntilMs) {
            LimitDiagnostics.log(context, "manager", "兜底检查：$pkg 处于宽限期，跳过")
            return
        }
        if (BlockActivity.isShowing() && BlockActivity.isForPackage(pkg)) {
            LimitDiagnostics.log(context, "manager", "兜底检查：拦截页已展示，跳过")
            return
        }
        val used = UsageChecker.minutesUsedToday(context, pkg)
        if (!LimitChecker.isOverLimit(used, limit.maxMinutes)) {
            LimitDiagnostics.log(context, "manager", "兜底检查：$pkg 已用 $used 分钟，未超 ${limit.maxMinutes}，跳过")
            return
        }
        LimitDiagnostics.log(context, "manager", "兜底检查：$pkg 超限，安排确认后弹拦截")
        scheduleConfirm(context, limit, used, now, requireRootMatch = true)
    }

    fun setGrace(packageName: String, durationMs: Long) {
        gracePackage = packageName
        graceUntilMs = System.currentTimeMillis() + durationMs
    }

    private fun showBlock(context: Context, limit: AppLimit, usedMinutes: Int) {
        BlockActivity.dismissCurrent()
        val intent = Intent(context, BlockActivity::class.java).apply {
            putExtra(BlockActivity.EXTRA_PACKAGE, limit.packageName)
            putExtra(BlockActivity.EXTRA_LABEL, limit.appLabel)
            putExtra(BlockActivity.EXTRA_USED, usedMinutes)
            putExtra(BlockActivity.EXTRA_MAX, limit.maxMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
            LimitDiagnostics.log(context, "manager", "拦截页启动成功")
        } catch (e: Exception) {
            // 系统限制后台启动等情况：静默降级，仅保留通知提醒
            LimitDiagnostics.log(
                context, "manager",
                "拦截页启动失败: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private const val CONFIRM_DELAY_MS = 300L
    private const val CONFIRM_TIMEOUT_MS = 1_500L
    private const val ROOT_RETRY_MS = 200L
    private const val MAX_CONFIRM_RETRIES = 1
    private const val NOTIFY_COOLDOWN_MS = 10 * 60_000L
}
