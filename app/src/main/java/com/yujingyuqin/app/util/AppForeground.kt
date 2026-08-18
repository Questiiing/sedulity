package com.yujingyuqin.app.util

/**
 * 记录本应用是否有界面正显示在屏幕上。
 * 用于拦截防误判：只要业精于勤的任何页面在前台，
 * 收到的一切外部应用事件都视为过期/误投，直接忽略。
 */
object AppForeground {
    @Volatile
    var visible: Boolean = false
}
