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
    fun weeklySumsAddUpCounts() {
        val days = statsDays(StatsRange.THREE_MONTHS, today, null)
        val counts = days.indices.map { if (it < 7) 2.0 else 0.0 }
        val sums = statsSums(StatsRange.THREE_MONTHS, days, counts)
        assertEquals(14.0, sums.first().value, 1e-9)
        assertEquals(0.0, sums[1].value, 1e-9)
    }

    @Test
    fun missingRelationshipRatingCountsAsZero() {
        assertEquals(0.0, relationshipPercentOf(null), 1e-9)
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

    @Test
    fun weightedStarsMultiplyWeightByStars() {
        val record = DayRecord(
            dayKey = "2026-11-03",
            ownerUid = "u",
            snapshots = listOf(
                CategorySnapshot("a", "A", 3, 4),
                CategorySnapshot("b", "B", 2, 5),
            ),
        )
        assertEquals(22, weightedStarsOf(record, null))
        assertEquals(0, weightedStarsOf(null, null))
    }

    @Test
    fun relationshipAspectPercentIsStarsOfFive() {
        val record = DayRecord(
            dayKey = "2026-11-03",
            ownerUid = "u",
            snapshots = emptyList(),
            counters = mapOf(RELATIONSHIP_COUNTER_KEY to mapOf(RelationshipAspect.SEX.key to 4.0)),
        )
        assertEquals(80.0, relationshipAspectPercentOf(record, RelationshipAspect.SEX), 1e-9)
        assertEquals(0.0, relationshipAspectPercentOf(record, RelationshipAspect.INTIMACY), 1e-9)
    }

    @Test
    fun taskTimersStartAtSelection() {
        val created = java.time.Instant.parse("2026-11-01T10:00:00Z")
        val selected = java.time.Instant.parse("2026-11-03T10:00:00Z")
        val task = Task(
            id = "t", ownerUid = "u", type = TaskType.REWARD, title = "Rewardtitle",
            dayKey = "2026-11-01", status = TaskStatus.OPEN, createdAt = created, selectedAt = selected,
        )
        // 48 h nach Auswahl, nicht nach Erstellung
        assertEquals(TaskStatus.OPEN, task.withAutoConfirmation(selected.plusSeconds(47 * 3600)).status)
        assertEquals(TaskStatus.AUTO_CONFIRMED, task.withAutoConfirmation(selected.plusSeconds(48 * 3600)).status)
        // 3 h nach Auswahl erinnern
        assertEquals(false, task.needsPartnerConfirmationReminder(selected.plusSeconds(2 * 3600)))
        assertEquals(true, task.needsPartnerConfirmationReminder(selected.plusSeconds(3 * 3600)))
    }

    @Test
    fun notYetPostponesDeadlineByTwentyFourHours() {
        val selected = java.time.Instant.parse("2026-11-03T10:00:00Z")
        val task = Task(
            id = "t", ownerUid = "u", type = TaskType.REWARD, title = "Rewardtitle",
            dayKey = "2026-11-03", status = TaskStatus.OPEN, createdAt = selected, selectedAt = selected,
        )
        val now = selected.plusSeconds(40 * 3600)
        val once = task.postponed(now)
        assertEquals(selected.plusSeconds(72 * 3600), once.autoConfirmAt)
        assertEquals(1, once.postponeCount)
        val twice = once.postponed(now)
        assertEquals(selected.plusSeconds(96 * 3600), twice.autoConfirmAt)
        assertEquals(2, twice.postponeCount)
        assertEquals(TaskStatus.OPEN, twice.withAutoConfirmation(selected.plusSeconds(95 * 3600)).status)
    }

    @Test
    fun deadlineWarningComesSixHoursBeforeAutoConfirm() {
        val selected = java.time.Instant.parse("2026-11-03T10:00:00Z")
        val task = Task(
            id = "t", ownerUid = "u", type = TaskType.REWARD, title = "Rewardtitle",
            dayKey = "2026-11-03", status = TaskStatus.OPEN, createdAt = selected, selectedAt = selected,
        )
        assertEquals(false, task.needsDeadlineWarning(selected.plusSeconds(41 * 3600)))
        assertEquals(true, task.needsDeadlineWarning(selected.plusSeconds(42 * 3600)))
    }

    @Test
    fun bedtimeStarsDecreaseAfterMidnight() {
        assertEquals(5, bedtimeStars(23, 30))
        assertEquals(4, bedtimeStars(0, 30))
        assertEquals(3, bedtimeStars(1, 30))
        assertEquals(2, bedtimeStars(2, 30))
        assertEquals(0, bedtimeStars(3, 0))
    }

    @Test
    fun rewardTiersFollowStreakMilestones() {
        assertEquals(3, rewardTierForStreak(3))
        assertEquals(2, rewardTierForStreak(5))
        assertEquals(1, rewardTierForStreak(7))
        assertEquals(null, rewardTierForStreak(4))
    }

    @Test
    fun streakRestartsAfterBaseline() {
        val cats = listOf(Category(id = "c", ownerUid = "u", name = "C", weight = 1, kind = CategoryKind.STARS, targets = listOf("u")))
        val records = (1..5).associate { d ->
            val key = today.minusDays(d.toLong()).toString()
            key to DayRecord(dayKey = key, ownerUid = "u", snapshots = listOf(CategorySnapshot("c", "C", 1, 5)))
        }
        assertEquals(5, consecutiveSuccessDays(today.minusDays(1), records, cats))
        assertEquals(2, consecutiveSuccessDays(today.minusDays(1), records, cats, notBefore = today.minusDays(3)))
    }
}
