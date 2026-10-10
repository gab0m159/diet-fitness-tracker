package com.example.diettracker.domain

import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 营养目标算法的测试。
 *
 * 重点是把 v7 的新规则钉死：**碳蛋脂按体重给，热量目标由三者反算**，
 * 而不是老的「TDEE × 倍率 → 按百分比切」。
 */
class NutritionTargetsTest {

    private fun estimate(
        weightKg: Double = 70.0,
        goal: GoalMode = GoalMode.MAINTAIN,
        activity: ActivityLevel = ActivityLevel.MODERATE
    ) = NutritionCalculator.estimate(
        weightKg = weightKg,
        heightCm = 175.0,
        age = 25,
        sex = Sex.MALE,
        activity = activity,
        goal = goal,
        bodyFatPercent = null
    )

    @Test
    fun `每公斤克数按模式区分`() {
        val bulk = NutritionCalculator.perKgTargets(GoalMode.BULK)
        assertEquals(4.0, bulk.carbsPerKg, 0.001)
        assertEquals(1.8, bulk.proteinPerKg, 0.001)
        assertEquals(1.0, bulk.fatPerKg, 0.001)

        val maintain = NutritionCalculator.perKgTargets(GoalMode.MAINTAIN)
        assertEquals(3.5, maintain.carbsPerKg, 0.001)
        assertEquals(1.6, maintain.proteinPerKg, 0.001)
        assertEquals(0.9, maintain.fatPerKg, 0.001)

        val cut = NutritionCalculator.perKgTargets(GoalMode.CUT)
        assertEquals(2.5, cut.carbsPerKg, 0.001)
        // 减脂期蛋白高于增肌（热量缺口下保肌肉），但不进入 2.3+ 的备赛区间
        assertEquals(2.0, cut.proteinPerKg, 0.001)
        assertEquals(0.8, cut.fatPerKg, 0.001)
    }

    @Test
    fun `宏量按体重直接相乘`() {
        val targets = NutritionCalculator.macroTargets(70.0, GoalMode.MAINTAIN)
        assertEquals(245.0, targets.carbs, 0.001)
        assertEquals(112.0, targets.protein, 0.001)
        assertEquals(63.0, targets.fat, 0.001)
    }

    @Test
    fun `体重为零时宏量全为零`() {
        val targets = NutritionCalculator.macroTargets(0.0, GoalMode.BULK)
        assertEquals(0.0, targets.carbs, 0.001)
        assertEquals(0.0, targets.protein, 0.001)
        assertEquals(0.0, targets.fat, 0.001)
    }

    @Test
    fun `目标热量等于碳蛋脂折算之和`() {
        val targets = NutritionCalculator.macroTargets(70.0, GoalMode.MAINTAIN)
        val expected = 245.0 * 4 + 112.0 * 4 + 63.0 * 9
        assertEquals(expected, NutritionCalculator.targetCaloriesFromMacros(targets), 0.001)
        // 保持模式 70kg → 980 + 448 + 567 = 1995 kcal
        assertEquals(1995.0, NutritionCalculator.targetCaloriesFromMacros(targets), 0.001)
    }

    @Test
    fun `估算结果里目标热量来自宏量而不是 TDEE 乘倍率`() {
        val e = estimate()
        val fromMacros = NutritionCalculator.targetCaloriesFromMacros(e.targets)
        assertEquals(fromMacros, e.targetCalories, 0.001)
        // 与 TDEE × 倍率不同：保持模式 TDEE 2594，但目标热量是 1995
        assertTrue(
            "目标热量应低于 TDEE×倍率，实际 target=${e.targetCalories} tdee=${e.tdee}",
            e.targetCalories < e.tdee
        )
    }

    @Test
    fun `减脂模式蛋白高于增肌模式`() {
        val bulkProtein = NutritionCalculator.macroTargets(70.0, GoalMode.BULK).protein
        val cutProtein = NutritionCalculator.macroTargets(70.0, GoalMode.CUT).protein
        assertTrue("减脂期蛋白应更高", cutProtein > bulkProtein)
    }

    @Test
    fun `增肌模式热量目标最高`() {
        val bulk = NutritionCalculator.targetCaloriesFromMacros(
            NutritionCalculator.macroTargets(70.0, GoalMode.BULK)
        )
        val maintain = NutritionCalculator.targetCaloriesFromMacros(
            NutritionCalculator.macroTargets(70.0, GoalMode.MAINTAIN)
        )
        val cut = NutritionCalculator.targetCaloriesFromMacros(
            NutritionCalculator.macroTargets(70.0, GoalMode.CUT)
        )
        assertTrue(bulk > maintain)
        assertTrue(maintain > cut)
    }

    @Test
    fun `70 公斤保持模式的三个数字`() {
        // 245 / 112 / 63 g → 1995 kcal（对比旧算法的 324 / 162 / 72）
        val e = estimate()
        assertEquals(245.0, e.targets.carbs, 0.001)
        assertEquals(112.0, e.targets.protein, 0.001)
        assertEquals(63.0, e.targets.fat, 0.001)
        assertEquals(1995.0, e.targetCalories, 0.001)
    }

    @Test
    fun `蛋白质摄入量落在 ISSN 建议区间`() {
        // ISSN 2017：增肌/维持 1.4-2.0 g/kg 足够；减脂期 2.0-2.3 之间是务实值
        // （2.3-3.1 是备赛运动员的上限区间，对普通用户不现实）
        listOf(GoalMode.BULK, GoalMode.MAINTAIN, GoalMode.CUT).forEach { goal ->
            val perKg = NutritionCalculator.perKgTargets(goal).proteinPerKg
            assertTrue("$goal 的蛋白 g/kg 应在 1.4-2.3", perKg in 1.4..2.3)
        }
    }

    @Test
    fun `脂肪摄入量落在通行建议区间`() {
        // 0.8 - 1.0 g/kg（保证激素水平的最低需要）
        listOf(GoalMode.BULK, GoalMode.MAINTAIN, GoalMode.CUT).forEach { goal ->
            val perKg = NutritionCalculator.perKgTargets(goal).fatPerKg
            assertTrue("$goal 的脂肪 g/kg 应在 0.8-1.0", perKg in 0.8..1.0)
        }
    }

    @Test
    fun `BMR 与 TDEE 链路未受影响`() {
        val e = estimate()
        // Mifflin 男：10×70 + 6.25×175 − 5×25 + 5 = 1673.75
        assertEquals(1673.75, e.bmrMifflin, 0.01)
        // 中度 ×1.55
        assertEquals(1673.75 * 1.55, e.tdee, 0.01)
    }

    @Test
    fun `体脂率影响 Katch 参考值但不影响目标`() {
        val without = estimate()
        val with = NutritionCalculator.estimate(
            weightKg = 70.0,
            heightCm = 175.0,
            age = 25,
            sex = Sex.MALE,
            activity = ActivityLevel.MODERATE,
            goal = GoalMode.MAINTAIN,
            bodyFatPercent = 18.5
        )
        assertEquals(null, without.bmrKatch)
        assertTrue("填了体脂率应有 Katch 参考值", with.bmrKatch != null)
        // 目标宏量与热量不受体脂率影响
        assertEquals(without.targets.carbs, with.targets.carbs, 0.001)
        assertEquals(without.targetCalories, with.targetCalories, 0.001)
    }
}
