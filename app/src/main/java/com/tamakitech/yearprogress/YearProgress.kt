package com.tamakitech.yearprogress

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 今年の残り。
 *
 * @property remainingRatio 残り割合（0.0〜1.0、実時間比）
 * @property remainingDays 今日を含む残り日数（12/31 → 1）
 * @property remainingPercentTenths 表示用の残り% × 10（小数第1位で切り上げ、最小 1 = 0.1%）
 */
data class YearProgress(
    val year: Int,
    val remainingRatio: Double,
    val remainingDays: Long,
    val remainingPercentTenths: Int,
) {
    /** 表示値（"13.5" など）。ロケールに依存しない。 */
    val percentText: String
        get() = "${remainingPercentTenths / 10}.${remainingPercentTenths % 10}"

    /** バーを赤にするか。表示値 30.0% 以下で true（30.0% ちょうどは赤）。 */
    val isLow: Boolean
        get() = remainingPercentTenths <= LOW_THRESHOLD_TENTHS

    companion object {
        const val LOW_THRESHOLD_TENTHS = 300
    }
}

fun calcYearProgress(clock: Clock = Clock.systemDefaultZone()): YearProgress {
    val zone = clock.zone
    val now = ZonedDateTime.now(clock)
    val start = LocalDate.of(now.year, 1, 1).atStartOfDay(zone)
    val end = start.plusYears(1)
    val totalMillis = Duration.between(start, end).toMillis()
    val remainingMillis = Duration.between(now, end).toMillis()
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate())
    // 浮動小数の誤差で切り上げがずれないよう整数演算で ceil(remaining / total × 1000)
    val tenths = Math.floorDiv(remainingMillis * 1000 + totalMillis - 1, totalMillis)
        .toInt()
        .coerceIn(1, 1000)
    return YearProgress(
        year = now.year,
        remainingRatio = remainingMillis.toDouble() / totalMillis,
        remainingDays = days,
        remainingPercentTenths = tenths,
    )
}
