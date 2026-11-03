package com.trenits.fivestar.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatisticsTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 3)

    @Test
    fun weekHasSevenDaysEndingToday() {
        val days = statsDays(StatsRange.WEEK, today, null)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 10, 28), days.first())
        assertEquals(today, days.last())
    }

    @Test
    fun monthHasThirtyDays() {
        assertEquals(30, statsDays(StatsRange.MONTH, today, null).size)
    }

    @Test
    fun threeMonthsAreWeeklyBuckets() {
        val days = statsDays(StatsRange.THREE_MONTHS, today, null)
        assertEquals(90, days.size)
        val points = statsPoints(StatsRange.THREE_MONTHS, days, List(days.size) { 50.0 })
        assertEquals(13, points.size)
    }

    @Test
    fun allTimeStartsAtAccountStartEvenWithoutData() {
        val account = LocalDate.of(2026, 10, 1)
        val days = statsDays(StatsRange.ALL, today, null, account)
        assertEquals(account, days.first())
        assertEquals(34, days.size)
    }

    @Test
    fun allTimeUsesEarlierOfAccountAndFirstRecord() {
        val account = LocalDate.of(2026, 10, 10)
        val firstRecord = LocalDate.of(2026, 10, 1)
        assertEquals(firstRecord, statsDays(StatsRange.ALL, today, firstRecord, account).first())
    }

    @Test
    fun weeklyBucketAveragesValues() {
        val days = statsDays(StatsRange.THREE_MONTHS, today, null)
        val values = days.indices.map { if (it < 7) 100.0 else 0.0 }
        val points = statsPoints(StatsRange.THREE_MONTHS, days, values)
        assertEquals(100.0, points.first().value, 1e-9)
        assertEquals(0.0, points[1].value, 1e-9)
    }

    @Test
    fun missingDayWithoutRecordCountsAsZero() {
        val category = Category(id = "preset_water_u", ownerUid = "u", name = "Water", weight = 3, kind = CategoryKind.WATER)
        assertEquals(0.0, categoryPercentOf(null, category), 1e-9)
        val record = DayRecord(
            dayKey = "2026-11-03",
            ownerUid = "u",
            snapshots = listOf(CategorySnapshot("preset_water_u", "Water", 3, 4)),
        )
        assertEquals(80.0, categoryPercentOf(record, category), 1e-9)
    }
}
