package com.example.diettracker.data.seed

import com.example.diettracker.data.db.FoodDao
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantDao
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.db.SeedVersionDao
import com.example.diettracker.data.db.SeedVersionEntity
import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility

/**
 * 安装与**增量补齐**内置食物库。
 *
 * 内置行是最低优先级的一层：用户自建的食物永远不动，同名时内置那条不会插入。
 * 所以一个已经自己建过「鸡蛋」的用户会保留自己的版本。
 *
 * 内置数据有两种形态：
 *
 *  - **每 100g 的食物**（麦当劳之外的 [BrandFoods]、[RecommendedFoods]、
 *    [CommonFoods]、[PackagedFoods]）——直接按实体插入；
 *  - **按份计量的品牌食物**（麦当劳、星巴克）——每个商品带一个或多个规格变体，
 *    官方公布的每份数值必须原样存下来，走 `installMcDonalds` / `installStarbucks`。
 *
 * 增量更新靠 [SeedVersionEntity] 的版本戳驱动，见 [install]。
 */
object FoodSeed {

    /**
     * 安装 / 补齐全套内置食物数据。
     *
     * ## 增量更新怎么做
     *
     * 每批数据在 [SeedVersionEntity] 里有一个版本号。这里的逻辑是：
     *
     *  - 从没装过（库里没有版本戳）→ 整批装入，然后写上当前版本；
     *  - 装过但版本落后（代码里升了版本）→ **只补库里没有的名字**，再更新版本戳。
     *
     * 因此「给品牌库加了 20 条」只需要把 `VERSION_FOOD_BRAND` +1，老用户升级时会
     * 自动补上这 20 条，而他们自己改过、删过的条目不受影响（[insertMissing] 只插
     * 不存在的名字，且尊重同名用户食物）。
     *
     * [freshInstall] 为真时（刚建库）忽略版本戳，整批装入——避免首次启动时因为
     * 版本戳表还是空的而漏装。
     */
    suspend fun install(
        foodDao: FoodDao,
        variantDao: FoodVariantDao,
        seedVersionDao: SeedVersionDao,
        freshInstall: Boolean = false
    ) {
        // ---- 品牌食品（含麦当劳的规格变体）--------------------------------
        installBatch(
            dao = seedVersionDao,
            key = SeedVersionEntity.KEY_FOOD_BRAND,
            targetVersion = SeedVersionEntity.VERSION_FOOD_BRAND,
            freshInstall = freshInstall
        ) {
            insertMissing(foodDao, BrandFoods.nonMcDonalds())
            installMcDonalds(foodDao, variantDao)
        }

        // ---- 推荐食品 ------------------------------------------------------
        installBatch(
            dao = seedVersionDao,
            key = SeedVersionEntity.KEY_FOOD_RECOMMENDED,
            targetVersion = SeedVersionEntity.VERSION_FOOD_RECOMMENDED,
            freshInstall = freshInstall
        ) {
            insertMissing(foodDao, RecommendedFoods.all())
        }

        // ---- 常见食材（主食 / 豆类 / 蔬菜 / 甜品）-------------------------
        installBatch(
            dao = seedVersionDao,
            key = SeedVersionEntity.KEY_FOOD_COMMON,
            targetVersion = SeedVersionEntity.VERSION_FOOD_COMMON,
            freshInstall = freshInstall
        ) {
            insertMissing(foodDao, CommonFoods.all())
        }

        // ---- 包装食品与饮料 ------------------------------------------------
        installBatch(
            dao = seedVersionDao,
            key = SeedVersionEntity.KEY_FOOD_PACKAGED,
            targetVersion = SeedVersionEntity.VERSION_FOOD_PACKAGED,
            freshInstall = freshInstall
        ) {
            insertMissing(foodDao, PackagedFoods.all())
            // 星巴克：官方只给每份数值，走按份规格；函数内部逐条自查，可重复调用。
            PackagedFoods.installStarbucks(foodDao, variantDao)
        }
    }

    /**
     * 跑一批内置数据的增量补齐。
     *
     * 只有「从没装过」或「版本落后」才会执行 [block]；执行完把版本戳更新到
     * [targetVersion]。
     */
    private suspend fun installBatch(
        dao: SeedVersionDao,
        key: String,
        targetVersion: Int,
        freshInstall: Boolean,
        block: suspend () -> Unit
    ) {
        val installed = if (freshInstall) 0 else dao.installedVersion(key)
        if (installed >= targetVersion) return

        block()

        dao.upsert(
            SeedVersionEntity(seedKey = key, version = targetVersion)
        )
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
