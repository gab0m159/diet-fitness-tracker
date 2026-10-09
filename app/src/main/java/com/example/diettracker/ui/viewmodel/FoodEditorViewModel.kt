package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.repository.DietRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backing state for the create/edit food form.
 *
 * All fields are raw strings because they come straight from text fields; they
 * are parsed and validated together in [save].
 */
data class FoodEditorUiState(
    val foodId: Long = 0L,
    val name: String = "",
    val carbs: String = "",
    val protein: String = "",
    val fat: String = "",
    val servingSize: String = "",
    val note: String = "",
    val isEditing: Boolean = false,
    val loaded: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
) {
    /** Live preview of the nutrition of one serving. */
    val servingPreview: ServingPreview?
        get() {
            val serving = servingSize.toDoubleOrNull() ?: return null
            val c = carbs.toDoubleOrNull() ?: 0.0
            val p = protein.toDoubleOrNull() ?: 0.0
            val f = fat.toDoubleOrNull() ?: 0.0
            if (serving <= 0.0) return null
            val factor = serving / 100.0
            return ServingPreview(c * factor, p * factor, f * factor)
        }
}

data class ServingPreview(val carbs: Double, val protein: Double, val fat: Double) {
    val calories: Double get() = carbs * 4.0 + protein * 4.0 + fat * 9.0
}

class FoodEditorViewModel(
    private val repository: DietRepository,
    private val foodId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FoodEditorUiState(
            foodId = foodId,
            isEditing = foodId != 0L,
            loaded = foodId == 0L
        )
    )
    val uiState: StateFlow<FoodEditorUiState> = _uiState.asStateFlow()

    init {
        if (foodId != 0L) {
            viewModelScope.launch {
                val food = repository.getFood(foodId)
                _uiState.update { state ->
                    if (food == null) {
                        state.copy(
                            loaded = true,
                            error = "食物不存在，可能已被删除"
                        )
                    } else {
                        state.copy(
                            name = food.name,
                            carbs = trimNumber(food.carbsPer100g),
                            protein = trimNumber(food.proteinPer100g),
                            fat = trimNumber(food.fatPer100g),
                            servingSize = trimNumber(food.servingSizeGrams),
                            note = food.note,
                            loaded = true
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onCarbsChange(value: String) = _uiState.update { it.copy(carbs = value, error = null) }
    fun onProteinChange(value: String) = _uiState.update { it.copy(protein = value, error = null) }
    fun onFatChange(value: String) = _uiState.update { it.copy(fat = value, error = null) }
    fun onServingChange(value: String) = _uiState.update { it.copy(servingSize = value, error = null) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value, error = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Validates and persists. Sets [FoodEditorUiState.savedSuccessfully] so the
     * caller can pop back.
     */
    fun save() {
        val state = _uiState.value
        if (state.saving) return

        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "请填写食物名称") }
            return
        }
        val carbs = state.carbs.toDoubleOrNull()
        val protein = state.protein.toDoubleOrNull()
        val fat = state.fat.toDoubleOrNull()
        if (carbs == null || carbs < 0) {
            _uiState.update { it.copy(error = "碳水含量请填写 0 或正数（每 100g 克数）") }
            return
        }
        if (protein == null || protein < 0) {
            _uiState.update { it.copy(error = "蛋白质含量请填写 0 或正数（每 100g 克数）") }
            return
        }
        if (fat == null || fat < 0) {
            _uiState.update { it.copy(error = "脂肪含量请填写 0 或正数（每 100g 克数）") }
            return
        }
        val serving = state.servingSize.toDoubleOrNull()
        if (serving == null || serving <= 0) {
            _uiState.update { it.copy(error = "一份大小请填写大于 0 的克数，例如 50") }
            return
        }

        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val duplicate = repository.isFoodNameTaken(name, state.foodId)
            if (duplicate) {
                _uiState.update {
                    it.copy(saving = false, error = "已存在同名食物「$name」，请换个名字")
                }
                return@launch
            }

            val entity = FoodEntity(
                id = state.foodId,
                name = name,
                carbsPer100g = carbs,
                proteinPer100g = protein,
                fatPer100g = fat,
                servingSizeGrams = serving,
                note = state.note.trim()
            )

            val result = if (state.isEditing) {
                repository.updateFood(entity)
            } else {
                repository.addFood(entity).map { }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, savedSuccessfully = true) }
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

    private fun trimNumber(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toString()
    }

    companion object {
        fun factory(repository: DietRepository, foodId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FoodEditorViewModel(repository, foodId) as T
            }
    }
}
