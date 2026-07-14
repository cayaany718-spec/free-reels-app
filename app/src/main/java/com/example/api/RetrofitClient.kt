package com.example.api

import com.example.shortdrama.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SUPABASE_URL + "/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val supabaseApi: SupabaseApi = retrofit.create(SupabaseApi::class.java)
}
