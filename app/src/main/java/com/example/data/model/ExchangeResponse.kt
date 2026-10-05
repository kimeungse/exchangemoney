package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeResponse(
    @Json(name = "result") val result: String,
    @Json(name = "base_code") val baseCode: String,
    @Json(name = "rates") val rates: Map<String, Double>
)

@JsonClass(generateAdapter = true)
data class HistoricalUsdResponse(
    @Json(name = "date") val date: String?,
    @Json(name = "usd") val usd: Map<String, Double>?
)

enum class AggregationUnit(val labelKo: String, val descriptionKo: String) {
    DAILY("일별", "일일 종가 기준"),
    WEEKLY("주별", "주간 평균 기준"),
    MONTHLY("월별", "월간 평균 기준")
}

enum class DateRangePreset(val labelKo: String, val days: Int?) {
    ONE_WEEK("1주", 7),
    ONE_MONTH("1개월", 30),
    THREE_MONTHS("3개월", 90),
    SIX_MONTHS("6개월", 180),
    ONE_YEAR("1년", 365),
    CUSTOM("직접 선택", null)
}

enum class ChartPairMode(val titleKo: String, val shortLabel: String, val unitSuffix: String) {
    TJS_TO_KRW("1 소모니(TJS) 당 원화(KRW)", "1 TJS → KRW", "원"),
    KRW_TO_TJS("1,000 원(KRW) 당 소모니(TJS)", "1,000 KRW → TJS", "TJS")
}

data class HistoricalChartPoint(
    val xLabel: String,
    val detailPeriodLabel: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val krwPerTjs: Double,
    val highKrwPerTjs: Double,
    val lowKrwPerTjs: Double,
    val tjsPer1000Krw: Double,
    val highTjsPer1000Krw: Double,
    val lowTjsPer1000Krw: Double,
    val krwToUsd: Double,
    val tjsToUsd: Double,
    val sampleCount: Int
) {
    fun valueFor(mode: ChartPairMode): Double = when (mode) {
        ChartPairMode.TJS_TO_KRW -> krwPerTjs
        ChartPairMode.KRW_TO_TJS -> tjsPer1000Krw
    }

    fun highFor(mode: ChartPairMode): Double = when (mode) {
        ChartPairMode.TJS_TO_KRW -> highKrwPerTjs
        ChartPairMode.KRW_TO_TJS -> highTjsPer1000Krw
    }

    fun lowFor(mode: ChartPairMode): Double = when (mode) {
        ChartPairMode.TJS_TO_KRW -> lowKrwPerTjs
        ChartPairMode.KRW_TO_TJS -> lowTjsPer1000Krw
    }
}

