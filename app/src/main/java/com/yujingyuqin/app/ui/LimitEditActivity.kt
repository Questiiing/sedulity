package com.yujingyuqin.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yujingyuqin.app.R
import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.data.LimitStore
import com.yujingyuqin.app.databinding.ActivityLimitEditBinding
import com.yujingyuqin.app.limit.LimitManager
import com.yujingyuqin.app.util.AppForeground
import com.yujingyuqin.app.util.StatusBarInsets

class LimitEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLimitEditBinding
    private var existingLimit: AppLimit? = null
    private var selectedPackage: String? = null
    private var selectedLabel: String? = null

    private val appPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val pkg = result.data?.getStringExtra(AppPickerActivity.EXTRA_PACKAGE)
                    ?: return@registerForActivityResult
                val label = try {
                    val pm = packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }
                selectedPackage = pkg
                selectedLabel = label
                updateAppRow()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppForeground.visible = true
        LimitManager.cancelPendingBlock()
        binding = ActivityLimitEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarInsets.applyTopInset(binding.topBar)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        existingLimit = pkg?.let {
            LimitStore(this).load().firstOrNull { l -> l.packageName == it }
        }
        val editing = existingLimit != null
        val limit = existingLimit ?: AppLimit(packageName = "", appLabel = "")
        selectedPackage = limit.packageName.ifEmpty { null }
        selectedLabel = limit.appLabel.ifEmpty { null }

        binding.tvTitle.text = if (editing) "编辑上限" else "添加上限"
        binding.btnDelete.visibility = if (editing) View.VISIBLE else View.GONE
        binding.sliderLimit.value = limit.maxMinutes.toFloat().coerceIn(5f, 600f)
        binding.switchEnabled.isChecked = limit.enabled
        updateAppRow()
        updateLimitText()

        binding.btnBack.setOnClickListener { finish() }
        binding.cardApp.setOnClickListener {
            appPickerLauncher.launch(Intent(this, AppPickerActivity::class.java))
        }
        binding.sliderLimit.addOnChangeListener { _, _, _ -> updateLimitText() }
        binding.btnSave.setOnClickListener { saveLimit() }
        binding.btnDelete.setOnClickListener { deleteLimit() }
    }

    override fun onResume() {
        super.onResume()
        AppForeground.visible = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.visible = false
    }

    private fun updateAppRow() {
        val pkg = selectedPackage
        if (pkg == null) {
            binding.tvAppName.text = ""
            binding.tvAppHint.text = "点击选择应用"
            binding.ivAppIcon.setImageResource(R.drawable.ic_app_fallback)
        } else {
            binding.tvAppName.text = selectedLabel ?: pkg
            binding.tvAppHint.text = pkg
            binding.ivAppIcon.setImageDrawable(
                try {
                    packageManager.getApplicationIcon(pkg)
                } catch (e: Exception) {
                    null
                }
            )
        }
    }

    private fun updateLimitText() {
        binding.tvLimitValue.text = "${binding.sliderLimit.value.toInt()} 分钟"
    }

    private fun saveLimit() {
        val pkg = selectedPackage
        if (pkg.isNullOrEmpty()) {
            Toast.makeText(this, "请先选择应用", Toast.LENGTH_SHORT).show()
            return
        }
        val base = existingLimit
            ?: AppLimit(packageName = pkg, appLabel = selectedLabel ?: pkg)
        val updated = base.copy(
            appLabel = selectedLabel ?: base.appLabel,
            maxMinutes = binding.sliderLimit.value.toInt(),
            enabled = binding.switchEnabled.isChecked
        )
        LimitStore(this).upsert(updated)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteLimit() {
        val pkg = selectedPackage ?: return
        LimitStore(this).delete(pkg)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}
