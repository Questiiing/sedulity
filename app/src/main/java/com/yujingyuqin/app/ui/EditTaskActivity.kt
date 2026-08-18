package com.yujingyuqin.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.data.TaskStore
import com.yujingyuqin.app.databinding.ActivityEditTaskBinding
import com.yujingyuqin.app.limit.LimitManager
import com.yujingyuqin.app.reminder.ReminderManager
import com.yujingyuqin.app.util.AppForeground
import com.yujingyuqin.app.util.StatusBarInsets
import java.util.Locale

class EditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTaskBinding
    private var existingTask: Task? = null
    private var selectedPackage: String? = null
    private var selectedLabel: String? = null
    private var reminderHour = 21
    private var reminderMinute = 0

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
        binding = ActivityEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarInsets.applyTopInset(binding.topBar)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        existingTask = pkg?.let {
            TaskStore(this).load().firstOrNull { t -> t.packageName == it }
        }
        val editing = existingTask != null
        val task = existingTask ?: Task(packageName = "", appLabel = "")
        selectedPackage = task.packageName.ifEmpty { null }
        selectedLabel = task.appLabel.ifEmpty { null }
        reminderHour = task.reminderHour
        reminderMinute = task.reminderMinute

        binding.tvTitle.text = if (editing) "编辑任务" else "添加任务"
        binding.btnDelete.visibility = if (editing) View.VISIBLE else View.GONE
        binding.sliderTarget.value = task.targetMinutes.toFloat().coerceIn(5f, 300f)
        binding.switchRepeat.isChecked = task.repeatUntilDone
        binding.switchEnabled.isChecked = task.enabled
        updateAppRow()
        updateTargetText()
        updateTimeText()

        binding.btnBack.setOnClickListener { finish() }
        binding.cardApp.setOnClickListener {
            appPickerLauncher.launch(
                android.content.Intent(this, AppPickerActivity::class.java)
            )
        }
        binding.sliderTarget.addOnChangeListener { _, _, _ -> updateTargetText() }
        binding.cardTime.setOnClickListener { showTimePicker() }
        binding.btnSave.setOnClickListener { saveTask() }
        binding.btnDelete.setOnClickListener { deleteTask() }
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
            binding.ivAppIcon.setImageResource(
                com.yujingyuqin.app.R.drawable.ic_app_fallback
            )
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

    private fun updateTargetText() {
        val minutes = binding.sliderTarget.value.toInt()
        binding.tvTargetValue.text = "$minutes 分钟"
    }

    private fun updateTimeText() {
        binding.tvTimeValue.text =
            String.format(Locale.CHINA, "%02d:%02d", reminderHour, reminderMinute)
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(reminderHour)
            .setMinute(reminderMinute)
            .setTitleText("选择提醒时间")
            .build()
        picker.addOnPositiveButtonClickListener {
            reminderHour = picker.hour
            reminderMinute = picker.minute
            updateTimeText()
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun saveTask() {
        val pkg = selectedPackage
        if (pkg.isNullOrEmpty()) {
            Toast.makeText(this, "请先选择应用", Toast.LENGTH_SHORT).show()
            return
        }
        val base = existingTask ?: Task(packageName = pkg, appLabel = selectedLabel ?: pkg)
        val updated = base.copy(
            appLabel = selectedLabel ?: base.appLabel,
            targetMinutes = binding.sliderTarget.value.toInt(),
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            repeatUntilDone = binding.switchRepeat.isChecked,
            enabled = binding.switchEnabled.isChecked
        )
        TaskStore(this).upsert(updated)
        ReminderManager.rescheduleAll(this)
        ReminderManager.checkTask(this, updated)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteTask() {
        val pkg = selectedPackage ?: return
        TaskStore(this).delete(pkg)
        existingTask?.let { ReminderManager.cancelAlarms(this, it) }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}
