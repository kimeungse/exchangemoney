package com.example.data.remote

import com.example.data.model.ExchangeResponse
import com.example.data.model.HistoricalUsdResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface RateApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeResponse

    @GET
    suspend fun getHistoricalUsdRates(@Url fullUrl: String): HistoricalUsdResponse
}

