package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AggregationUnit
import com.example.data.model.ChartPairMode
import com.example.data.model.DateRangePreset
import com.example.data.model.HistoricalChartPoint
import com.example.data.repository.ExchangeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalRateChartSection(
    points: List<HistoricalChartPoint>,
    aggregationUnit: AggregationUnit,
    dateRangePreset: DateRangePreset,
    startDateMillis: Long,
    endDateMillis: Long,
    minAllowedDateMillis: Long,
    maxAllowedDateMillis: Long,
    chartPairMode: ChartPairMode,
    isLoading: Boolean,
    onAggregationUnitChange: (AggregationUnit) -> Unit,
    onDateRangePresetChange: (DateRangePreset) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    onChartPairModeChange: (ChartPairMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember(points, aggregationUnit, chartPairMode) {
        mutableStateOf(if (points.isNotEmpty()) points.lastIndex else -1)
    }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var activeDateTab by remember { mutableStateOf(0) } // 0 = Start Date, 1 = End Date
    var showDataTable by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }
    val startDateText = remember(startDateMillis) { dateFormatter.format(Date(startDateMillis)) }
    val endDateText = remember(endDateMillis) { dateFormatter.format(Date(endDateMillis)) }
    val totalDaysSelected = remember(startDateMillis, endDateMillis) {
        ((abs(endDateMillis - startDateMillis) / ExchangeRepository.DAY_MILLIS) + 1L).toInt()
    }

    val primaryAccent = if (chartPairMode == ChartPairMode.TJS_TO_KRW) {
        Color(0xFF38BDF8) // Sky Blue
    } else {
        Color(0xFF34D399) // Emerald
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("historical_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Section Title & Pair Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "환율 추이 그래프",
                            tint = primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KRW ⇄ TJS 1년 환율 추이",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = chartPairMode.titleKo,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Pair Direction Toggle Button
                Surface(
                    onClick = {
                        val nextMode = if (chartPairMode == ChartPairMode.TJS_TO_KRW) {
                            ChartPairMode.KRW_TO_TJS
                        } else {
                            ChartPairMode.TJS_TO_KRW
                        }
                        onChartPairModeChange(nextMode)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("chart_pair_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "기준 통화 전환",
                            tint = primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chartPairMode.shortLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Aggregation Unit Selector: 일별 (Daily) / 주별 (Weekly) / 월별 (Monthly)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "조회 단위 (일별 / 주별 / 월별)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = aggregationUnit.descriptionKo,
                        color = primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AggregationUnit.entries.forEach { unit ->
                        val isSelected = aggregationUnit == unit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isSelected) primaryAccent else Color.Transparent
                                )
                                .clickable { onAggregationUnitChange(unit) }
                                .testTag("unit_${unit.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit.labelKo,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Date Range Selector: Quick Presets + Direct Custom Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "조회 기간 선택 (최대 1년)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "총 ${totalDaysSelected}일간 (${points.size}개 구간)",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                // Quick Preset Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        val isSelected = dateRangePreset == preset
                        Surface(
                            onClick = {
                                if (preset == DateRangePreset.CUSTOM) {
                                    activeDateTab = 0
                                    showDateRangeDialog = true
                                } else {
                                    onDateRangePresetChange(preset)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF334155) else Color(0xFF1E293B),
                            modifier = Modifier
                                .height(34.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) primaryAccent else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("preset_${preset.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.labelKo,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Direct Custom Start/End Date Picker Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(
                            width = 1.dp,
                            color = if (dateRangePreset == DateRangePreset.CUSTOM) primaryAccent.copy(alpha = 0.6f) else Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                activeDateTab = 0
                                showDateRangeDialog = true
                            }
                            .padding(vertical = 2.dp)
                            .testTag("start_date_picker_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "시작일 선택",
                            tint = primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("시작일 (직접 선택)", color = Color(0xFF64748B), fontSize = 10.sp)
                            Text(
                                text = startDateText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "〜",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // End Date Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                activeDateTab = 1
                                showDateRangeDialog = true
                            }
                            .padding(vertical = 2.dp)
                            .testTag("end_date_picker_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "종료일 선택",
                            tint = primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("종료일 (직접 선택)", color = Color(0xFF64748B), fontSize = 10.sp)
                            Text(
                                text = endDateText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Change Period CTA pill
                    Surface(
                        onClick = {
                            activeDateTab = 0
                            showDateRangeDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = primaryAccent.copy(alpha = 0.18f),
                        modifier = Modifier.testTag("open_date_dialog_button")
                    ) {
                        Text(
                            text = "기간 변경",
                            color = primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4. Inspected Point Tooltip Banner & Interactive Line Chart
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = primaryAccent,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "과거 1년치 환율 데이터를 집계하고 있습니다...",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "선택한 기간에 표시할 환율 데이터가 없습니다.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            } else {
                val safeIndex = selectedPointIndex.coerceIn(0, points.lastIndex)
                val activePoint = points[safeIndex]
                val firstPoint = points.first()
                val startVal = firstPoint.valueFor(chartPairMode)
                val activeVal = activePoint.valueFor(chartPairMode)
                val diffVal = activeVal - startVal
                val diffPct = if (startVal > 0.0) (diffVal / startVal) * 100.0 else 0.0
                val isPositive = diffVal >= 0.0
                val diffColor = if (isPositive) Color(0xFF34D399) else Color(0xFFF43F5E)
                val diffSign = if (isPositive) "+" else ""

                // Active Point Detail Readout Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activePoint.detailPeriodLabel,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "그래프를 터치/드래그하여 상세 조회",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (chartPairMode == ChartPairMode.TJS_TO_KRW) {
                                        "1 TJS = ${String.format(Locale.US, "%,.2f", activeVal)} 원"
                                    } else {
                                        "1,000 KRW = ${String.format(Locale.US, "%,.3f", activeVal)} TJS"
                                    },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = diffColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "시작일 대비 $diffSign${String.format(Locale.US, "%.2f", diffVal)} ($diffSign${String.format(Locale.US, "%.2f", diffPct)}%)",
                                    color = diffColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Sub-row: High/Low within bucket (for weekly/monthly) + USD cross rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (aggregationUnit != AggregationUnit.DAILY) {
                                val hi = activePoint.highFor(chartPairMode)
                                val lo = activePoint.lowFor(chartPairMode)
                                Text(
                                    text = "구간 최고 ${formatRateValue(hi, chartPairMode)} • 최저 ${formatRateValue(lo, chartPairMode)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            } else {
                                val inverseText = if (chartPairMode == ChartPairMode.TJS_TO_KRW) {
                                    "1,000 KRW = ${String.format(Locale.US, "%.3f", activePoint.tjsPer1000Krw)} TJS"
                                } else {
                                    "1 TJS = ${String.format(Locale.US, "%,.2f", activePoint.krwPerTjs)} 원"
                                }
                                Text(
                                    text = "역환율: $inverseText",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = "USD/KRW ${String.format(Locale.US, "%,.1f", activePoint.krwToUsd)} • USD/TJS ${String.format(Locale.US, "%.2f", activePoint.tjsToUsd)}",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Interactive Canvas Line Chart
                InteractiveRateLineChart(
                    points = points,
                    chartPairMode = chartPairMode,
                    aggregationUnit = aggregationUnit,
                    selectedIndex = safeIndex,
                    primaryColor = primaryAccent,
                    onSelectIndex = { selectedPointIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .testTag("interactive_line_chart_canvas")
                )

                // 5. Period Summary Stats (평균 / 최고 / 최저 / 변동률)
                val allValues = points.map { it.valueFor(chartPairMode) }
                val minPoint = points.minByOrNull { it.valueFor(chartPairMode) }
                val maxPoint = points.maxByOrNull { it.valueFor(chartPairMode) }
                val avgVal = allValues.average()
                val periodStartVal = points.first().valueFor(chartPairMode)
                val periodEndVal = points.last().valueFor(chartPairMode)
                val periodChangePct = if (periodStartVal > 0) {
                    ((periodEndVal - periodStartVal) / periodStartVal) * 100.0
                } else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniCard(
                        title = "기간 평균",
                        value = formatRateValue(avgVal, chartPairMode),
                        subText = "${points.size}개 ${aggregationUnit.labelKo} 평균",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "기간 최고",
                        value = formatRateValue(maxPoint?.valueFor(chartPairMode) ?: avgVal, chartPairMode),
                        subText = maxPoint?.xLabel ?: "-",
                        valueColor = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "기간 최저",
                        value = formatRateValue(minPoint?.valueFor(chartPairMode) ?: avgVal, chartPairMode),
                        subText = minPoint?.xLabel ?: "-",
                        valueColor = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f)
                    )
                    val isPeriodUp = periodChangePct >= 0.0
                    StatMiniCard(
                        title = "기간 변동",
                        value = "${if (isPeriodUp) "+" else ""}${String.format(Locale.US, "%.2f", periodChangePct)}%",
                        subText = "${points.first().xLabel} → ${points.last().xLabel}",
                        valueColor = if (isPeriodUp) Color(0xFF34D399) else Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. Collapsible Data Table for Daily / Weekly / Monthly Records
                Surface(
                    onClick = { showDataTable = !showDataTable },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_chart_table_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${aggregationUnit.labelKo} 환율 데이터 표 ${if (showDataTable) "접기" else "펼쳐보기"} (${points.size}건)",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (showDataTable) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "데이터 표 토글",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showDataTable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("일자 / 기간", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("1 TJS (원화)", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("1,000 KRW (소모니)", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                        // Show up to 15 most recent points in reverse chronological order
                        points.asReversed().take(15).forEach { pt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pt.detailPeriodLabel.substringBefore(" ("),
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", pt.krwPerTjs)} 원",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.3f", pt.tjsPer1000Krw)} TJS",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (points.size > 15) {
                            Text(
                                text = "* 최근 15개 ${aggregationUnit.labelKo} 구간이 표시됩니다. 전체 구간은 상단 라인 그래프를 드래그하여 확인하세요.",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateRangeDialog) {
        CustomDateRangePickerDialog(
            initialStartMillis = startDateMillis,
            initialEndMillis = endDateMillis,
            minAllowedMillis = minAllowedDateMillis,
            maxAllowedMillis = maxAllowedDateMillis,
            initialTab = activeDateTab,
            primaryAccent = primaryAccent,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { newStart, newEnd ->
                showDateRangeDialog = false
                onCustomDateRangeSelected(newStart, newEnd)
            }
        )
    }
}

@Composable
private fun InteractiveRateLineChart(
    points: List<HistoricalChartPoint>,
    chartPairMode: ChartPairMode,
    aggregationUnit: AggregationUnit,
    selectedIndex: Int,
    primaryColor: Color,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val values = remember(points, chartPairMode) {
        points.map { it.valueFor(chartPairMode) }
    }
    val highs = remember(points, chartPairMode) {
        points.map { it.highFor(chartPairMode) }
    }
    val lows = remember(points, chartPairMode) {
        points.map { it.lowFor(chartPairMode) }
    }

    val rawMin = remember(lows) { lows.minOrNull() ?: 0.0 }
    val rawMax = remember(highs) { highs.maxOrNull() ?: 1.0 }
    val span = (rawMax - rawMin).let { if (it <= 0.0001) 1.0 else it }
    val yMin = rawMin - span * 0.12
    val yMax = rawMax + span * 0.12
    val yRange = (yMax - yMin).coerceAtLeast(0.0001)

    val minIdx = remember(values) {
        values.indices.minByOrNull { values[it] } ?: 0
    }
    val maxIdx = remember(values) {
        values.indices.maxByOrNull { values[it] } ?: 0
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF162032))
            .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        val leftPad = 48.dp.toPx()
                        val rightPad = 12.dp.toPx()
                        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        if (points.size <= 1) {
                            onSelectIndex(0)
                        } else {
                            val fraction = ((offset.x - leftPad) / chartWidth).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val leftPad = 48.dp.toPx()
                        val rightPad = 12.dp.toPx()
                        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        if (points.size <= 1) {
                            onSelectIndex(0)
                        } else {
                            val fraction = ((change.position.x - leftPad) / chartWidth).coerceIn(0f, 1f)
                            val idx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
        ) {
            val leftPad = 48.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 16.dp.toPx()
            val bottomPad = 26.dp.toPx()

            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            fun xForIndex(index: Int): Float {
                if (points.size <= 1) return leftPad + chartWidth / 2f
                return leftPad + (index.toFloat() / (points.size - 1).toFloat()) * chartWidth
            }

            fun yForValue(v: Double): Float {
                val normalized = ((v - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                return topPad + chartHeight * (1f - normalized)
            }

            // 1. Draw Horizontal Grid Lines & Y-Axis Labels
            val gridSteps = 4
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val labelStyle = TextStyle(
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )

            for (i in 0..gridSteps) {
                val frac = i.toFloat() / gridSteps.toFloat()
                val y = topPad + chartHeight * (1f - frac)
                val gridVal = yMin + yRange * frac

                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.6f),
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                val labelText = if (chartPairMode == ChartPairMode.TJS_TO_KRW) {
                    String.format(Locale.US, "%.1f", gridVal)
                } else {
                    String.format(Locale.US, "%.2f", gridVal)
                }
                val measured = textMeasurer.measure(labelText, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                        y = y - measured.size.height / 2f
                    )
                )
            }

            if (points.isEmpty()) return@Canvas

            // 2. If Weekly or Monthly, draw High-Low Volatility Band
            if (aggregationUnit != AggregationUnit.DAILY && points.size > 1) {
                val bandPath = Path()
                points.indices.forEach { idx ->
                    val x = xForIndex(idx)
                    val yHi = yForValue(highs[idx])
                    if (idx == 0) bandPath.moveTo(x, yHi) else bandPath.lineTo(x, yHi)
                }
                for (idx in points.lastIndex downTo 0) {
                    val x = xForIndex(idx)
                    val yLo = yForValue(lows[idx])
                    bandPath.lineTo(x, yLo)
                }
                bandPath.close()
                drawPath(
                    path = bandPath,
                    color = primaryColor.copy(alpha = 0.12f)
                )
            }

            // 3. Build Main Line Path & Fill Area Path
            val linePath = Path()
            val fillPath = Path()

            points.indices.forEach { idx ->
                val x = xForIndex(idx)
                val y = yForValue(values[idx])
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, topPad + chartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(xForIndex(points.lastIndex), topPad + chartHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.32f),
                        primaryColor.copy(alpha = 0.02f)
                    ),
                    startY = topPad,
                    endY = topPad + chartHeight
                )
            )

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 4. Draw Data Point Dots when count <= 45
            if (points.size <= 45) {
                points.indices.forEach { idx ->
                    val cx = xForIndex(idx)
                    val cy = yForValue(values[idx])
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 3.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            // 5. Highlight Min & Max Points with Badges
            val maxPointOffset = Offset(xForIndex(maxIdx), yForValue(values[maxIdx]))
            val minPointOffset = Offset(xForIndex(minIdx), yForValue(values[minIdx]))

            drawCircle(
                color = Color(0xFF34D399),
                radius = 4.5.dp.toPx(),
                center = maxPointOffset
            )
            drawCircle(
                color = Color(0xFFF43F5E),
                radius = 4.5.dp.toPx(),
                center = minPointOffset
            )

            // 6. Draw X-Axis Date Labels (up to 5 ticks)
            val xTickCount = min(5, points.size)
            if (xTickCount == 1) {
                val measured = textMeasurer.measure(points[0].xLabel, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = xForIndex(0) - measured.size.width / 2f,
                        y = topPad + chartHeight + 6.dp.toPx()
                    )
                )
            } else if (xTickCount > 1) {
                for (t in 0 until xTickCount) {
                    val idx = ((t.toFloat() / (xTickCount - 1).toFloat()) * (points.size - 1)).roundToInt()
                        .coerceIn(0, points.lastIndex)
                    val cx = xForIndex(idx)
                    val measured = textMeasurer.measure(points[idx].xLabel, style = labelStyle)
                    val clampedX = (cx - measured.size.width / 2f)
                        .coerceIn(leftPad, size.width - rightPad - measured.size.width)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = clampedX,
                            y = topPad + chartHeight + 6.dp.toPx()
                        )
                    )
                }
            }

            // 7. Draw Selected Point Crosshair & Indicator
            val safeSel = selectedIndex.coerceIn(0, points.lastIndex)
            val selX = xForIndex(safeSel)
            val selY = yForValue(values[safeSel])

            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(selX, topPad),
                end = Offset(selX, topPad + chartHeight),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 9.dp.toPx(),
                center = Offset(selX, selY)
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(selX, selY)
            )
            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = Offset(selX, selY)
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    subText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = subText,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialStartMillis: Long,
    initialEndMillis: Long,
    minAllowedMillis: Long,
    maxAllowedMillis: Long,
    initialTab: Int,
    primaryAccent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var tempStartMillis by remember { mutableStateOf(initialStartMillis) }
    var tempEndMillis by remember { mutableStateOf(initialEndMillis) }

    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }

    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = tempStartMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in (minAllowedMillis - ExchangeRepository.DAY_MILLIS)..(maxAllowedMillis + ExchangeRepository.DAY_MILLIS)
            }
        }
    )

    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = tempEndMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in (minAllowedMillis - ExchangeRepository.DAY_MILLIS)..(maxAllowedMillis + ExchangeRepository.DAY_MILLIS)
            }
        }
    )

    LaunchedEffect(startPickerState.selectedDateMillis) {
        startPickerState.selectedDateMillis?.let { picked ->
            tempStartMillis = picked.coerceIn(minAllowedMillis, maxAllowedMillis)
            if (tempStartMillis > tempEndMillis) {
                tempEndMillis = tempStartMillis
            }
        }
    }

    LaunchedEffect(endPickerState.selectedDateMillis) {
        endPickerState.selectedDateMillis?.let { picked ->
            tempEndMillis = picked.coerceIn(minAllowedMillis, maxAllowedMillis)
            if (tempEndMillis < tempStartMillis) {
                tempStartMillis = tempEndMillis
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "조회 기간 직접 선택 (과거 1년 내)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Start & End Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 0) primaryAccent else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "시작일",
                                color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = dateFormatter.format(Date(tempStartMillis)),
                                color = if (selectedTab == 0) Color(0xFF0F172A) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 1) primaryAccent else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "종료일",
                                color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = dateFormatter.format(Date(tempEndMillis)),
                                color = if (selectedTab == 1) Color(0xFF0F172A) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Material 3 DatePicker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    if (selectedTab == 0) {
                        DatePicker(
                            state = startPickerState,
                            title = null,
                            headline = null,
                            showModeToggle = false
                        )
                    } else {
                        DatePicker(
                            state = endPickerState,
                            title = null,
                            headline = null,
                            showModeToggle = false
                        )
                    }
                }

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalStart = min(tempStartMillis, tempEndMillis)
                            val finalEnd = max(tempStartMillis, tempEndMillis)
                            onConfirm(finalStart, finalEnd)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_custom_date_button")
                    ) {
                        Text(
                            text = "선택 기간 적용",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatRateValue(value: Double, mode: ChartPairMode): String {
    return when (mode) {
        ChartPairMode.TJS_TO_KRW -> "${String.format(Locale.US, "%,.2f", value)} 원"
        ChartPairMode.KRW_TO_TJS -> "${String.format(Locale.US, "%.3f", value)} TJS"
    }
}
