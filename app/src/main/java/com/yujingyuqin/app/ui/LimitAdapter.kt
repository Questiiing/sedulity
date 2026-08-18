package com.yujingyuqin.app.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yujingyuqin.app.R
import com.yujingyuqin.app.data.AppLimit
import com.yujingyuqin.app.databinding.ItemLimitBinding
import com.yujingyuqin.app.limit.LimitChecker

object LimitDiffCallback : DiffUtil.ItemCallback<Pair<AppLimit, Int>>() {
    override fun areItemsTheSame(
        oldItem: Pair<AppLimit, Int>,
        newItem: Pair<AppLimit, Int>
    ): Boolean = oldItem.first.packageName == newItem.first.packageName

    override fun areContentsTheSame(
        oldItem: Pair<AppLimit, Int>,
        newItem: Pair<AppLimit, Int>
    ): Boolean = oldItem == newItem
}

class LimitAdapter(
    private val onClick: (AppLimit) -> Unit
) : ListAdapter<Pair<AppLimit, Int>, LimitAdapter.VH>(LimitDiffCallback) {

    class VH(val binding: ItemLimitBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemLimitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (limit, used) = getItem(position)
        val b = holder.binding
        val context = b.root.context

        b.ivIcon.setImageDrawable(loadIcon(context, limit))
        b.tvName.text = limit.appLabel
        b.tvDetail.text = "上限 ${limit.maxMinutes} 分钟 · 已用 $used 分钟"
        b.progressBar.setProgressCompat(
            (LimitChecker.progress(used, limit.maxMinutes) * 100).toInt(),
            true
        )

        when {
            !limit.enabled -> {
                b.tvStatus.text = "已停用"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_gray)
                b.tvStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
            }
            LimitChecker.isOverLimit(used, limit.maxMinutes) -> {
                b.tvStatus.text = "已超限"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_danger)
                b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.danger))
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.danger)
                )
            }
            else -> {
                b.tvStatus.text = "剩余 ${LimitChecker.remainingMinutes(used, limit.maxMinutes)} 分钟"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_blue)
                b.tvStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.primary_dark)
                )
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }
        }

        b.root.setOnClickListener { onClick(limit) }
    }

    private fun loadIcon(context: Context, limit: AppLimit): Drawable? = try {
        context.packageManager.getApplicationIcon(limit.packageName)
    } catch (e: Exception) {
        ContextCompat.getDrawable(context, R.drawable.ic_app_fallback)
    }
}
