package ir.zagroserp.pda.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListUiState(
    val query: String = "",
    val isSyncing: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListUiState())
    val state: StateFlow<CustomerListUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    val customers: Flow<PagingData<Customer>> = queryFlow
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { query -> customerRepository.pagingCustomers(query) }
        .cachedIn(viewModelScope)

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            customerRepository.refreshCustomers(_state.value.query.takeIf { it.isNotBlank() })
                .let { result ->
                    if (result is ir.zagroserp.pda.core.AppResult.Failure) {
                        _state.update { it.copy(error = result.error.message) }
                    }
                }
            _state.update { it.copy(isSyncing = false) }
        }
    }
}
