package com.example.diettracker.domain

import com.example.diettracker.data.db.ScheduleEntryKind
import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.data.model.TrainingObjective
import com.example.diettracker.data.repository.TodayExerciseCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 训练模块的纯逻辑测试：步进规则与频率排程。
 *
 * 这两块是 v6 的产品规则所在，而且都不碰 Android，所以用普通 JVM 单测把它们钉住：
 * 「成功才加重、失败永不降重、跳过什么都不改」和「几天一次 + 全局延期一天」。
 */
class TrainingRulesTest {

    private val today = "2026-09-27"
    private fun day(offset: Long) = com.example.diettracker.util.DateUtils.plusDays(today, offset)

    private fun split(
        id: Long = 1L,
        position: Int = 0,
        intervalDays: Int = 4,
        nextDueDate: String? = today
    ) = TrainingSplitEntity(
        id = id,
        name = "胸 + 三头",
        bodyPart = "CHEST",
        position = position,
        intervalDays = intervalDays,
        nextDueDate = nextDueDate
    )

    // ------------------------------------------------------------ 步进规则

    @Test
    fun `成功按步进值加重`() {
        val result = ProgressionEngine.onSuccess(currentWeightKg = 60.0, incrementKg = 2.5)
        assertEquals(62.5, result.weightKg, 0.0001)
        assertEquals(ProgressionEngine.Action.ADD_WEIGHT, result.action)
    }

    @Test
    fun `成功后的重量吸附到步进的整数倍`() {
        // 61kg + 2.5kg = 63.5kg，按 2.5 吸附后仍然是 62.5
        val result = ProgressionEngine.onSuccess(currentWeightKg = 61.0, incrementKg = 2.5)
        assertEquals(62.5, result.weightKg, 0.0001)
    }

    @Test
    fun `还没设定重量时成功不凭空发明重量`() {
        val result = ProgressionEngine.onSuccess(currentWeightKg = 0.0, incrementKg = 2.5)
        assertEquals(0.0, result.weightKg, 0.0001)
        assertEquals(ProgressionEngine.Action.BASELINE, result.action)
    }

    @Test
    fun `失败永不降重`() {
        // 旧实现连续 3 次失败会降 5%，新规则必须始终保持不变。
        val result = ProgressionEngine.onFailure(currentWeightKg = 100.0)
        assertEquals(100.0, result.weightKg, 0.0001)
        assertEquals(ProgressionEngine.Action.HOLD, result.action)
    }

    @Test
    fun `跳过不改重量`() {
        val result = ProgressionEngine.onSkip(currentWeightKg = 80.0)
        assertEquals(80.0, result.weightKg, 0.0001)
    }

    @Test
    fun `步进为零时成功也不加重`() {
        val result = ProgressionEngine.onSuccess(currentWeightKg = 60.0, incrementKg = 0.0)
        assertEquals(60.0, result.weightKg, 0.0001)
        assertEquals(ProgressionEngine.Action.HOLD, result.action)
    }

    @Test
    fun `卡片显示本次到下次的两段重量`() {
        assertEquals("本次 60kg → 下次 62.5kg", card(60.0, 2.5).weightProgressLabel)

        // 步进 0（默认）时没有「下次」。
        assertEquals("本次 60kg", card(60.0, 0.0).weightProgressLabel)

        // 目标重量还没填时不显示数字。
        assertEquals("目标重量未设定", card(0.0, 2.5).weightProgressLabel)
    }

    private fun card(weight: Double, increment: Double) = TodayExerciseCard(
        sourceKind = ScheduleEntryKind.TRAINING_DAY,
        sourceId = 1L,
        splitId = 1L,
        splitName = "胸 + 三头",
        exerciseName = "杠铃卧推",
        targetSets = 4,
        targetReps = "8-12",
        targetWeightKg = weight,
        incrementKg = increment
    )

