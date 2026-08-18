package com.yujingyuqin.app.data

data class AppLimit(
    val packageName: String,
    var appLabel: String,
    var maxMinutes: Int = 60,
    var enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
