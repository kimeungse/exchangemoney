package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.local.ExchangeDatabase
import com.example.data.local.ConversionHistoryEntity
import com.example.data.repository.ExchangeRepository
import com.example.ui.components.HistoricalRateChartSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExchangeViewModel
import com.example.ui.viewmodel.ExchangeViewModelFactory
import com.example.ui.viewmodel.RateUiState
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and dependency injection matching MVVM Repo rules
        val database = Room.databaseBuilder(
            applicationContext,
            ExchangeDatabase::class.java,
            "exchange_database"
        ).fallbackToDestructiveMigration().build()

        val dao = database.exchangeDao()
        val repository = ExchangeRepository(dao)
        val viewModel = ViewModelProvider(
            this,
            ExchangeViewModelFactory(repository)
        )[ExchangeViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    ExchangeScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel,
    modifier: Modifier = Modifier
) {
    val rateState by viewModel.rateUiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val inputAmount by viewModel.inputAmount.collectAsStateWithLifecycle()
    val sourceCurrency by viewModel.sourceCurrency.collectAsStateWithLifecycle()
    val targetCurrency by viewModel.targetCurrency.collectAsStateWithLifecycle()
    val aiInsight by viewModel.aiInsight.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    // Historical Chart states
    val historicalPoints by viewModel.historicalPoints.collectAsStateWithLifecycle()
    val aggregationUnit by viewModel.aggregationUnit.collectAsStateWithLifecycle()
    val dateRangePreset by viewModel.dateRangePreset.collectAsStateWithLifecycle()
    val startDateMillis by viewModel.startDateMillis.collectAsStateWithLifecycle()
    val endDateMillis by viewModel.endDateMillis.collectAsStateWithLifecycle()
    val chartPairMode by viewModel.chartPairMode.collectAsStateWithLifecycle()
    val isChartLoading by viewModel.isChartLoading.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Background Gradient for Dark Slate ambiance matching premium design
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E293B)  // Slate 800
        )
    )

    Box(
        modifier = modifier
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "환율 비교기",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "원화 (KRW) ⇄ 소모니 (TJS) ⇄ 달러 (USD)",
                        color = Color(0xFF94A3B8), // Slate 400
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                IconButton(
                    onClick = { viewModel.fetchRates() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF334155), // Slate 700
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Divider
            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rate Dashboard
                item {
                    when (val state = rateState) {
                        is RateUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("실시간 환율 데이터를 조회하고 있습니다...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                        is RateUiState.Success -> {
                            RateCardsSection(
                                krwToUsd = state.krwToUsd,
                                tjsToUsd = state.tjsToUsd,
                                lastUpdated = state.lastUpdated
                            )
                        }
                        is RateUiState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)), // Dark Red
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "오류",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "연결 오류: ${state.message}\n오프라인 모드 기본값으로 계산합니다.",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 1-Year Historical KRW <-> TJS Line Chart Section (Daily / Weekly / Monthly + Custom Date Range)
                item {
                    HistoricalRateChartSection(
                        points = historicalPoints,
                        aggregationUnit = aggregationUnit,
                        dateRangePreset = dateRangePreset,
                        startDateMillis = startDateMillis,
                        endDateMillis = endDateMillis,
                        minAllowedDateMillis = viewModel.minAllowedDateMillis,
                        maxAllowedDateMillis = viewModel.maxAllowedDateMillis,
                        chartPairMode = chartPairMode,
                        isLoading = isChartLoading,
                        onAggregationUnitChange = { viewModel.setAggregationUnit(it) },
                        onDateRangePresetChange = { viewModel.setDateRangePreset(it) },
                        onCustomDateRangeSelected = { start, end ->
                            viewModel.setCustomDateRange(start, end)
                        },
                        onChartPairModeChange = { viewModel.setChartPairMode(it) }
                    )
                }

                // Interactive Calculator Section
                item {
                    val currentKrw = if (rateState is RateUiState.Success) (rateState as RateUiState.Success).krwToUsd else 1380.0
                    val currentTjs = if (rateState is RateUiState.Success) (rateState as RateUiState.Success).tjsToUsd else 10.9

                    ConverterSection(
                        inputAmount = inputAmount,
                        sourceCurrency = sourceCurrency,
                        targetCurrency = targetCurrency,
                        krwRate = currentKrw,
                        tjsRate = currentTjs,
                        onAmountChange = { viewModel.inputAmount.value = it },
                        onSourceCurrencyChange = { newSource ->
                            if (newSource == targetCurrency) {
                                viewModel.targetCurrency.value = sourceCurrency
                            }
                            viewModel.sourceCurrency.value = newSource
                        },
                        onTargetCurrencyChange = { newTarget ->
                            if (newTarget == sourceCurrency) {
                                viewModel.sourceCurrency.value = targetCurrency
                            }
                            viewModel.targetCurrency.value = newTarget
                        },
                        onSaveClick = { amt, src, tgtAmt, tgt ->
                            viewModel.saveConversion(amt, src, tgtAmt, tgt)
                            keyboardController?.hide()
                        }
                    )
                }

                // AI Insight Analysis from Gemini API
                item {
                    val currentKrw = if (rateState is RateUiState.Success) (rateState as RateUiState.Success).krwToUsd else 1380.0
                    val currentTjs = if (rateState is RateUiState.Success) (rateState as RateUiState.Success).tjsToUsd else 10.9

                    AiInsightCard(
                        insightText = aiInsight,
                        isLoading = isAiLoading,
                        onRegenerate = { viewModel.generateAiInsight(currentKrw, currentTjs) }
                    )
                }

                // Recent History List powered by Room Local Cache
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "최근 계산 내역",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (historyList.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearHistory() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF43F5E))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = "전체 삭제", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("전체 삭제", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (historyList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "최근 환전 기록이 없습니다.\n상단의 변환기에서 기록하기 버튼을 눌러보세요.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(historyList, key = { it.id }) { item ->
                        HistoryItemRow(
                            item = item,
                            onDelete = { viewModel.deleteHistoryItem(item.id) }
                        )
                    }
                }
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "실시간 오픈 에이피아이 환율 제공 • Powered by Gemini",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RateCardsSection(
    krwToUsd: Double,
    tjsToUsd: Double,
    lastUpdated: Long
) {
    val krwPerTjs = krwToUsd / tjsToUsd
    val tjsPerKrw = tjsToUsd / krwToUsd

    // Formatting timestamp
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    val dateString = timeFormat.format(Date(lastUpdated))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Direct USD comparisons (Korea and Tajikistan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // USD -> KRW Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵 1 달러 (USD)", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("🇺🇸", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "${String.format(Locale.US, "%,.1f", krwToUsd)} 원",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("대한민국 (KRW)", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }

            // USD -> TJS Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵 1 달러 (USD)", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("🇺🇸", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", tjsToUsd)} TJS",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("타지키스탄 (TJS)", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }
        }

        // TJS <-> KRW Cross Rates Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TJS -> KRW Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1 소모니 (TJS)", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("🇹🇯", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", krwPerTjs)} 원",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("대한민국 원화 (KRW)", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }

            // KRW -> TJS Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1,000 원 (KRW)", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("🇰🇷", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "${String.format(Locale.US, "%,.4f", tjsPerKrw * 1000)} TJS",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("타지키스탄 소모니 (TJS)", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }
        }

        // Standard Benchmark Footer strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊 달러 기준 비교", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(" • ", color = Color(0xFF64748B))
                    Text("USD/KRW ${String.format(Locale.US, "%,.1f", krwToUsd)}", color = Color(0xFF34D399), fontSize = 11.sp)
                    Text(" • ", color = Color(0xFF64748B))
                    Text("USD/TJS ${String.format(Locale.US, "%,.2f", tjsToUsd)}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
                Text(
                    text = "조회시간: $dateString",
                    color = Color(0xFF475569),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ConverterSection(
    inputAmount: String,
    sourceCurrency: String,
    targetCurrency: String,
    krwRate: Double,
    tjsRate: Double,
    onAmountChange: (String) -> Unit,
    onSourceCurrencyChange: (String) -> Unit,
    onTargetCurrencyChange: (String) -> Unit,
    onSaveClick: (Double, String, Double, String) -> Unit
) {
    val amountDouble = inputAmount.toDoubleOrNull() ?: 1.0

    val sourceRate = when (sourceCurrency) {
        "KRW" -> krwRate
        "TJS" -> tjsRate
        else -> 1.0
    }
    val targetRate = when (targetCurrency) {
        "KRW" -> krwRate
        "TJS" -> tjsRate
        else -> 1.0
    }

    val calculatedTarget = if (sourceRate > 0.0) (amountDouble / sourceRate) * targetRate else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "실시간 환율 변환기 (KRW / TJS / USD)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Number input field
            OutlinedTextField(
                value = inputAmount,
                onValueChange = { newValue ->
                    // Clean input: only numbers and one decimal dot
                    val cleaned = newValue.filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) {
                        onAmountChange(cleaned)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("금액 입력", color = Color(0xFF94A3B8)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = Color(0xFF38BDF8)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Source Currency selection row
            Column {
                Text(
                    text = "보내는 화폐 (From)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currencies = listOf("KRW" to "🇰🇷", "TJS" to "🇹🇯", "USD" to "🇺🇸")
                    currencies.forEach { (curr, flag) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sourceCurrency == curr) Color(0xFF334155) else Color(0xFF1E293B))
                                .clickable { onSourceCurrencyChange(curr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$flag $curr",
                                color = if (sourceCurrency == curr) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Direction symbol separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        thickness = 1.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▼", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        thickness = 1.dp
                    )
                }
            }

            // Target Currency selection row
            Column {
                Text(
                    text = "받는 화폐 (To)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currencies = listOf("KRW" to "🇰🇷", "TJS" to "🇹🇯", "USD" to "🇺🇸")
                    currencies.forEach { (curr, flag) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (targetCurrency == curr) Color(0xFF334155) else Color(0xFF1E293B))
                                .clickable { onTargetCurrencyChange(curr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$flag $curr",
                                color = if (targetCurrency == curr) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Result Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "환전 예상 금액",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = when (targetCurrency) {
                            "KRW" -> "${String.format(Locale.KOREA, "%,.2f", calculatedTarget)} 원"
                            "TJS" -> "${String.format(Locale.US, "%,.4f", calculatedTarget)} TJS"
                            else -> "${String.format(Locale.US, "%,.4f", calculatedTarget)} USD"
                        },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = when (targetCurrency) {
                        "KRW" -> "🇰🇷 KRW"
                        "TJS" -> "🇹🇯 TJS"
                        else -> "🇺🇸 USD"
                    },
                    color = when (targetCurrency) {
                        "KRW" -> Color(0xFF34D399)
                        "TJS" -> Color(0xFF38BDF8)
                        else -> Color(0xFFFBBF24)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // History log trigger button with Material design standards
            Button(
                onClick = {
                    onSaveClick(amountDouble, sourceCurrency, calculatedTarget, targetCurrency)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "기록 추가",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "내역에 기록하기",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AiInsightCard(
    insightText: String,
    isLoading: Boolean,
    onRegenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✨ Gemini AI 경제 분석",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLoading) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "AI 분석 갱신",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "실시간 거시 경제 흐름과 분석 정보를 수집하고 있습니다...",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = insightText.ifEmpty { "환율 데이터를 기반으로 Gemini 인공지능이 양국 경제 분석을 준비 중입니다." },
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: ConversionHistoryEntity,
    onDelete: () -> Unit
) {
    val srcFlag = when (item.sourceCurrency) {
        "KRW" -> "🇰🇷"
        "TJS" -> "🇹🇯"
        else -> "🇺🇸"
    }
    val tgtFlag = when (item.targetCurrency) {
        "KRW" -> "🇰🇷"
        "TJS" -> "🇹🇯"
        else -> "🇺🇸"
    }

    val timeFormat = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)
    val displayTime = timeFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(srcFlag, fontSize = 14.sp)
                    Text(
                        text = when (item.sourceCurrency) {
                            "KRW" -> "${String.format(Locale.KOREA, "%,.0f", item.sourceAmount)} KRW"
                            "TJS" -> "${String.format(Locale.US, "%,.2f", item.sourceAmount)} TJS"
                            else -> "${String.format(Locale.US, "%,.2f", item.sourceAmount)} USD"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "이동",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(tgtFlag, fontSize = 14.sp)
                    Text(
                        text = when (item.targetCurrency) {
                            "KRW" -> "${String.format(Locale.KOREA, "%,.1f", item.targetAmount)} KRW"
                            "TJS" -> "${String.format(Locale.US, "%,.3f", item.targetAmount)} TJS"
                            else -> "${String.format(Locale.US, "%,.2f", item.targetAmount)} USD"
                        },
                        color = when (item.targetCurrency) {
                            "KRW" -> Color(0xFF34D399)
                            "TJS" -> Color(0xFF38BDF8)
                            else -> Color(0xFFFBBF24) // Gold/Yellow for USD
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayTime,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
