package com.yujingyuqin.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.yujingyuqin.app.R
import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.data.LimitStore
import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.data.TaskStore
import com.yujingyuqin.app.databinding.ActivityMainBinding
import com.yujingyuqin.app.limit.LimitDiagnostics
import com.yujingyuqin.app.limit.LimitManager
import com.yujingyuqin.app.limit.LimitWatchService
import com.yujingyuqin.app.reminder.ReminderManager
import com.yujingyuqin.app.usage.UsageChecker
import com.yujingyuqin.app.util.AppForeground
import com.yujingyuqin.app.util.StatusBarInsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var limitAdapter: LimitAdapter
    private var currentTab = 0

    private val taskEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refresh()
        }
    private val limitEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppForeground.visible = true
        LimitManager.cancelPendingBlock()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarInsets.applyTopInset(binding.headerRoot)

        taskAdapter = TaskAdapter { task -> openTaskEditor(task) }
        binding.taskList.layoutManager = LinearLayoutManager(this)
        binding.taskList.adapter = taskAdapter

        limitAdapter = LimitAdapter { limit -> openLimitEditor(limit) }
        binding.limitList.layoutManager = LinearLayoutManager(this)
        binding.limitList.adapter = limitAdapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                refresh()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.fabAdd.setOnClickListener {
            if (currentTab == 0) openTaskEditor(null) else openLimitEditor(null)
        }
        binding.btnGrantUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnExactAlarm.setOnClickListener { openExactAlarmSettings() }
        binding.btnAccessibility.setOnClickListener { openAccessibilitySettings() }
        binding.btnDiagAccessibility.setOnClickListener { openAccessibilitySettings() }
        binding.btnDiagCheck.setOnClickListener { showSelfTest() }
        binding.btnDiagLog.setOnClickListener { showDiagnosticsLog() }
        setupVersion()

        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        AppForeground.visible = true
        ReminderManager.checkAllTasks(this)
        refresh()
    }

    override fun onStop() {
        super.onStop()
        AppForeground.visible = false
    }

    private fun refresh() {
        refreshTasks()
        refreshLimits()
        refreshBanners()
        refreshDiagnostics()
        binding.tvDate.text =
            SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
    }

    private fun refreshTasks() {
        val tasks = TaskStore(this).load()
        val hasUsage = UsageChecker.needsUsageAccess(this)
        val items = tasks.map { task ->
            task to if (hasUsage) UsageChecker.minutesUsedToday(this, task.packageName) else 0
        }
        taskAdapter.submitList(items)

        val onTaskTab = currentTab == 0
        binding.taskList.visibility =
            if (onTaskTab && tasks.isNotEmpty()) View.VISIBLE else View.GONE
        binding.taskEmptyState.visibility =
            if (onTaskTab && tasks.isEmpty()) View.VISIBLE else View.GONE

        val usageMap = items.associate { it.first.packageName to it.second }
        val enabled = tasks.filter { it.enabled }
        val doneCount = enabled.count { (usageMap[it.packageName] ?: 0) >= it.targetMinutes }
        binding.tvSummary.text = if (enabled.isEmpty()) {
            "还没有任务，点右下角 + 添加第一个"
        } else {
            "今日 $doneCount/${enabled.size} 个任务达标"
        }
    }

    private fun refreshLimits() {
        val limits = LimitStore(this).load()
        val hasUsage = UsageChecker.needsUsageAccess(this)
        val items = limits.map { limit ->
            limit to if (hasUsage) UsageChecker.minutesUsedToday(this, limit.packageName) else 0
        }
        limitAdapter.submitList(items)

        val onLimitTab = currentTab == 1
        binding.limitList.visibility =
            if (onLimitTab && limits.isNotEmpty()) View.VISIBLE else View.GONE
        binding.limitEmptyState.visibility =
            if (onLimitTab && limits.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshBanners() {
        val hasUsage = UsageChecker.needsUsageAccess(this)
        binding.bannerUsage.visibility = if (hasUsage) View.GONE else View.VISIBLE
        binding.bannerExact.visibility =
            if (canScheduleExactAlarm()) View.GONE else View.VISIBLE

        val showLimitBanners = currentTab == 1
        binding.bannerAccessibility.visibility =
            if (showLimitBanners && !isAccessibilityEnabled()) View.VISIBLE else View.GONE
    }

    private fun refreshDiagnostics() {
        val onLimitTab = currentTab == 1
        binding.diagCard.visibility = if (onLimitTab) View.VISIBLE else View.GONE
        if (!onLimitTab) return

        val enabled = isAccessibilityEnabled()
        binding.tvDiagAccessibility.text =
            if (enabled) "无障碍服务：已开启" else "无障碍服务：未开启"
        binding.tvDiagAccessibility.setTextColor(
            ContextCompat.getColor(this, if (enabled) R.color.success else R.color.danger)
        )

        val lastEvent = LimitWatchService.lastEventTimeMs
        if (lastEvent <= 0L) {
            binding.tvDiagLastEvent.text = "最近事件：从未收到"
            binding.tvDiagEventDelay.visibility = View.GONE
        } else {
            binding.tvDiagLastEvent.text =
                "最近事件：" + SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(lastEvent)) +
                    "（第 ${LimitWatchService.eventCount} 个）"
            val lag = LimitWatchService.lastEventLagMs
            binding.tvDiagEventDelay.visibility = View.VISIBLE
            binding.tvDiagEventDelay.text =
                if (lag > EVENT_LAG_WARN_MS) {
                    "事件延迟：${lag}ms（较大，可能被系统省电影响）"
                } else {
                    "事件延迟：${lag}ms"
                }
        }

        val lastPkg = LimitWatchService.lastForegroundPackage
        binding.tvDiagForeground.text =
            if (lastPkg == null) "最近识别前台：无" else "最近识别前台：${appLabelOf(lastPkg)}"

        binding.tvDiagActiveWindow.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            val active = LimitWatchService.activeWindowPackage()
            binding.tvDiagActiveWindow.text =
                if (active == null) "当前活动窗口：读取不到（HyperOS 常见，不影响判断）"
                else "当前活动窗口：${appLabelOf(active)}"
        }

        val now = System.currentTimeMillis()
        val stale = lastEvent > 0L && now - lastEvent > DIAG_STALE_MS
        binding.tvDiagHint.visibility =
            if (enabled && (lastEvent <= 0L || stale)) View.VISIBLE else View.GONE
        binding.tvDiagHint.text = if (lastEvent <= 0L) {
            "已开启却收不到事件：多半被「受限设置」拦住了。请到 设置→应用→应用管理→业精于勤→右上角更多→允许受限设置，然后关闭再重新开启无障碍。"
        } else {
            "最近 2 分钟没有新事件：请切换几次应用测试；仍无更新可能是 HyperOS 关掉了无障碍服务，请重新开启。"
        }
    }

    private fun showDiagnosticsLog() {
        val logText = LimitDiagnostics.readLog(this)
        AlertDialog.Builder(this)
            .setTitle("拦截诊断日志")
            .setMessage(if (logText.isBlank() || logText == "（暂无日志）") {
                "暂无日志。请先开启无障碍，多切换几个应用（尤其打开一次超限应用），再回来点「导出日志」。"
            } else {
                logText
            })
            .setPositiveButton("复制日志") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("limit_log", logText))
                Toast.makeText(this, "日志已复制，粘贴发给我即可", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /** 一键自检：把无障碍、事件链路、各限制应用的统计情况一次性列出来 */
    private fun showSelfTest() {
        val sb = StringBuilder()
        val enabled = isAccessibilityEnabled()
        sb.append(if (enabled) "✔ 无障碍服务：已开启" else "✘ 无障碍服务：未开启（先点右上角「无障碍设置」开启）")
            .append("\n")

        val lastEvent = LimitWatchService.lastEventTimeMs
        if (lastEvent <= 0L) {
            sb.append("✘ 最近事件：从未收到\n")
            sb.append("  已开启却收不到事件 → 多半被「受限设置」拦住：\n")
            sb.append("  设置→应用→应用管理→业精于勤→更多→允许受限设置\n")
        } else {
            sb.append("✔ 最近事件：")
                .append(SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(lastEvent)))
                .append("（第 ").append(LimitWatchService.eventCount).append(" 个，延迟 ")
                .append(LimitWatchService.lastEventLagMs).append("ms）\n")
        }

        val active = LimitWatchService.activeWindowPackage()
        sb.append(
            if (active == null) "• 当前活动窗口：读取不到（HyperOS 常见，不影响判断）"
            else "• 当前活动窗口：${appLabelOf(active)}"
        ).append("\n\n")

        val limits = LimitStore(this).load().filter { it.enabled }
        if (limits.isEmpty()) {
            sb.append("还没有启用任何使用限制，请先添加一个再测。")
        } else {
            sb.append("已启用的限制：\n")
            val hasUsage = UsageChecker.needsUsageAccess(this)
            limits.forEach { limit ->
                val used = if (hasUsage) UsageChecker.minutesUsedToday(this, limit.packageName) else 0
                val status = when {
                    !hasUsage -> "（未开启使用情况访问）"
                    used >= limit.maxMinutes -> "已超限"
                    else -> "剩余 ${limit.maxMinutes - used} 分钟"
                }
                sb.append("· ${limit.appLabel}：上限 ${limit.maxMinutes} 分钟，统计已用 $used 分钟 → $status\n")
            }
            sb.append("\n提示：系统使用统计可能滞后几分钟。刚用超的时间不会立刻计入，请等 1-2 分钟再退出重进测试。")
        }

        AlertDialog.Builder(this)
            .setTitle("拦截自检")
            .setMessage(sb.toString())
            .setPositiveButton("复制结果") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("limit_selfcheck", sb.toString()))
                Toast.makeText(this, "已复制，粘贴发给我即可", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun appLabelOf(packageName: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }

    private fun openTaskEditor(task: Task?) {
        val intent = Intent(this, EditTaskActivity::class.java)
        if (task != null) {
            intent.putExtra(EditTaskActivity.EXTRA_PACKAGE, task.packageName)
        }
        taskEditLauncher.launch(intent)
    }

    private fun openLimitEditor(limit: AppLimit?) {
        val intent = Intent(this, LimitEditActivity::class.java)
        if (limit != null) {
            intent.putExtra(LimitEditActivity.EXTRA_PACKAGE, limit.packageName)
        }
        limitEditLauncher.launch(intent)
    }

    private fun canScheduleExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, LimitWatchService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun openExactAlarmSettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION
            )
        }
    }

    private fun setupVersion() {
        val versionName = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        binding.tvVersion.text = "v$versionName"
        binding.tvVersion.setOnClickListener { showChangelog() }
    }

    private fun showChangelog() {
        AlertDialog.Builder(this)
            .setTitle("版本更新记录")
            .setMessage(getString(R.string.changelog))
            .setPositiveButton("知道了", null)
            .show()
    }

    private companion object {
        const val REQUEST_NOTIFICATION = 100
        const val DIAG_STALE_MS = 2 * 60_000L
        const val EVENT_LAG_WARN_MS = 2_000L
    }
}
