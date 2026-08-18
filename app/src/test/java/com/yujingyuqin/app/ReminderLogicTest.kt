package com.yujingyuqin.app

import com.yujingyuqin.app.data.Task
import com.yujingyuqin.app.reminder.ReminderLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderLogicTest {

    private fun calendarOf(hour: Int, minute: Int, day: Int = 15): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.AUGUST, day, hour, minute, 0)
        }

    @Test
    fun `nextReminderTime returns today when time not passed`() {
        val now = calendarOf(10, 0)
        val task = Task(packageName = "p", appLabel = "P", reminderHour = 21, reminderMinute = 0)
        val next = ReminderLogic.nextReminderTime(now, task)
        assertEquals(15, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, next.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `nextReminderTime returns tomorrow when time passed`() {
        val now = calendarOf(22, 30)
        val task = Task(packageName = "p", appLabel = "P", reminderHour = 21, reminderMinute = 0)
        val next = ReminderLogic.nextReminderTime(now, task)
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, next.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `reminderTimeReached boundary`() {
        val task = Task(packageName = "p", appLabel = "P", reminderHour = 21, reminderMinute = 0)
        assertFalse(ReminderLogic.reminderTimeReached(calendarOf(20, 59), task))
        assertTrue(ReminderLogic.reminderTimeReached(calendarOf(21, 0), task))
        assertTrue(ReminderLogic.reminderTimeReached(calendarOf(23, 59), task))
    }

    @Test
    fun `nextRepeatTime within day returns interval later`() {
        val nowMs = calendarOf(21, 0).timeInMillis
        val endOfDayMs = calendarOf(23, 59).timeInMillis
        val next = ReminderLogic.nextRepeatTime(nowMs, endOfDayMs)
        assertEquals(nowMs + ReminderLogic.REPEAT_INTERVAL_MS, next)
    }

    @Test
    fun `nextRepeatTime beyond end of day returns null`() {
        val nowMs = calendarOf(23, 40).timeInMillis
        val endOfDayMs = calendarOf(23, 59).timeInMillis
        assertNull(ReminderLogic.nextRepeatTime(nowMs, endOfDayMs))
    }
}
