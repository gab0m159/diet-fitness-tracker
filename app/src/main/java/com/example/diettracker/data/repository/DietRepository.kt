package com.example.diettracker.data.repository

import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.db.DietEntryEntity
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.db.MacroGoalEntity
import com.example.diettracker.data.model.AmountMode
import com.example.diettracker.data.model.DiaryEntry
import com.example.diettracker.data.model.Macros
import com.example.diettracker.data.model.MealType
import com.example.diettracker.data.model.toDiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for the UI layer. It hides Room behind:
 *  - domain models (`DiaryEntry`, `Macros`) instead of rows,
 *  - suspend functions instead of DAO calls,
 *  - `Flow`s that already emit the shape the screens need.
 *
 * Every write returns [Result] so the ViewModels can surface errors without
 * crashing on a constraint violation.
 */
class DietRepository(private val db: AppDatabase) {

    private val foodDao = db.foodDao()
    private val variantDao = db.foodVariantDao()
    private val entryDao = db.dietEntryDao()
    private val goalDao = db.macroGoalDao()

    // ------------------------------------------------------- food variants

    /**
     * Sizes / specs of a branded food, e.g. 薯条 迷你/小/中/大.
     *
     * Empty for every food that stores per-100g nutrition instead.
     */
    fun observeVariants(foodId: Long): Flow<List<FoodVariantEntity>> =
        variantDao.observeForFood(foodId)

    suspend fun getVariants(foodId: Long): List<FoodVariantEntity> =
        variantDao.getForFood(foodId)

    /** All variants keyed by food id, for list rendering without N+1 queries. */
    fun observeAllVariants(): Flow<Map<Long, List<FoodVariantEntity>>> =
        variantDao.observeAll().map { list -> list.groupBy { it.foodId } }

    suspend fun getAllVariants(): Map<Long, List<FoodVariantEntity>> =
        variantDao.getAll().groupBy { it.foodId }

    // ---------------------------------------------------------------- foods

    fun observeFoods(): Flow<List<FoodEntity>> = foodDao.observeAll()

    fun searchFoods(query: String): Flow<List<FoodEntity>> =
        if (query.isBlank()) foodDao.observeAll() else foodDao.search(query.trim())

    fun observeFood(id: Long): Flow<FoodEntity?> = foodDao.observeById(id)

    suspend fun getFood(id: Long): FoodEntity? = foodDao.getById(id)

    fun observeByCategory(category: String): Flow<List<FoodEntity>> =
        foodDao.observeByCategory(category)

    fun observeByBrand(brand: String): Flow<List<FoodEntity>> =
        foodDao.observeByBrand(brand)

    fun observeByNutritionTag(tag: String): Flow<List<FoodEntity>> =
        foodDao.observeByNutritionTag(tag)

    fun observeBrands(): Flow<List<String>> = foodDao.observeBrands()

    suspend fun isFoodNameTaken(name: String, excludeId: Long = 0L): Boolean =
        foodDao.countWithName(name.trim(), excludeId) > 0

    suspend fun addFood(food: FoodEntity): Result<Long> = runCatching {
        foodDao.insert(food.copy(id = 0L))
    }

    suspend fun updateFood(food: FoodEntity): Result<Unit> = runCatching {
        foodDao.update(food)
    }

    suspend fun deleteFood(food: FoodEntity): Result<Unit> = runCatching {
        foodDao.delete(food)
    }

    /** How many diary entries reference this food (deleting cascades them away). */
    suspend fun entriesUsingFood(foodId: Long): Int = entryDao.countByFood(foodId)

    // --------------------------------------------------------------- diary

    /** The day's diary, oldest first, with macros already computed. */
    fun observeDiary(date: String): Flow<List<DiaryEntry>> =
        entryDao.observeByDate(date)
            .map { rows -> rows.map { it.toDiaryEntry() } }

    /** Just the day's macro totals, for the summary card. */
    fun observeDayTotals(date: String): Flow<DayTotals> =
        entryDao.observeByDate(date).map { rows ->
            DayTotals(
                macros = rows.fold(Macros.ZERO) { acc, row ->
                    acc + Macros(row.carbsGrams, row.proteinGrams, row.fatGrams)
                },
                entryCount = rows.size
            )
        }

    suspend fun getEntry(id: Long): DietEntryEntity? = entryDao.getById(id)

    /**
     * Adds an entry.
     *
     * Two paths, decided by the food:
     *
     *  - **Variant food** (`food.hasVariants`, e.g. McDonald's): [amount] is a
     *    serving count and [variantId] selects the size. The published
     *    per-serving numbers are snapshotted as-is, so the entry matches the
     *    official figures exactly.
     *  - **Per-100g food**: [mode] decides whether [amount] is grams or servings,
     *    converted through the food's `servingSizeGrams`.
     *
     * @return [Result] failing with [IllegalArgumentException] on bad input and
     *         [NoSuchElementException] when the food or variant no longer exists.
     */
    suspend fun addEntry(
        date: String,
        foodId: Long,
        amount: Double,
        mode: AmountMode,
        mealType: MealType,
        variantId: Long? = null
    ): Result<Long> = runCatching {
        require(amount > 0.0) { "数量必须大于 0" }
        val food = foodDao.getById(foodId) ?: error("食物不存在，可能已被删除")

        if (food.hasVariants) {
            val variant = resolveVariant(food, variantId)
            return@runCatching entryDao.insert(
                DietEntryEntity(
                    date = date,
                    foodId = food.id,
                    foodName = food.name,
                    // Variant foods have no published weight, so grams is recorded
                    // as 0 and the serving count carries the amount.
                    grams = 0.0,
                    amountMode = AmountMode.SERVING.name,
                    servings = amount,
                    servingSizeGrams = 0.0,
                    carbsPer100g = 0.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 0.0,
                    mealType = mealType.name,
                    variantSpec = variant.specName,
                    variantKcalPerServing = variant.kcal,
                    variantCarbsPerServing = variant.carbsG,
                    variantProteinPerServing = variant.proteinG,
                    variantFatPerServing = variant.fatG,
                    variantSodiumPerServing = variant.sodiumMg,
                    variantCalciumPerServing = variant.calciumMg
                )
            )
        }

        require(food.servingSizeGrams > 0.0) { "该食物的一份大小无效" }
        val grams = when (mode) {
            AmountMode.GRAMS -> amount
            AmountMode.SERVING -> amount * food.servingSizeGrams
        }
        entryDao.insert(
            DietEntryEntity(
                date = date,
                foodId = food.id,
                foodName = food.name,
                grams = grams,
                amountMode = mode.name,
                servings = if (mode == AmountMode.SERVING) amount else 0.0,
                servingSizeGrams = food.servingSizeGrams,
                carbsPer100g = food.carbsPer100g,
                proteinPer100g = food.proteinPer100g,
                fatPer100g = food.fatPer100g,
                mealType = mealType.name
            )
        )
    }

