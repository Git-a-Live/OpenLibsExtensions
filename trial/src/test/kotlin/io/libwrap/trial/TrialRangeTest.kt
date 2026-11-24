package io.libwrap.trial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialRangeTest {

    @Test
    fun `比例区间记录起止与长度`() {
        val range = TrialRange(0.2f, 0.6f)

        assertEquals(0.2f, range.start, 1e-6f)
        assertEquals(0.6f, range.end, 1e-6f)
        assertEquals(0.4f, range.length, 1e-6f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `起止顺序颠倒时拒绝构造`() {
        TrialRange(0.6f, 0.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `越出 0 到 1 时拒绝构造`() {
        TrialRange(-0.1f, 0.5f)
    }

    @Test
    fun `包含判断含端点`() {
        val range = TrialRange(0.2f, 0.6f)

        assertTrue(0.2f in range)
        assertTrue(0.6f in range)
        assertTrue(0.4f in range)
        assertFalse(0.199f in range)
        assertFalse(0.601f in range)
    }

    @Test
    fun `毫秒换算为比例`() {
        val range = TrialRange.ofMillis(startMillis = 30_000, endMillis = 60_000, durationMillis = 120_000)

        assertEquals(0.25f, range!!.start, 1e-6f)
        assertEquals(0.5f, range.end, 1e-6f)
    }

    @Test
    fun `超出时长的毫秒被钳制`() {
        val range = TrialRange.ofMillis(startMillis = 100_000, endMillis = 200_000, durationMillis = 120_000)

        assertEquals(100_000f / 120_000f, range!!.start, 1e-6f)
        assertEquals(1f, range.end, 1e-6f)
    }

    @Test
    fun `换算后区间长度为零时返回 null`() {
        assertNull(TrialRange.ofMillis(startMillis = 50_000, endMillis = 50_000, durationMillis = 120_000))
        assertNull(TrialRange.ofMillis(startMillis = 200_000, endMillis = 300_000, durationMillis = 120_000))
    }

    @Test
    fun `时长非正时返回 null`() {
        assertNull(TrialRange.ofMillis(startMillis = 0, endMillis = 10, durationMillis = 0))
        assertNull(TrialRange.ofMillis(startMillis = 0, endMillis = 10, durationMillis = -1))
    }

    @Test
    fun `整段区间覆盖全部比例`() {
        assertEquals(0f, TrialRange.Full.start, 1e-6f)
        assertEquals(1f, TrialRange.Full.end, 1e-6f)
        assertTrue(0.5f in TrialRange.Full)
    }
}
