package com.yujingyuqin.app.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object StatusBarInsets {

    /** 将视图内容下移到状态栏之下（保留视图背景铺满状态栏区域） */
    fun applyTopInset(view: View) {
        val baseTop = view.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, baseTop + top, v.paddingRight, v.paddingBottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
