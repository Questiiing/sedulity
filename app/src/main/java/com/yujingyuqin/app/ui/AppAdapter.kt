package com.yujingyuqin.app.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yujingyuqin.app.R
import com.yujingyuqin.app.databinding.ItemAppBinding

data class AppInfo(
    val packageName: String,
    val label: String
)

object AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
    override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
        oldItem.packageName == newItem.packageName

    override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
        oldItem == newItem
}

class AppAdapter(
    private val onClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.VH>(AppDiffCallback) {

    class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val info = getItem(position)
        val b = holder.binding
        b.tvAppLabel.text = info.label
        b.tvPackage.text = info.packageName
        b.ivIcon.setImageDrawable(loadIcon(b.root.context, info))
        b.root.setOnClickListener { onClick(info) }
    }

    private fun loadIcon(context: android.content.Context, info: AppInfo): Drawable? = try {
        context.packageManager.getApplicationIcon(info.packageName)
    } catch (e: Exception) {
        ContextCompat.getDrawable(context, R.drawable.ic_app_fallback)
    }

}
