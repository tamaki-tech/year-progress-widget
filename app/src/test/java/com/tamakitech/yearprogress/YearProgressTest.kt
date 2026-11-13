package com.tamakitech.yearprogress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class YearProgressTest {
    private fun at(local: String, zone: ZoneId = ZoneOffset.UTC): YearProgress =
        calcYearProgress(Clock.fixed(LocalDateTime.parse(local).atZone(zone).toInstant(), zone))

    @Test fun newYearStartsAtOneHundredPercent() {
        val result = at("2027-01-01T00:00:00")
        assertEquals("100.0", result.percentText)
        assertEquals(365L, result.remainingDays)
    }

    @Test fun lastMinuteRoundsUpToPointOnePercent() {
        val result = at("2026-12-31T23:59:00")
        assertEquals("0.1", result.percentText)
        assertEquals(1L, result.remainingDays)
    }

    @Test fun novemberThirteenthRoundsUpAndHasFortyNineDaysRemaining() {
        val result = at("2026-11-13T00:00:00")
        assertEquals("13.5", result.percentText)
        assertEquals(49L, result.remainingDays)
        assertEquals(49.0 / 365.0, result.remainingRatio, 0.000001)
    }

    @Test fun leapYearDayCountsAndRatioUseThreeHundredSixtySixDays() {
        assertEquals(366L, at("2028-01-01T00:00:00").remainingDays)
        assertEquals(308L, at("2028-02-28T00:00:00").remainingDays)
        assertEquals(307L, at("2028-02-29T00:00:00").remainingDays)
        assertEquals(306L, at("2028-03-01T00:00:00").remainingDays)

        val julySecond = at("2028-07-02T00:00:00")
        val expected = DurationMillis.between(
            Instant.parse("2028-07-02T00:00:00Z"), Instant.parse("2029-01-01T00:00:00Z")
        ).toDouble() / (366.0 * 24 * 60 * 60 * 1000)
        assertEquals(expected, julySecond.remainingRatio, 0.000001)
    }

    @Test fun newYorkDstChangesKeepElapsedTimeRatioMonotonic() {
        val zone = ZoneId.of("America/New_York")
        val totalMillis = 365.0 * 24 * 60 * 60 * 1000
        val instants = listOf(
            "2026-03-08T01:59:59-05:00", "2026-03-08T03:00:00-04:00",
            "2026-11-01T01:59:59-04:00", "2026-11-01T01:00:00-05:00"
        ).map(Instant::parse)
        val ratios = instants.map { instant ->
            calcYearProgress(Clock.fixed(instant, zone)).remainingRatio
        }
        for (i in 0 until ratios.lastIndex) assertTrue(ratios[i] > ratios[i + 1])
        for ((instant, ratio) in instants.zip(ratios)) {
            val start = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone).toInstant()
            val expected = 1.0 - (instant.toEpochMilli() - start.toEpochMilli()) / totalMillis
            assertEquals(expected, ratio, 0.000001)
        }
    }

    @Test fun lowThresholdIsInclusiveAtThirtyPercent() {
        assertFalse(YearProgress(2026, 0.301, 110, 301).isLow)
        assertTrue(YearProgress(2026, 0.300, 110, 300).isLow)
    }

    @Test fun finalMillisecondNeverDisplaysZero() {
        val result = at("2026-12-31T23:59:59.999")
        assertEquals("0.1", result.percentText)
        assertTrue(result.remainingPercentTenths >= 1)
    }

    private object DurationMillis {
        fun between(start: Instant, end: Instant): Long = end.toEpochMilli() - start.toEpochMilli()
    }
}
