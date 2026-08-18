package com.yujingyuqin.app.limit

object LimitChecker {
    fun isOverLimit(usedMinutes: Int, maxMinutes: Int): Boolean = usedMinutes >= maxMinutes

    fun remainingMinutes(usedMinutes: Int, maxMinutes: Int): Int =
        (maxMinutes - usedMinutes).coerceAtLeast(0)

    fun progress(usedMinutes: Int, maxMinutes: Int): Float =
        (usedMinutes.toFloat() / maxMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
}
