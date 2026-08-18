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
import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.databinding.ItemTaskBinding

object TaskDiffCallback : DiffUtil.ItemCallback<Pair<Task, Int>>() {
    override fun areItemsTheSame(
        oldItem: Pair<Task, Int>,
        newItem: Pair<Task, Int>
    ): Boolean = oldItem.first.packageName == newItem.first.packageName

    override fun areContentsTheSame(
        oldItem: Pair<Task, Int>,
        newItem: Pair<Task, Int>
    ): Boolean = oldItem == newItem
}

class TaskAdapter(
    private val onClick: (Task) -> Unit
) : ListAdapter<Pair<Task, Int>, TaskAdapter.VH>(TaskDiffCallback) {

    class VH(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (task, used) = getItem(position)
        val b = holder.binding
        val context = b.root.context

        b.ivIcon.setImageDrawable(loadIcon(context, task))
        b.tvName.text = task.appLabel
        b.tvDetail.text = "目标 ${task.targetMinutes} 分钟 · 已用 $used 分钟"

        val target = task.targetMinutes.coerceAtLeast(1)
        val progress = (used.toFloat() / target).coerceIn(0f, 1f)
        b.progressBar.setProgressCompat((progress * 100).toInt(), true)

        when {
            !task.enabled -> {
                b.tvStatus.text = "已停用"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_gray)
                b.tvStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
            }
            used >= task.targetMinutes -> {
                b.tvStatus.text = "已达标 ✓"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_green)
                b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.success)
                )
            }
            else -> {
                b.tvStatus.text = "还差 ${task.targetMinutes - used} 分钟"
                b.tvStatus.setBackgroundResource(R.drawable.bg_chip_orange)
                b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                b.progressBar.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }
        }

        b.root.setOnClickListener { onClick(task) }
    }

    private fun loadIcon(context: Context, task: Task): Drawable? = try {
        context.packageManager.getApplicationIcon(task.packageName)
    } catch (e: Exception) {
        ContextCompat.getDrawable(context, R.drawable.ic_app_fallback)
    }

}
