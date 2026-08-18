package com.yujingyuqin.app.usage

object UsageTimeCalculator {
    /** entries: (packageName, foregroundMillis) */
    fun sumForegroundMillis(packageName: String, entries: List<Pair<String, Long>>): Long =
        entries.filter { it.first == packageName }.sumOf { it.second }

    fun minutes(ms: Long): Int = (ms / 60_000L).toInt()
}
