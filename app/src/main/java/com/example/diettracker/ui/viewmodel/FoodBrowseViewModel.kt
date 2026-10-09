package com.example.diettracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.repository.DietRepository
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

/** State for the brand / recommended food browser. */
data class FoodBrowseUiState(
    val category: FoodCategory = FoodCategory.BRAND,
    val query: String = "",
    val brandFilter: String? = null,
    val tagFilter: String? = null,
    val foods: List<FoodEntity> = emptyList(),
    val brands: List<String> = emptyList(),
    val loading: Boolean = true
)

/**
 * Browsing the bundled brand and recommended foods.
 *
 * Filtering happens in the DAO so that searching "麦当劳" or "优质碳水" matches
 * on brand and tag columns, not just the name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodBrowseViewModel(
    private val repository: DietRepository,
    initialCategory: FoodCategory
) : ViewModel() {

    private val category = MutableStateFlow(initialCategory)
    private val query = MutableStateFlow("")
    private val brandFilter = MutableStateFlow<String?>(null)
    private val tagFilter = MutableStateFlow<String?>(null)
    private val loading = MutableStateFlow(true)

    /** Query+category drive the DAO; brand/tag filters are applied on top. */
    private val baseFlow = combine(category, query) { cat, q -> cat to q }
        .flatMapLatest { (cat, q) ->
            if (q.isBlank()) {
                repository.observeByCategory(cat.name)
            } else {
                repository.searchFoods(q)
            }
        }

    private val brands = repository.observeBrands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filters = combine(category, query, brandFilter, tagFilter) { c, q, b, t ->
        Filter(c, q, b, t)
    }

    val uiState: StateFlow<FoodBrowseUiState> = combine(
        filters,
        baseFlow,
        brands,
        loading
    ) { filter, foods, brandList, isLoading ->
        val filtered = foods.filter { food ->
            val brandOk = filter.brand == null || food.brandLabel == filter.brand
            val tagOk = filter.tag == null || food.tags.contains(filter.tag)
            // Keep the category view coherent when no search narrowed it down.
            val categoryOk = filter.query.isNotBlank() || food.category == filter.category.name
            brandOk && tagOk && categoryOk
        }
        FoodBrowseUiState(
            category = filter.category,
            query = filter.query,
            brandFilter = filter.brand,
            tagFilter = filter.tag,
            foods = filtered,
            brands = brandList,
            loading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FoodBrowseUiState(category = initialCategory)
    )

    /** Bundle of the four filter inputs, so `combine` stays type-safe. */
    private data class Filter(
        val category: FoodCategory,
        val query: String,
        val brand: String?,
        val tag: String?
    )

    init {
        viewModelScope.launch {
            baseFlow.collect { loading.value = false }
        }
    }

    fun onCategoryChange(value: FoodCategory) {
        category.value = value
        brandFilter.value = null
        tagFilter.value = null
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onBrandFilterChange(value: String?) {
        brandFilter.update { value }
    }

    fun onTagFilterChange(value: String?) {
        tagFilter.update { value }
    }

    companion object {
        fun factory(
            repository: DietRepository,
            initialCategory: FoodCategory
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FoodBrowseViewModel(repository, initialCategory) as T
        }
    }
}
