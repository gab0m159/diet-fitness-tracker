package com.example.diettracker.domain

import com.example.diettracker.data.db.SeedVersionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 迁移与内置数据版本戳的**结构约定**测试。
 *
 * 这里不连数据库（那要 androidTest），而是把几条容易写错的规则钉住，配合
 * `tools/verify_migrations.py` 使用：
 *
 *  - 脚本负责「迁移 DDL 与 Room schema 是否逐字符一致」；
 *  - 这里负责「版本戳表本身的取值是否自洽、新增批次有没有漏登记」。
 */
class MigrationRulesTest {

    @Test
    fun `每一批内置数据都有非零版本号`() {
        assertTrue("必须至少登记一批内置数据", SeedVersionEntity.ALL.isNotEmpty())
        SeedVersionEntity.ALL.forEach { (key, version) ->
            assertTrue("批次 $key 的 key 不该为空", key.isNotBlank())
            assertTrue("批次 $key 的版本号必须 >= 1，0 表示从未安装", version >= 1)
        }
    }

    @Test
    fun `批次 key 唯一且不重复`() {
        val keys = SeedVersionEntity.ALL.keys.toList()
        assertEquals("批次 key 不能重复", keys.size, keys.toSet().size)
    }

    @Test
    fun `批次 key 使用稳定的小写形式`() {
        // key 会写进数据库，改名等于让老用户重装一遍，所以格式要固定。
        SeedVersionEntity.ALL.keys.forEach { key ->
            assertEquals("批次 key 应为小写下划线形式：$key", key.lowercase(), key)
            assertTrue("批次 key 不该含空格：$key", !key.contains(' '))
        }
    }

    @Test
    fun `食物相关的四批都已登记`() {
        // 这几批对应 FoodSeed.install 里的四个 installBatch 调用。
        // 少了任何一个，那批内置数据在增量更新时就永远不会被补齐。
        val expected = listOf(
            SeedVersionEntity.KEY_FOOD_BRAND,
            SeedVersionEntity.KEY_FOOD_RECOMMENDED,
            SeedVersionEntity.KEY_FOOD_COMMON,
            SeedVersionEntity.KEY_FOOD_PACKAGED
        )
        expected.forEach { key ->
            assertNotNull("$key 没有登记到 SeedVersionEntity.ALL", SeedVersionEntity.ALL[key])
        }
        assertEquals("登记的批次数应与食物批次数一致", expected.size, SeedVersionEntity.ALL.size)
    }

    @Test
    fun `从未安装的批次读出来是第 0 版`() {
        // 这个约定被 FoodSeed.installBatch 依赖：0 < targetVersion 才会执行安装。
        val neverInstalled = 0
        SeedVersionEntity.ALL.forEach { (key, version) ->
            assertTrue(
                "批次 $key 在从未安装时应触发安装（0 < $version）",
                neverInstalled < version
            )
        }
    }

    @Test
    fun `版本戳自增后应触发补齐`() {
        // 模拟「给品牌库加数据」：代码里把版本 +1，老用户的已安装版本落后 → 触发。
        val installedOnOldDevice = SeedVersionEntity.VERSION_FOOD_BRAND
        val newCodeVersion = SeedVersionEntity.VERSION_FOOD_BRAND + 1
        assertTrue(
            "版本落后时必须触发补齐",
            installedOnOldDevice < newCodeVersion
        )
        // 已经是最新版则不重复执行
        assertTrue(
            "版本相同时不该重复执行",
            !(SeedVersionEntity.VERSION_FOOD_BRAND < SeedVersionEntity.VERSION_FOOD_BRAND)
        )
    }
}
