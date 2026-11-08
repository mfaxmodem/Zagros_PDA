package ir.zagroserp.pda.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.model.Goals
import ir.zagroserp.pda.domain.model.ReportSummary
import ir.zagroserp.pda.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ReportRange(val label: String, val days: Long) {
    WEEK("۷ روز", 7),
    MONTH("۳۰ روز", 30),
    QUARTER("۹۰ روز", 90),
}

data class ReportsUiState(
    val range: ReportRange = ReportRange.MONTH,
    val report: ReportSummary = ReportSummary(),
    val goals: Goals = Goals(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val goalProgress: Float
        get() = when {
            goals.targetValue > 0 ->
                (goals.achievedValue / goals.targetValue).toFloat().coerceIn(0f, 1f)
            else -> (goals.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
        }
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun setRange(range: ReportRange) {
        _state.update { it.copy(range = range) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val days = _state.value.range.days
            val to = LocalDate.now()
            val from = to.minusDays(days)

            reportRepository.goals().let { result ->
                when (result) {
                    is AppResult.Success -> _state.update { it.copy(goals = result.data) }
                    is AppResult.Failure -> _state.update { it.copy(error = result.error.message) }
                }
            }

            reportRepository.report(from.toString(), to.toString()).let { result ->
                when (result) {
                    is AppResult.Success -> _state.update { it.copy(report = result.data) }
                    is AppResult.Failure -> _state.update { it.copy(error = result.error.message) }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }
}
