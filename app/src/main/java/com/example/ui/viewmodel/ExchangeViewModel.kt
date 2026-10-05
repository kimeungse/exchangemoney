package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversionHistoryEntity
import com.example.data.model.AggregationUnit
import com.example.data.model.ChartPairMode
import com.example.data.model.DateRangePreset
import com.example.data.model.HistoricalChartPoint
import com.example.data.repository.ExchangeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

sealed interface RateUiState {
    object Loading : RateUiState
    data class Success(
        val krwToUsd: Double,
        val tjsToUsd: Double,
        val lastUpdated: Long
    ) : RateUiState
    data class Error(val message: String) : RateUiState
}

class ExchangeViewModel(private val repository: ExchangeRepository) : ViewModel() {

    private val _rateUiState = MutableStateFlow<RateUiState>(RateUiState.Loading)
    val rateUiState: StateFlow<RateUiState> = _rateUiState.asStateFlow()

    // Interactive calculator states
    val inputAmount = MutableStateFlow("1")
    val sourceCurrency = MutableStateFlow("KRW") // Or "TJS", "USD"
    val targetCurrency = MutableStateFlow("TJS") // Or "KRW", "USD"

    // Historical Chart states (1-Year KRW <-> TJS Daily / Weekly / Monthly + Custom Date Range)
    val minAllowedDateMillis: Long = ExchangeRepository.startOfDayMillis(
        System.currentTimeMillis() - 365L * ExchangeRepository.DAY_MILLIS
    )
    val maxAllowedDateMillis: Long = ExchangeRepository.startOfDayMillis(System.currentTimeMillis())

    private val _aggregationUnit = MutableStateFlow(AggregationUnit.DAILY)
    val aggregationUnit: StateFlow<AggregationUnit> = _aggregationUnit.asStateFlow()

    private val _dateRangePreset = MutableStateFlow(DateRangePreset.ONE_YEAR)
    val dateRangePreset: StateFlow<DateRangePreset> = _dateRangePreset.asStateFlow()

    private val _startDateMillis = MutableStateFlow(minAllowedDateMillis)
    val startDateMillis: StateFlow<Long> = _startDateMillis.asStateFlow()

    private val _endDateMillis = MutableStateFlow(maxAllowedDateMillis)
    val endDateMillis: StateFlow<Long> = _endDateMillis.asStateFlow()

    private val _chartPairMode = MutableStateFlow(ChartPairMode.TJS_TO_KRW)
    val chartPairMode: StateFlow<ChartPairMode> = _chartPairMode.asStateFlow()

    private val _historicalPoints = MutableStateFlow<List<HistoricalChartPoint>>(emptyList())
    val historicalPoints: StateFlow<List<HistoricalChartPoint>> = _historicalPoints.asStateFlow()

    private val _isChartLoading = MutableStateFlow(true)
    val isChartLoading: StateFlow<Boolean> = _isChartLoading.asStateFlow()

    // AI Insight states
    private val _aiInsight = MutableStateFlow("")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Conversion history list
    val historyList: StateFlow<List<ConversionHistoryEntity>> = repository.conversionHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        fetchRates()
    }

    fun fetchRates() {
        viewModelScope.launch {
            _rateUiState.value = RateUiState.Loading
            try {
                val rates = repository.getRates()
                _rateUiState.value = RateUiState.Success(
                    krwToUsd = rates.first,
                    tjsToUsd = rates.second,
                    lastUpdated = System.currentTimeMillis()
                )
                loadHistoricalChart(rates.first, rates.second)
                // Automatically fetch AI insight once rates are successfully fetched
                generateAiInsight(rates.first, rates.second)
            } catch (e: Exception) {
                _rateUiState.value = RateUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
                loadHistoricalChart(1380.0, 10.9)
            }
        }
    }

    fun setAggregationUnit(unit: AggregationUnit) {
        if (_aggregationUnit.value != unit) {
            _aggregationUnit.value = unit
            reloadChartWithCurrentRates()
        }
    }

    fun setChartPairMode(mode: ChartPairMode) {
        _chartPairMode.value = mode
    }

    fun setDateRangePreset(preset: DateRangePreset) {
        _dateRangePreset.value = preset
        val days = preset.days
        if (days != null) {
            val today = ExchangeRepository.startOfDayMillis(System.currentTimeMillis())
            val start = (today - (days - 1).toLong() * ExchangeRepository.DAY_MILLIS)
                .coerceAtLeast(minAllowedDateMillis)
            _startDateMillis.value = start
            _endDateMillis.value = today
            reloadChartWithCurrentRates()
        }
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val normalizedStart = ExchangeRepository.startOfDayMillis(min(startMillis, endMillis))
            .coerceIn(minAllowedDateMillis, maxAllowedDateMillis)
        val normalizedEnd = ExchangeRepository.startOfDayMillis(max(startMillis, endMillis))
            .coerceIn(minAllowedDateMillis, maxAllowedDateMillis)

        _dateRangePreset.value = DateRangePreset.CUSTOM
        _startDateMillis.value = normalizedStart
        _endDateMillis.value = normalizedEnd
        reloadChartWithCurrentRates()
    }

    private fun reloadChartWithCurrentRates() {
        val currentState = _rateUiState.value
        val krw = if (currentState is RateUiState.Success) currentState.krwToUsd else 1380.0
        val tjs = if (currentState is RateUiState.Success) currentState.tjsToUsd else 10.9
        loadHistoricalChart(krw, tjs)
    }

    private fun loadHistoricalChart(krw: Double, tjs: Double) {
        viewModelScope.launch {
            _isChartLoading.value = true
            try {
                val points = repository.getHistoricalChartData(
                    startMillis = _startDateMillis.value,
                    endMillis = _endDateMillis.value,
                    aggregationUnit = _aggregationUnit.value,
                    currentKrw = krw,
                    currentTjs = tjs
                )
                _historicalPoints.value = points
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isChartLoading.value = false
            }
        }
    }


    fun generateAiInsight(krw: Double, tjs: Double) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                _aiInsight.value = repository.fetchComparativeInsights(krw, tjs)
            } catch (e: Exception) {
                _aiInsight.value = "AI 분석을 불러오는 도중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun computeConversion(amountStr: String, source: String, target: String, krw: Double, tjs: Double): Double {
        val amount = amountStr.toDoubleOrNull() ?: return 0.0
        val sourceRate = when (source) {
            "KRW" -> krw
            "TJS" -> tjs
            else -> 1.0
        }
        val targetRate = when (target) {
            "KRW" -> krw
            "TJS" -> tjs
            else -> 1.0
        }
        if (sourceRate <= 0.0 || targetRate <= 0.0) return 0.0
        return (amount / sourceRate) * targetRate
    }

    fun saveConversion(amount: Double, source: String, targetAmount: Double, target: String) {
        viewModelScope.launch {
            if (amount > 0) {
                repository.saveHistory(amount, source, targetAmount, target)
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// Factory for ViewModel injection
class ExchangeViewModelFactory(private val repository: ExchangeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExchangeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExchangeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
