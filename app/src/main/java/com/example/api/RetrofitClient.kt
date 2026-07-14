package com.example.api

import com.example.shortdrama.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SUPABASE_URL + "/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val supabaseApi: SupabaseApi = retrofit.create(SupabaseApi::class.java)
}
