package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.db.MacroGoalEntity
import com.example.diettracker.data.model.DailyEnergy
import com.example.diettracker.data.model.DiaryEntry
import com.example.diettracker.data.model.Macros
import com.example.diettracker.data.model.MealType
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the home (daily diary) screen renders.
 *
 * [remaining] may be negative, which the UI shows as "超出".
 */
data class DiaryUiState(
    val date: String = DateUtils.today(),
    val entries: List<DiaryEntry> = emptyList(),
    val consumed: Macros = Macros.ZERO,
    val goal: Macros = Macros.ZERO,
    val remaining: Macros = Macros.ZERO,
    val loading: Boolean = true,
    val message: String? = null,
    /**
     * Calories burned by today's workouts.
     *
     * Display-only: it feeds [energy] but deliberately does **not** change
     * [remaining] — the user decides whether to eat it back.
     */
    val burnedCalories: Double = 0.0
) {
    val goalCalories: Double get() = goal.calories
    val consumedCalories: Double get() = consumed.calories

    /** Intake / burn / net, for the energy card. */
    val energy: DailyEnergy
        get() = DailyEnergy(
            intakeCalories = consumedCalories,
            burnedCalories = burnedCalories
        )

    /** Entries grouped into meals, in a fixed display order, empty groups dropped. */
    val groupedByMeal: List<Pair<MealType, List<DiaryEntry>>>
        get() = MealType.entries
            .map { meal -> meal to entries.filter { it.mealType == meal } }
            .filter { it.second.isNotEmpty() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val repository: DietRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(DateUtils.today())
    private val loading = MutableStateFlow(true)
    private val message = MutableStateFlow<String?>(null)

    private val entriesFlow = selectedDate.flatMapLatest { date ->
        repository.observeDiary(date)
    }

    /** Workout burn for the selected day, which follows the date selection. */
    private val burnedFlow = selectedDate.flatMapLatest { date ->
        repository.observeBurnedKcal(date)
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        combine(selectedDate, entriesFlow) { date, entries -> date to entries },
        repository.observeGoals(),
        burnedFlow,
        loading,
        message
    ) { (date, entries), goals, burned, isLoading, msg ->
        val consumed = entries.fold(Macros.ZERO) { acc, entry -> acc + entry.macros }
        val goalMacros = Macros(goals.carbsGrams, goals.proteinGrams, goals.fatGrams)
        DiaryUiState(
            date = date,
            entries = entries,
            consumed = consumed,
            goal = goalMacros,
            // Remaining is purely goal minus intake. Workout burn is NOT added
            // back here, by explicit product decision.
            remaining = Macros(
                carbs = goalMacros.carbs - consumed.carbs,
                protein = goalMacros.protein - consumed.protein,
                fat = goalMacros.fat - consumed.fat
            ),
            loading = isLoading,
            message = msg,
            burnedCalories = burned
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    /** Foods for the "add entry" picker, exposed so that screen can share this VM. */
    private val foodQuery = MutableStateFlow("")
    val foods: StateFlow<List<FoodEntity>> = foodQuery
        .flatMapLatest { q -> repository.searchFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Variants keyed by food id, so the picker can show per-serving nutrition for
     * branded foods rather than the unused per-100g columns.
     */
    val variantsByFood: StateFlow<Map<Long, List<FoodVariantEntity>>> =
        repository.observeAllVariants()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            entriesFlow.collect { loading.value = false }
        }
    }

    // ------------------------------------------------------------ date nav

    fun setDate(date: String) = selectedDate.update { date }

    fun previousDay() = selectedDate.update { DateUtils.minusDays(it, 1) }

    fun nextDay() = selectedDate.update { DateUtils.plusDays(it, 1) }

    fun goToToday() = selectedDate.update { DateUtils.today() }

    // -------------------------------------------------------------- edits

    fun onFoodQueryChange(value: String) {
        foodQuery.value = value
    }

    /**
     * Sizes of a branded food, loaded when the user taps it in the picker.
     * Empty for per-100g foods.
     */
    suspend fun variantsFor(foodId: Long): List<FoodVariantEntity> =
        repository.getVariants(foodId)

    fun addEntry(
        foodId: Long,
        amount: Double,
        mode: com.example.diettracker.data.model.AmountMode,
        meal: MealType,
        variantId: Long? = null
    ) {
        viewModelScope.launch {
            repository.addEntry(selectedDate.value, foodId, amount, mode, meal, variantId).fold(
                onSuccess = { message.value = "已添加到 ${DateUtils.friendlyLabel(selectedDate.value)}" },
                onFailure = { message.value = "添加失败：${it.message ?: "未知错误"}" }
            )
        }
    }

    fun updateEntry(
        entryId: Long,
        amount: Double,
        mode: com.example.diettracker.data.model.AmountMode,
        meal: MealType,
        variantId: Long? = null
    ) {
        viewModelScope.launch {
            repository.updateEntry(entryId, amount, mode, meal, variantId).fold(
                onSuccess = { message.value = "已更新记录" },
                onFailure = { message.value = "更新失败：${it.message ?: "未知错误"}" }
            )
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId).fold(
                onSuccess = { message.value = "已删除该条记录" },
                onFailure = { message.value = "删除失败：${it.message ?: "未知错误"}" }
            )
        }
    }

    fun clearDay() {
        viewModelScope.launch {
            repository.clearDay(selectedDate.value).fold(
                onSuccess = { message.value = "已清空当天记录" },
                onFailure = { message.value = "清空失败：${it.message ?: "未知错误"}" }
            )
        }
    }

    fun clearMessage() {
        message.value = null
    }

    companion object {
        /**
         * @param trainingRepository accepted for symmetry with the other
         *        ViewModels; the training block on the home screen has its own
         *        [WorkoutViewModel].
         */
        fun factory(
            repository: DietRepository,
            @Suppress("UNUSED_PARAMETER")
            trainingRepository: com.example.diettracker.data.repository.TrainingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DiaryViewModel(repository) as T
        }
    }
}

/** Shown on the goals screen; also read by the home summary card. */
data class GoalUiState(
    val carbs: String = "",
    val protein: String = "",
    val fat: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null
) {
    val preview: Macros?
        get() {
            val c = carbs.toDoubleOrNull() ?: return null
            val p = protein.toDoubleOrNull() ?: return null
            val f = fat.toDoubleOrNull() ?: return null
            if (c < 0 || p < 0 || f < 0) return null
            return Macros(c, p, f)
        }
}

/** Daily macro goal editor (本应用只有一项目标设置，全局持久保存). */
class GoalViewModel(
    private val repository: DietRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val goal: MacroGoalEntity = repository.getGoals()
            _uiState.update {
                it.copy(
                    carbs = format(goal.carbsGrams),
                    protein = format(goal.proteinGrams),
                    fat = format(goal.fatGrams),
                    loading = false
                )
            }
        }
    }

    fun onCarbsChange(value: String) = _uiState.update { it.copy(carbs = value, error = null) }
    fun onProteinChange(value: String) = _uiState.update { it.copy(protein = value, error = null) }
    fun onFatChange(value: String) = _uiState.update { it.copy(fat = value, error = null) }

    fun restoreDefaults() {
        val defaults = MacroGoalEntity.default()
        _uiState.update {
            it.copy(
                carbs = format(defaults.carbsGrams),
                protein = format(defaults.proteinGrams),
                fat = format(defaults.fatGrams),
                error = null
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val carbs = state.carbs.toDoubleOrNull()
        val protein = state.protein.toDoubleOrNull()
        val fat = state.fat.toDoubleOrNull()

        if (carbs == null || carbs < 0) {
            _uiState.update { it.copy(error = "碳水目标请填写 0 或正数") }
            return
        }
        if (protein == null || protein < 0) {
            _uiState.update { it.copy(error = "蛋白质目标请填写 0 或正数") }
            return
        }
        if (fat == null || fat < 0) {
            _uiState.update { it.copy(error = "脂肪目标请填写 0 或正数") }
            return
        }
        if (carbs + protein + fat <= 0.0) {
            _uiState.update { it.copy(error = "至少填写一项大于 0 的目标") }
            return
        }

        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            repository.saveGoals(carbs, protein, fat).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, savedMessage = "目标已保存") }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            saving = false,
                            error = "保存失败：${error.message ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun clearSavedMessage() = _uiState.update { it.copy(savedMessage = null) }

    private fun format(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }

    companion object {
        fun factory(repository: DietRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GoalViewModel(repository) as T
            }
    }
}