    @Test
    fun `次数写法都能解析`() {
        assertEquals(8..8, ProgressionEngine.repRange("8"))
        assertEquals(8..12, ProgressionEngine.repRange("8-12"))
        assertEquals(10..12, ProgressionEngine.repRange("12,12,10"))
        assertNull(ProgressionEngine.repRange(""))
        assertEquals("8-12 次", ProgressionEngine.repsLabel("8-12"))
        assertEquals("8 次", ProgressionEngine.repsLabel("8"))
    }

    @Test
    fun `默认次数区间跟着训练目标`() {
        // 力量举的默认区间是 3-6（引擎里一贯的取值），不是 `TrainingObjective` 上
        // 写着的 1-6：那 1 次是极限单次，不作为默认训练区间。
        assertEquals("3-6", ProgressionEngine.defaultRepsSpec(TrainingObjective.POWERLIFTING))
        assertEquals("8-12", ProgressionEngine.defaultRepsSpec(TrainingObjective.HYPERTROPHY))
        assertEquals("8-12", ProgressionEngine.defaultRepsSpec(TrainingObjective.PHYSIQUE))
    }

    // ------------------------------------------------------------ 频率排程

    @Test
    fun `从未练过的训练日视为到期`() {
        assertTrue(FrequencyScheduler.isDue(split(nextDueDate = null), today))
    }

    @Test
    fun `到期日已过或就是今天则到期`() {
        assertTrue(FrequencyScheduler.isDue(split(nextDueDate = today), today))
        assertTrue(FrequencyScheduler.isDue(split(nextDueDate = day(-3)), today))
        assertFalse(FrequencyScheduler.isDue(split(nextDueDate = day(1)), today))
    }

    @Test
    fun `到期的训练日按到期日再按顺序排列`() {
        val splits = listOf(
            split(id = 1, position = 0, nextDueDate = day(2)),   // 未到期
            split(id = 2, position = 2, nextDueDate = day(-1)),  // 到期
            split(id = 3, position = 1, nextDueDate = day(-5))   // 到期且更早
        )
        val due = FrequencyScheduler.dueSplits(splits, today)
        assertEquals(listOf(3L, 2L), due.map { it.id })
    }

    @Test
    fun `完成或跳过后到期日推进一个间隔`() {
        val handled = FrequencyScheduler.afterHandled(split(intervalDays = 4), today)
        assertEquals(day(4), handled.nextDueDate)
        assertEquals(today, handled.lastDoneDate)
    }

    @Test
    fun `延期把所有训练日统一顺延一天`() {
        val splits = listOf(
            split(id = 1, nextDueDate = today),    // 今天到期 -> 明天
            split(id = 2, nextDueDate = day(3)),   // 将来 -> 4 天后
            split(id = 3, nextDueDate = day(-9)),  // 逾期很久 -> 只看今天，明天
            split(id = 4, nextDueDate = null)      // 从未练过 -> 明天
        )
        val postponed = FrequencyScheduler.postponeAll(splits, today)
        assertEquals(day(1), postponed[0].nextDueDate)
        assertEquals(day(4), postponed[1].nextDueDate)
        assertEquals(day(1), postponed[2].nextDueDate)
        assertEquals(day(1), postponed[3].nextDueDate)
    }

    @Test
    fun `延期不改动最近完成日期`() {
        val postponed = FrequencyScheduler
            .postponeAll(listOf(split(nextDueDate = today)), today)
            .first()
        assertNull(postponed.lastDoneDate)
    }

    @Test
    fun `新建训练日按顺序错开初始到期日`() {
        assertEquals(today, FrequencyScheduler.initialNextDueDate(today, 0))
        assertEquals(day(1), FrequencyScheduler.initialNextDueDate(today, 1))
        assertEquals(day(2), FrequencyScheduler.initialNextDueDate(today, 2))
    }

    @Test
    fun `间隔被夹在合法范围内`() {
        assertEquals(1, TrainingSplitEntity.sanitizeInterval(0))
        assertEquals(30, TrainingSplitEntity.sanitizeInterval(99))
        assertEquals(5, TrainingSplitEntity.sanitizeInterval(5))
    }
}
