package com.example.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import com.squareup.moshi.JsonClass

interface SupabaseApi {
    @GET("rest/v1/app_config")
    suspend fun getAppConfig(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "key,value"
    ): List<AppConfigRow>
}

@JsonClass(generateAdapter = true)
data class AppConfigRow(
    val key: String,
    val value: String
)
