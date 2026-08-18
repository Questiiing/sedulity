package com.yujingyuqin.app

import com.yujingyuqin.app.usage.UsageTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTimeCalculatorTest {

    @Test
    fun `sumForegroundMillis only counts matching package`() {
        val entries = listOf(
            "com.read" to 12L * 60 * 1000,
            "com.read" to 3L * 60 * 1000,
            "com.other" to 99L * 60 * 1000
        )
        assertEquals(
            15L * 60 * 1000,
            UsageTimeCalculator.sumForegroundMillis("com.read", entries)
        )
    }

    @Test
    fun `sumForegroundMillis returns zero for unknown package`() {
        assertEquals(0L, UsageTimeCalculator.sumForegroundMillis("com.missing", emptyList()))
    }

    @Test
    fun `minutes floors millis`() {
        assertEquals(12, UsageTimeCalculator.minutes(12L * 60 * 1000 + 59_999))
        assertEquals(0, UsageTimeCalculator.minutes(59_999))
    }
}
