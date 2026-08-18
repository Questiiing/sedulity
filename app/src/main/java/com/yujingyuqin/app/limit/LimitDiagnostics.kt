package com.yujingyuqin.app.limit

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拦截链路诊断日志：追加写入应用私有目录，
 * 用于排查"为什么没弹 / 为什么弹错应用"。
 */
object LimitDiagnostics {

    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.CHINA)

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        try {
            val line = "${formatter.format(Date())} [$tag] $message"
            val file = File(context.filesDir, LOG_FILE)
            file.appendText(line + "\n")
            if (file.length() > MAX_BYTES) {
                val lines = file.readLines()
                file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            // 日志写入失败不影响主功能
        }
    }

    fun readLog(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE)
            if (file.exists()) file.readText() else "（暂无日志）"
        } catch (e: Exception) {
            "读取日志失败：${e.message}"
        }
    }

    private const val LOG_FILE = "limit_log.txt"
    private const val MAX_BYTES = 200_000L
    private const val MAX_LINES = 300
}
