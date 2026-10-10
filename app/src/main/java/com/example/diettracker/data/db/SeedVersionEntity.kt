package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 内置数据的版本戳。
 *
 * ## 为什么需要它
 *
 * 内置内容（食物库等）会随版本增加条目。以前的播种逻辑靠「这个分类是不是空的」
 * 或「某条哨兵数据在不在」来判断要不要装，这在**增量更新**时会失效：
 *
 *  - 分类判空：`BRAND` 里早就有麦当劳的数据，后来新增 20 条品牌食品时分类非空，
 *    那 20 条永远装不上；
 *  - 哨兵判名：用户手动删掉哨兵那一条，整批数据会在下次启动时被重新塞回来。
 *
 * 所以改成给每批内置数据记一个版本号：**当前已安装版本 < 代码里的版本** 时才执行
 * 增量补齐，写完把版本戳更新掉。用户删过的条目不会被重新插回（见 `FoodSeed` 里
 * 的 `insertMissing` 只插不存在的名字）。
 */
@Entity(tableName = "seed_versions")
data class SeedVersionEntity(
    /** 批次标识，例如 `"food_brand"`、`"food_common"`。 */
    @PrimaryKey
    @ColumnInfo(name = "seed_key")
    val seedKey: String,

    /** 已安装到第几版。初次安装写当前版本。 */
    @ColumnInfo(name = "version")
    val version: Int,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        // ---- 各批内置数据的版本号。加了新条目就把这里 +1。---------------

        /** 品牌食品（麦当劳 / 肯德基 / 瑞幸 / Manner / 山姆）。 */
        const val KEY_FOOD_BRAND = "food_brand"
        const val VERSION_FOOD_BRAND = 1

        /** 推荐食品（USDA 等）。 */
        const val KEY_FOOD_RECOMMENDED = "food_recommended"
        const val VERSION_FOOD_RECOMMENDED = 1

        /** 常见食材（主食 / 豆类 / 蔬菜 / 甜品）。 */
        const val KEY_FOOD_COMMON = "food_common"
        const val VERSION_FOOD_COMMON = 1

        /** 包装食品与饮料。 */
        const val KEY_FOOD_PACKAGED = "food_packaged"
        const val VERSION_FOOD_PACKAGED = 1

        /** 全部批次及其当前版本。新增批次记得加进来。 */
        val ALL: Map<String, Int> = mapOf(
            KEY_FOOD_BRAND to VERSION_FOOD_BRAND,
            KEY_FOOD_RECOMMENDED to VERSION_FOOD_RECOMMENDED,
            KEY_FOOD_COMMON to VERSION_FOOD_COMMON,
            KEY_FOOD_PACKAGED to VERSION_FOOD_PACKAGED
        )
    }
}