    /**
     * Picks the variant to log: the requested one, the food's first variant, or a
     * clear error when the food has none.
     */
    private suspend fun resolveVariant(
        food: FoodEntity,
        variantId: Long?
    ): FoodVariantEntity {
        val variants = variantDao.getForFood(food.id)
        if (variants.isEmpty()) {
            error("「${food.name}」缺少规格数据，请重新安装食物库")
        }
        if (variantId == null) return variants.first()
        return variants.firstOrNull { it.id == variantId }
            ?: variants.first()
    }

    /**
     * Rewrites an entry's amount, meal and (for variant foods) size.
     *
     * Variant entries keep their snapshotted per-serving values and only change
     * the serving count or the selected size; per-100g entries are re-snapshotted
     * from the current food record, since the user is explicitly re-choosing the
     * amount.
     */
    suspend fun updateEntry(
        entryId: Long,
        amount: Double,
        mode: AmountMode,
        mealType: MealType,
        variantId: Long? = null
    ): Result<Unit> = runCatching {
        require(amount > 0.0) { "数量必须大于 0" }
        val existing = entryDao.getById(entryId) ?: error("记录不存在")
        val food = foodDao.getById(existing.foodId) ?: error("食物不存在，可能已被删除")

        if (food.hasVariants) {
            val variant = resolveVariant(food, variantId)
            entryDao.update(
                existing.copy(
                    foodName = food.name,
                    grams = 0.0,
                    amountMode = AmountMode.SERVING.name,
                    servings = amount,
                    servingSizeGrams = 0.0,
                    carbsPer100g = 0.0,
                    proteinPer100g = 0.0,
                    fatPer100g = 0.0,
                    mealType = mealType.name,
                    variantSpec = variant.specName,
                    variantKcalPerServing = variant.kcal,
                    variantCarbsPerServing = variant.carbsG,
                    variantProteinPerServing = variant.proteinG,
                    variantFatPerServing = variant.fatG,
                    variantSodiumPerServing = variant.sodiumMg,
                    variantCalciumPerServing = variant.calciumMg
                )
            )
            return@runCatching
        }

        require(food.servingSizeGrams > 0.0) { "该食物的一份大小无效" }
        val grams = when (mode) {
            AmountMode.GRAMS -> amount
            AmountMode.SERVING -> amount * food.servingSizeGrams
        }
        entryDao.update(
            existing.copy(
                foodName = food.name,
                grams = grams,
                amountMode = mode.name,
                servings = if (mode == AmountMode.SERVING) amount else 0.0,
                servingSizeGrams = food.servingSizeGrams,
                carbsPer100g = food.carbsPer100g,
                proteinPer100g = food.proteinPer100g,
                fatPer100g = food.fatPer100g,
                mealType = mealType.name
            )
        )
    }

    suspend fun deleteEntry(entryId: Long): Result<Unit> = runCatching {
        entryDao.deleteById(entryId)
    }

    suspend fun clearDay(date: String): Result<Unit> = runCatching {
        entryDao.deleteByDate(date)
    }

    // --------------------------------------------------------------- goals

    /** Always emits a goal row, falling back to the defaults when unset. */
    fun observeGoals(): Flow<MacroGoalEntity> =
        goalDao.observe().map { it ?: MacroGoalEntity.default() }

    suspend fun getGoals(): MacroGoalEntity = goalDao.get() ?: MacroGoalEntity.default()

    suspend fun saveGoals(carbs: Double, protein: Double, fat: Double): Result<Unit> =
        runCatching {
            require(carbs >= 0.0 && protein >= 0.0 && fat >= 0.0) { "目标不能为负数" }
            goalDao.upsert(
                MacroGoalEntity(
                    carbsGrams = carbs,
                    proteinGrams = protein,
                    fatGrams = fat
                )
            )
        }

    // -------------------------------------------------- workout integration

    /**
     * Calories burned by workouts on [date].
     *
     * Read straight from the training tables so the diary can show a net figure.
     * Per product decision this value is **display-only** and never increases the
     * remaining macro allowance.
     */
    fun observeBurnedKcal(date: String): Flow<Double> =
        db.workoutSessionDao().observeBurnedKcal(date)

    suspend fun getBurnedKcal(date: String): Double =
        db.workoutSessionDao().getBurnedKcal(date)
}

/** A day's macro totals plus how many entries produced them. */
data class DayTotals(
    val macros: Macros,
    val entryCount: Int
)
