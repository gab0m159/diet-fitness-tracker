package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.repository.DietRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state of the food library screen. */
data class FoodLibraryUiState(
    val foods: List<FoodEntity> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val message: String? = null,
    /** Variants keyed by food id, so branded foods can show per-serving numbers. */
    val variantsByFood: Map<Long, List<FoodVariantEntity>> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLibraryViewModel(
    private val repository: DietRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val message = MutableStateFlow<String?>(null)

    private val foodsFlow = query.flatMapLatest { q ->
        repository.searchFoods(q)
    }

    val uiState: StateFlow<FoodLibraryUiState> =
        combine(
            foodsFlow,
            query,
            loading,
            message,
            repository.observeAllVariants()
        ) { foods, q, isLoading, msg, variants ->
            FoodLibraryUiState(
                foods = foods,
                query = q,
                loading = isLoading,
                message = msg,
                variantsByFood = variants
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodLibraryUiState()
        )

    init {
        // The first emission flips us out of the loading state.
        viewModelScope.launch {
            foodsFlow.collect {
                loading.value = false
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearMessage() {
        message.value = null
    }

    /** Deletes a food. Diary entries referencing it cascade away, so we say so. */
    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            val used = repository.entriesUsingFood(food.id)
            repository.deleteFood(food).fold(
                onSuccess = {
                    message.value = if (used > 0) {
                        "已删除「${food.name}」，同时移除了 $used 条饮食记录"
                    } else {
                        "已删除「${food.name}」"
                    }
                },
                onFailure = { message.value = "删除失败：${it.message ?: "未知错误"}" }
            )
        }
    }

    companion object {
        fun factory(repository: DietRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FoodLibraryViewModel(repository) as T
            }
    }
}
