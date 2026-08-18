package com.yujingyuqin.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yujingyuqin.app.R
import com.yujingyuqin.app.databinding.ActivityBlockBinding
import com.yujingyuqin.app.limit.LimitDiagnostics
import com.yujingyuqin.app.limit.LimitManager
import com.yujingyuqin.app.util.AppForeground

/**
 * 全屏拦截页：检测到打开超限应用时盖住它。
 * 30 秒倒计时后才能「继续使用」；按返回、回桌面或切走时自动结束。
 */
class BlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockBinding
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var blockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppForeground.visible = true
        LimitManager.cancelPendingBlock()
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        current = this
        render(intent)
        LimitDiagnostics.log(this, "block", "拦截页创建: ${intent.getStringExtra(EXTRA_PACKAGE)}")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    override fun onStop() {
        super.onStop()
        AppForeground.visible = false
        LimitDiagnostics.log(this, "block", "拦截页离开屏幕")
        // 离开拦截页（回桌面 / 切到别的应用）→ 自动结束
        if (!isFinishing) finish()
    }

    override fun onResume() {
        super.onResume()
        AppForeground.visible = true
    }

    override fun onDestroy() {
        LimitDiagnostics.log(this, "block", "拦截页销毁")
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        if (current === this) current = null
        super.onDestroy()
    }

    private fun render(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: blockedPackage ?: ""
        val used = intent.getIntExtra(EXTRA_USED, 0)
        val max = intent.getIntExtra(EXTRA_MAX, 0)
        binding.tvBlockDetail.text = "《$label》今日已用 $used 分钟，\n超过上限 $max 分钟"

        binding.btnHome.setOnClickListener {
            LimitDiagnostics.log(this, "block", "点击回桌面")
            goHome()
            finish()
        }
        startCountdown(binding.btnContinue)
    }

    private fun startCountdown(btn: Button) {
        var seconds = COUNTDOWN_SECONDS
        btn.isEnabled = false
        btn.text = "等待 $seconds 秒后继续使用"
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = object : Runnable {
            override fun run() {
                seconds--
                if (seconds > 0) {
                    btn.text = "等待 $seconds 秒后继续使用"
                    handler.postDelayed(this, 1000)
                } else {
                    btn.text = "继续使用"
                    btn.isEnabled = true
                    btn.setOnClickListener {
                        LimitDiagnostics.log(this@BlockActivity, "block", "点击继续使用，进入 60 秒宽限期")
                        LimitManager.setGrace(blockedPackage ?: "", GRACE_AFTER_CONTINUE_MS)
                        finish()
                    }
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (e: Exception) {
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_USED = "extra_used"
        const val EXTRA_MAX = "extra_max"
        const val GRACE_AFTER_CONTINUE_MS = 60_000L

        @Volatile
        private var current: BlockActivity? = null

        fun isShowing(): Boolean = current != null

        fun isForPackage(packageName: String): Boolean =
            current?.blockedPackage == packageName

        fun dismissCurrent() {
            val activity = current ?: return
            current = null
            activity.runOnUiThread { activity.finish() }
        }

        private const val COUNTDOWN_SECONDS = 30
    }
}
