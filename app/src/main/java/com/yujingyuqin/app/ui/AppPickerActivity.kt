package com.yujingyuqin.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.yujingyuqin.app.databinding.ActivityAppPickerBinding
import com.yujingyuqin.app.limit.LimitManager
import com.yujingyuqin.app.util.AppForeground
import com.yujingyuqin.app.util.StatusBarInsets

class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var adapter: AppAdapter
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppForeground.visible = true
        LimitManager.cancelPendingBlock()
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarInsets.applyTopInset(binding.topBar)

        adapter = AppAdapter { info ->
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_PACKAGE, info.packageName)
            )
            finish()
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        loadApps()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filter(s?.toString().orEmpty().trim())
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AppForeground.visible = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.visible = false
    }

    private fun loadApps() {
        val pm = packageManager
        allApps.clear()
        for (app in pm.getInstalledApplications(0)) {
            if (app.packageName == packageName) continue
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue
            val label = try {
                pm.getApplicationLabel(app).toString()
            } catch (e: Exception) {
                app.packageName
            }
            allApps.add(AppInfo(app.packageName, label))
        }
        allApps.sortBy { it.label.lowercase() }
        filter("")
    }

    private fun filter(query: String) {
        val list = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(list)
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}
