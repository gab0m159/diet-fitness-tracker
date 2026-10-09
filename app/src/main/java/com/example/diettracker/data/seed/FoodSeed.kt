package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodDao
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantDao
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility

/**
 * Installs the bundled food library exactly once.
 *
 * The bundled rows are the *lowest* priority tier: user-created foods are never
 * touched, and a bundled row is only inserted if its name is not already taken.
 * That means a user who has already created their own "鸡蛋" keeps theirs.
 *
 * Two shapes of bundled data exist:
 *
 *  - **Per-100g foods** ([BrandFoods] outside McDonald's, and [RecommendedFoods]),
 *    inserted straight from their entities.
 *  - **Per-serving branded foods** (McDonald's), where each product carries one or
 *    more size variants whose published per-serving numbers must be stored
 *    verbatim. Those go through [installMcDonalds].
 */
object FoodSeed {

    /**
     * v6 新增的两批内置数据各自的「哨兵」名字。
     *
     * 用「某个名字还在不在」判断这批数据有没有装过，而不是用「整个分类为不为空」：
     * 品牌分类里早就有麦当劳/肯德基的数据，如果还按分类判空，新加进来的这批永远
     * 装不上。代价是用户如果手动删掉哨兵那一条，这批会在下次启动时补回来。
     */
    private const val SENTINEL_COMMON = "大米"
    private const val SENTINEL_PACKAGED = "康师傅红烧牛肉面"

    /** Inserts whichever bundled categories are still empty. */
    suspend fun install(foodDao: FoodDao, variantDao: FoodVariantDao) {
        // Only seed a category when it has never been seeded, so the user can
        // delete a bundled item without it coming back on next launch.
        if (foodDao.countInCategory(FoodCategory.BRAND.name) == 0) {
            insertMissing(foodDao, BrandFoods.nonMcDonalds())
            installMcDonalds(foodDao, variantDao)
        }
        if (foodDao.countInCategory(FoodCategory.RECOMMENDED.name) == 0) {
            insertMissing(foodDao, RecommendedFoods.all())
        }

        // 常见食物：主食 / 豆类 / 蔬菜 / 甜品（每 100g）。
        if (foodDao.countWithName(SENTINEL_COMMON, 0L) == 0) {
            insertMissing(foodDao, CommonFoods.all())
        }
        // 包装食品与饮料（泡面 / 香肠 / 饮料 / 啤酒，每 100g 或每 100ml）。
        if (foodDao.countWithName(SENTINEL_PACKAGED, 0L) == 0) {
            insertMissing(foodDao, PackagedFoods.all())
        }
        // 星巴克：官方只给每份数值，走按份规格那条路；函数内部逐条自查，可重复调用。
        PackagedFoods.installStarbucks(foodDao, variantDao)
    }

    /**
     * Inserts the official McDonald's China items together with their size
     * variants.
     *
     * Foods are created with `hasVariants = true` and zeroed per-100g columns,
     * because their nutrition lives entirely in `food_variants` as published
     * per-serving values.
     */
    private suspend fun installMcDonalds(foodDao: FoodDao, variantDao: FoodVariantDao) {
        // Seed variants only when none exist yet, so re-running is harmless.
        if (variantDao.count() > 0) return

        McDonaldsChinaFoods.all().forEach { item ->
            // Respect a user-created food of the same name.
            if (foodDao.countWithName(item.name, 0L) > 0) return@forEach

            val foodId = foodDao.insert(
                FoodEntity(
                    name = item.name,
                    // Per-100g columns are unused for variant foods; the entry math
                    // reads the selected variant instead.
                    carbsPer100g = 0.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 0.0,
                    servingSizeGrams = 0.0,
                    note = if (item.variants.size > 1) {
                        "可选规格：${item.variants.joinToString(" / ") { it.spec }}"
                    } else {
                        ""
                    },
                    category = FoodCategory.BRAND.name,
                    brandTag = BrandTags.MCDONALDS,
                    nutritionTags = "",
                    dailyRecommendation = "",
                    dataSource = McDonaldsChinaFoods.SOURCE,
                    credibility = FoodCredibility.OFFICIAL.name,
                    hasVariants = true,
                    sourceNote = McDonaldsChinaFoods.SOURCE_NOTE
                )
            )

            variantDao.insertAll(
                item.variants.mapIndexed { index, variant ->
                    FoodVariantEntity(
                        foodId = foodId,
                        specName = variant.spec,
                        position = index,
                        kilojoules = variant.kilojoules,
                        kcal = variant.kcal,
                        proteinG = variant.proteinG,
                        fatG = variant.fatG,
                        carbsG = variant.carbsG,
                        sodiumMg = variant.sodiumMg,
                        calciumMg = variant.calciumMg
                    )
                }
            )
        }
    }

    /**
     * Inserts [foods], skipping any whose name already exists so a user-created
     * food always wins over the bundled data.
     */
    private suspend fun insertMissing(foodDao: FoodDao, foods: List<FoodEntity>) {
        val toInsert = foods.filter { food ->
            foodDao.countWithName(food.name, 0L) == 0
        }
        if (toInsert.isNotEmpty()) {
            foodDao.insertAllIgnore(toInsert)
        }
    }

    /** True when the library still has no user-created food at all. */
    suspend fun hasNoUserFoods(foodDao: FoodDao): Boolean =
        foodDao.countInCategory(FoodCategory.USER.name) == 0

    /** Every bundled per-100g food, for previews/tests. */
    fun allBundled(): List<FoodEntity> = BrandFoods.nonMcDonalds() + RecommendedFoods.all()

    /** Bundled McDonald's items, for previews/tests. */
    fun bundledMcDonalds() = McDonaldsChinaFoods.all()
}
