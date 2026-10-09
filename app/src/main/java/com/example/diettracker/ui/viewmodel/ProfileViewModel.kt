package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.UserProfileEntity
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Sex
import com.example.diettracker.domain.MacroEstimate
import com.example.diettracker.domain.NutritionCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Body metrics + goal estimation screen.
 *
 * The estimate is recomputed live on every keystroke so the user sees the effect
 * of changing activity level or goal mode immediately. Applying the estimate to
 * the macro goals is always an explicit action — the suggestion never silently
 * overwrites a hand-tuned target.
 */
data class ProfileUiState(
    val height: String = "",
    val weight: String = "",
    val age: String = "",
    val bodyFat: String = "",
    val sex: Sex = Sex.MALE,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goalMode: GoalMode = GoalMode.MAINTAIN,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Current saved macro goals, shown so the user can compare. */
    val currentGoalCarbs: Double = 0.0,
    val currentGoalProtein: Double = 0.0,
    val currentGoalFat: Double = 0.0
) {
    val weightKg: Double? get() = weight.toDoubleOrNull()?.takeIf { it > 0 }
    val heightCm: Double? get() = height.toDoubleOrNull()?.takeIf { it > 0 }
    val ageValue: Int? get() = age.toIntOrNull()?.takeIf { it > 0 }
    val bodyFatPercent: Double? get() =
        bodyFat.toDoubleOrNull()?.takeIf { it > 0.0 && it < 70.0 }

    /** Null until the three required fields parse. */
    val estimate: MacroEstimate?
        get() {
            val w = weightKg ?: return null
            val h = heightCm ?: return null
            val a = ageValue ?: return null
            return NutritionCalculator.estimate(
                weightKg = w,
                heightCm = h,
                age = a,
                sex = sex,
                activity = activity,
                goal = goalMode,
                bodyFatPercent = bodyFatPercent
            )
        }

    val hasBodyFat: Boolean get() = bodyFatPercent != null
}

class ProfileViewModel(
    private val dietRepository: DietRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = activityRepository.getProfile()
            _uiState.update {
                it.copy(
                    height = trimNumber(profile.heightCm),
                    weight = trimNumber(profile.weightKg),
                    age = profile.age.toString(),
                    bodyFat = profile.bodyFatPercent?.let { bf -> trimNumber(bf) } ?: "",
                    sex = Sex.fromStorage(profile.sex),
                    activity = ActivityLevel.fromStorage(profile.activityLevel),
                    goalMode = GoalMode.fromStorage(profile.goalMode),
                    loading = false
                )
            }
        }
        viewModelScope.launch {
            dietRepository.observeGoals().collect { goals ->
                _uiState.update {
                    it.copy(
                        currentGoalCarbs = goals.carbsGrams,
                        currentGoalProtein = goals.proteinGrams,
                        currentGoalFat = goals.fatGrams
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------- inputs

    fun onHeightChange(value: String) = _uiState.update { it.copy(height = value, error = null) }
    fun onWeightChange(value: String) = _uiState.update { it.copy(weight = value, error = null) }
    fun onAgeChange(value: String) = _uiState.update { it.copy(age = value, error = null) }
    fun onBodyFatChange(value: String) = _uiState.update { it.copy(bodyFat = value, error = null) }
    fun onSexChange(value: Sex) = _uiState.update { it.copy(sex = value) }
    fun onActivityChange(value: ActivityLevel) = _uiState.update { it.copy(activity = value) }
    fun onGoalModeChange(value: GoalMode) = _uiState.update { it.copy(goalMode = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    /** Clears the optional body-fat field. */
    fun clearBodyFat() = _uiState.update { it.copy(bodyFat = "", error = null) }

    // -------------------------------------------------------------- writes

    /** Persists the profile without touching the macro goals. */
    fun saveProfile() {
        val state = _uiState.value
        val weight = state.weightKg
        val height = state.heightCm
        val age = state.ageValue

        if (height == null) {
            _uiState.update { it.copy(error = "请填写有效身高（80-250 cm）") }
            return
        }
        if (weight == null) {
            _uiState.update { it.copy(error = "请填写有效体重（25-300 kg）") }
            return
        }
        if (age == null) {
            _uiState.update { it.copy(error = "请填写有效年龄（10-100）") }
            return
        }
        if (state.bodyFat.isNotBlank() && state.bodyFatPercent == null) {
            _uiState.update { it.copy(error = "体脂率请填 0-70 之间，或留空不填") }
            return
        }

        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val entity = UserProfileEntity(
                heightCm = height,
                weightKg = weight,
                age = age,
                sex = state.sex.name,
                bodyFatPercent = state.bodyFatPercent,
                activityLevel = state.activity.name,
                goalMode = state.goalMode.name
            )
            activityRepository.saveProfile(entity).fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, message = "身体数据已保存") }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(saving = false, error = error.message ?: "保存失败")
                    }
                }
            )
        }
    }

    /**
     * Copies the estimated macro targets into the daily goals.
     *
     * Only runs on an explicit tap; the estimate is otherwise purely advisory.
     * Values are rounded to whole grams for a clean goal row.
     */
    fun applyEstimateToGoals() {
        val estimate = _uiState.value.estimate
        if (estimate == null) {
            _uiState.update { it.copy(error = "请先填好身高、体重、年龄") }
            return
        }
        val rounded = NutritionCalculator.roundMacros(estimate.targets)
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            dietRepository.saveGoals(
                carbs = rounded.carbs,
                protein = rounded.protein,
                fat = rounded.fat
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            saving = false,
                            message = "已把建议目标应用到每日目标：" +
                                "碳水 ${rounded.carbs.toInt()}g / " +
                                "蛋白 ${rounded.protein.toInt()}g / " +
                                "脂肪 ${rounded.fat.toInt()}g"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(saving = false, error = error.message ?: "应用失败")
                    }
                }
            )
        }
    }

    /** Persists both the profile and its estimate in one go. */
    fun saveProfileAndApply() {
        saveProfile()
        // Applied on the next frame-ish; the profile save is not a prerequisite
        // for the goal write because the estimate comes from local state.
        applyEstimateToGoals()
    }

    private fun trimNumber(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }

    companion object {
        fun factory(
            dietRepository: DietRepository,
            activityRepository: ActivityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(dietRepository, activityRepository) as T
        }
    }
}
