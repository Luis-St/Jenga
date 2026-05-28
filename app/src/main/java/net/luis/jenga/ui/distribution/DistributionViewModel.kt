package net.luis.jenga.ui.distribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.data.repository.DistributionRepository
import net.luis.jenga.domain.model.Distribution
import net.luis.jenga.domain.model.DistributionGroup

data class DistributionListUiState(
    val distributions: List<Distribution> = emptyList()
)

class DistributionViewModel(private val repository: DistributionRepository) : ViewModel() {

    private val _listState = MutableStateFlow(DistributionListUiState())
    val listState: StateFlow<DistributionListUiState> = _listState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allDistributions.collect { list ->
                _listState.value = DistributionListUiState(distributions = list)
            }
        }
    }

    fun deleteDistribution(distribution: Distribution) {
        viewModelScope.launch { repository.deleteDistribution(distribution) }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DistributionViewModel(DistributionRepository(database)) as T
    }
}
