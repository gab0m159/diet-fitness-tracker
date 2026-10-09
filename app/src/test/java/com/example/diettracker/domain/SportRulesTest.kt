package com.example.diettracker.domain

import com.example.diettracker.data.db.ActivityLogEntity
import com.example.diettracker.data.db.ExerciseLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 运动模块的纯逻辑测试。
 *
 * v7 把训练计划（训练日 / 频率 / 步进 / 成功失败）整套换成了「运动项目 + 时长 +
 * MET」，所以这里测的是新规则：MET 热量计算、运动库完整性、以及动作卡片的
 * `重量 × 组数 × 次数`。
 */
class SportRulesTest {

    // ------------------------------------------------------- MET 热量计算

    @Test
    fun `MET 热量按体重与时长换算`() {
        // 跑步 MET 8.3，70kg，30 分钟 → 8.3 × 70 × 0.5 = 290.5
        val kcal = SportLibrary.estimateKcal(met = 8.3, bodyWeightKg = 70.0, minutes = 30)
        assertEquals(290.5, kcal, 0.01)
    }

    @Test
    fun `体重大的人同样时长消耗更多`() {
        val light = SportLibrary.estimateKcal(6.0, 55.0, 60)
        val heavy = SportLibrary.estimateKcal(6.0, 85.0, 60)
        assertTrue("体重更大应消耗更多", heavy > light)
        assertEquals(330.0, light, 0.01)
        assertEquals(510.0, heavy, 0.01)
    }

    @Test
    fun `时长为零或参数非法时返回零`() {
        assertEquals(0.0, SportLibrary.estimateKcal(8.0, 70.0, 0), 0.001)
        assertEquals(0.0, SportLibrary.estimateKcal(0.0, 70.0, 30), 0.001)
        assertEquals(0.0, SportLibrary.estimateKcal(8.0, 0.0, 30), 0.001)
    }

    @Test
    fun `运动项目自己的估算方法同样成立`() {
        val activity = ActivityLogEntity(
            date = "2026-10-09",
            sportKey = "RUNNING",
            sportName = "跑步",
            met = 8.3,
            durationMinutes = 30
        )
        assertEquals(290.5, activity.estimateBurn(70.0), 0.01)
    }

    // --------------------------------------------------------- 运动库

    @Test
    fun `运动库共 33 项且 key 唯一`() {
        // 有氧 13 + 球类 6 + 操课 6 + 力量与其它 8
        assertEquals(33, SportLibrary.all.size)
        val keys = SportLibrary.all.map { it.key }
        assertEquals("key 不应重复", keys.size, keys.distinct().size)
    }

    @Test
    fun `每个分组都非空`() {
        SportLibrary.grouped().forEach { (category, list) ->
            assertTrue("${category.label} 不应为空", list.isNotEmpty())
        }
        assertEquals(13, SportLibrary.all.count { it.category == SportCategory.CARDIO })
        assertEquals(6, SportLibrary.all.count { it.category == SportCategory.BALL })
        assertEquals(6, SportLibrary.all.count { it.category == SportCategory.CLASS })
        assertEquals(8, SportLibrary.all.count { it.category == SportCategory.STRENGTH })
    }

    @Test
    fun `每项运动的 MET 与默认时长都是正数`() {
        SportLibrary.all.forEach { sport ->
            assertTrue("${sport.name} 的 MET 应为正", sport.met > 0.0)
            assertTrue("${sport.name} 的默认时长应为正", sport.defaultMinutes > 0)
            assertTrue("${sport.name} 应有名字", sport.name.isNotBlank())
        }
    }

    @Test
    fun `撸铁存在于库里且被标记为力量项目`() {
        val strength = SportLibrary.find(SportLibrary.STRENGTH_KEY)
        assertNotNull("撸铁必须存在", strength)
        assertTrue("撸铁应标记为可加动作", strength!!.strength)
        assertEquals("撸铁", strength.name)
    }

    @Test
    fun `只有撸铁是可加动作的项目`() {
        val strengthItems = SportLibrary.all.filter { it.strength }
        assertEquals(1, strengthItems.size)
        assertEquals(SportLibrary.STRENGTH_KEY, strengthItems.first().key)
    }

    @Test
    fun `分组覆盖全部项目且不重不漏`() {
        val grouped = SportLibrary.grouped().flatMap { it.second }
        assertEquals(SportLibrary.all.size, grouped.size)
        assertEquals(
            SportLibrary.all.map { it.key }.sorted(),
            grouped.map { it.key }.sorted()
        )
    }

    @Test
    fun `跑步的 MET 取自通行对照表`() {
        assertEquals(8.3, SportLibrary.find("RUNNING")!!.met, 0.001)
        assertEquals(12.3, SportLibrary.find("JUMP_ROPE")!!.met, 0.001)
        assertEquals(2.3, SportLibrary.find("STRETCHING")!!.met, 0.001)
    }

    // ------------------------------------------------- 撸铁动作卡片

    @Test
    fun `动作卡片显示为 重量 组数 次数`() {
        val exercise = ExerciseLogEntity(
            activityId = 1L,
            date = "2026-10-09",
            exerciseName = "杠铃卧推",
            weightKg = 60.0,
            sets = 4,
            reps = 8
        )
        assertEquals("60kg × 4 组 × 8 次", exercise.summary)
        // 容量 = 60 × 4 × 8
        assertEquals(1920.0, exercise.volume, 0.001)
    }

    @Test
    fun `默认值就是三组十次`() {
        val exercise = ExerciseLogEntity(
            activityId = 1L,
            date = "2026-10-09",
            exerciseName = "深蹲"
        )
        assertEquals(3, exercise.sets)
        assertEquals(10, exercise.reps)
        assertEquals(0.0, exercise.weightKg, 0.001)
    }

    @Test
    fun `撸铁项本身也按 MET 算热量`() {
        val strength = SportLibrary.strength
        assertEquals(6.0, strength.met, 0.001)
        // 70kg 练 60 分钟 → 6.0 × 70 × 1 = 420
        assertEquals(420.0, SportLibrary.estimateKcal(strength.met, 70.0, 60), 0.01)
    }

    @Test
    fun `活动记录能区分是不是撸铁`() {
        val cardio = ActivityLogEntity(
            date = "2026-10-09",
            sportKey = "RUNNING",
            sportName = "跑步",
            met = 8.3,
            durationMinutes = 30
        )
        val gym = cardio.copy(sportKey = SportLibrary.STRENGTH_KEY, sportName = "撸铁")
        assertFalse(cardio.isStrength)
        assertTrue(gym.isStrength)
    }

    @Test
    fun `重量转换不会出现浮点尾巴`() {
        val exercise = ExerciseLogEntity(
            activityId = 1L,
            date = "2026-10-09",
            exerciseName = "卧推",
            weightKg = 62.5,
            sets = 3,
            reps = 10
        )
        assertEquals("62.5kg × 3 组 × 10 次", exercise.summary)
    }

    // ------------------------------------------------------------ 其它

    @Test
    fun `找不到的运动 key 返回 null`() {
        assertNull(SportLibrary.find("NOT_A_SPORT"))
    }
}
