package com.yujingyuqin.app

import com.yujingyuqin.app.limit.LimitChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitCheckerTest {

    @Test
    fun `over limit boundary`() {
        assertFalse(LimitChecker.isOverLimit(29, 30))
        assertTrue(LimitChecker.isOverLimit(30, 30))
        assertTrue(LimitChecker.isOverLimit(31, 30))
    }

    @Test
    fun `remaining minutes never negative`() {
        assertEquals(18, LimitChecker.remainingMinutes(12, 30))
        assertEquals(0, LimitChecker.remainingMinutes(35, 30))
    }

    @Test
    fun `progress clamps within range`() {
        assertEquals(0.5f, LimitChecker.progress(15, 30))
        assertEquals(1f, LimitChecker.progress(60, 30))
        assertEquals(0f, LimitChecker.progress(0, 30))
    }
}
